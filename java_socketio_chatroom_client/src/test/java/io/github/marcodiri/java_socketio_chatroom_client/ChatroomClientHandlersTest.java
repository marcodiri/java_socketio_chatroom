package io.github.marcodiri.java_socketio_chatroom_client;

import io.github.marcodiri.java_socketio_chatroom_client.model.ClientMessage;
import io.github.marcodiri.java_socketio_chatroom_client.view.ClientView;
import io.github.marcodiri.java_socketio_chatroom_server_mock.ChatroomServerMock;
import io.socket.client.IO;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.net.URI;
import java.sql.Timestamp;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class ChatroomClientHandlersTest {

	private ChatroomClient.ChatroomClientHandlers handlers;
	private ClientView view;

	private ChatroomClient client;

	private ChatroomServerMock serverMock;

	@Before
	public void setup() {
		view = mock(ClientView.class);
		client = new ChatroomClient(URI.create("http://localhost:3000"), IO.Options.builder().build(), view);
		handlers = client.getChatroomClientHandlers();

		serverMock = new ChatroomServerMock();
		try {
			serverMock.start();
		} catch (Exception ignored) {
			fail("Server startup failed");
		}
	}

	@After
	public void closeConnections() {
		client.getSocket().disconnect();
		try {
			serverMock.stop();
		} catch (Exception e) {
			fail("Failed to stop the server");
		}
	}

	@Test
	public void testConnectedHandlerCallsRoomJoinedOnJoinedEvent() {
		client.username = "user";
		connectClient();
		
		handlers.connectedHandler();

		String roomName = "RoomName";
		serverMock.sendEvent("joined", new JSONObject("{roomName: " + roomName + "}"));
		try {
			await().atMost(2, SECONDS).untilAsserted(() -> verify(view).roomJoined(roomName));
		} catch (org.awaitility.core.ConditionTimeoutException ignored) {
			fail("handleJoin not called");
		}
	}
	
	@Test
	public void testConnectedHandlerEmitsJoinEvent() {
		client.username = "user";
		AtomicBoolean joinReceived = new AtomicBoolean(false);
		AtomicReference<String> usernameReceived = new AtomicReference<>();

		serverMock.handleEvent("join", args -> {
			joinReceived.set(true);
			usernameReceived.set((String) args[0]);
		});
		
		connectClient();

		handlers.connectedHandler();
		
		try {
			await().atMost(2, SECONDS).untilTrue(joinReceived);
		} catch (org.awaitility.core.ConditionTimeoutException ignored) {
			fail("Client did not emit \"join\" event");
		}

		try {
			await().atMost(2, SECONDS).untilAsserted(() -> assertThat(usernameReceived.get()).isEqualTo(client.username));
		} catch (org.awaitility.core.ConditionTimeoutException ignored) {
			fail("Username is invalid");
		}
	}

	@Test
	public void testConnectedHandlerCallsAddMessageOnMsgEvent() {
		client.username = "user";
		connectClient();

		handlers.connectedHandler();
		
		ClientMessage msg = new ClientMessage(new Timestamp(0), "user", "message");
		try {
			serverMock.sendEvent("msg", msg.toJSON());
		} catch (NullPointerException e) {
			fail("Socket is null");
		}

		try {
			await().atMost(2, SECONDS).untilAsserted(() -> verify(view).addMessage(msg));
		} catch (org.awaitility.core.ConditionTimeoutException ignored) {
			fail("handleMessage not called");
		}
	}

	@Test
	public void testConnectedHandlerCallsShowErrorOnErrorEvent() {
		client.username = "user";
		connectClient();

		handlers.connectedHandler();
		
		String errorMessage = "Username is already taken";
		try {
			serverMock.sendEvent("error", new JSONObject("{message: " + errorMessage + "}"));
		} catch (NullPointerException e) {
			fail("Socket is null");
		}

		try {
			await().atMost(2, SECONDS).untilAsserted(() -> verify(view).showError(errorMessage));
		} catch (org.awaitility.core.ConditionTimeoutException ignored) {
			fail("showError on ClientView was not called or message is invalid");
		}
	}

	@Test
	public void testConnectedHandlerSetsConnectedToTrueAfterListenersAreAttached() {
		client.username = "user";
		connectClient();

		handlers.connectedHandler();

		assertThat(client.getSocket().hasListeners("msg")).isTrue();
		assertThat(client.getSocket().hasListeners("joined")).isTrue();
		assertThat(client.getSocket().hasListeners("error")).isTrue();
		
		try {
			await().atMost(2, SECONDS).until(() -> client.isConnected());
		} catch (org.awaitility.core.ConditionTimeoutException ignored) {
			fail("Handler does not set connected state");
		}
	}

	@Test
	public void testConnectedHandlerIfUsernameIsNull() {
		assertThat(client.username).isNull();

		assertThatThrownBy(() -> handlers.connectedHandler()).isInstanceOf(NullPointerException.class)
				.hasMessage("Username is null");
	}


	@Test
	public void testDisconnectedHandlerSetsConnectedToFalseAfterListenersAreDetached() {
		client.getSocket().on("event1", args -> {
		});
		client.getSocket().on("event2", args -> {
		});
		
		connectClient();
		client.connected.set(true);
		
		handlers.disconnectedHandler();
		
		assertThat(client.getSocket().hasListeners("event1")).isFalse();
		assertThat(client.getSocket().hasListeners("event2")).isFalse();
		
		assertThat(client.isConnected()).isFalse();
	}
	
	@Test
	public void testDisconnectedHandlerSetsUsernameToNull() {
		client.username = "user";

		connectClient();

		handlers.disconnectedHandler();

		assertThat(client.username).isNull();
	}

	private void connectClient() {
		client.getSocket().connect();
		try {
			await().atMost(2, SECONDS).until(() -> client.getSocket().connected());
		} catch (org.awaitility.core.ConditionTimeoutException ignored) {
			fail("Client cannot connect to server");
		}
	}
}
