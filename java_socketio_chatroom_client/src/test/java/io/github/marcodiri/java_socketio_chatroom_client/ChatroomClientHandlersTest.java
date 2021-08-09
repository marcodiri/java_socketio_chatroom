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
		client.disconnect();
		try {
			serverMock.stop();
		} catch (Exception e) {
			fail("Failed to stop the server");
		}
	}

	@Test
	public void testConnectedHandlerIfUsernameIsNotNull() {
		AtomicBoolean joinReceived = new AtomicBoolean(false);
		client.username = "user";
		AtomicReference<String> usernameReceived = new AtomicReference<>();

		serverMock.handleEvent("join", args -> {
			joinReceived.set(true);
			usernameReceived.set((String) args[0]);
		});

		client.getSocket().connect();
		try {
			await().atMost(2, SECONDS).until(() -> client.getSocket().connected());
		} catch (org.awaitility.core.ConditionTimeoutException ignored) {
			fail("Client cannot connect to server");
		}
		handlers.connectedHandler();

		// joined handler
		String roomName = "RoomName";
		serverMock.sendEvent("joined", new JSONObject("{roomName: " + roomName + "}"));
		try {
			await().atMost(2, SECONDS).untilAsserted(() -> verify(view).roomJoined(roomName));
		} catch (org.awaitility.core.ConditionTimeoutException ignored) {
			fail("handleJoin not called");
		}

		// join emit
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

		// msg handler
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

		// error handler
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

		// set connected
		try {
			await().atMost(2, SECONDS).until(() -> client.isConnected());
		} catch (org.awaitility.core.ConditionTimeoutException ignored) {
			fail("Handler does not set connected state");
		}

		assertThat(client.getSocket().hasListeners("msg")).isTrue();
		assertThat(client.getSocket().hasListeners("joined")).isTrue();
		assertThat(client.getSocket().hasListeners("error")).isTrue();
	}

	@Test
	public void testConnectedHandlerIfUsernameIsNull() {
		assertThat(client.username).isNull();

		assertThatThrownBy(() -> handlers.connectedHandler()).isInstanceOf(NullPointerException.class)
				.hasMessage("Username is null");
	}


	@Test
	public void testDisconnectHandler() {
		client.getSocket().on("event1", args -> {
		});
		client.getSocket().on("event2", args -> {
		});

		client.getSocket().connect();
		try {
			await().atMost(2, SECONDS).until(() -> client.getSocket().connected());
		} catch (org.awaitility.core.ConditionTimeoutException ignored) {
			fail("Client cannot connect to server");
		}

		client.connected.set(true);

		client.getSocket().disconnect();
		try {
			await().atMost(2, SECONDS).until(() -> !client.getSocket().connected());
		} catch (org.awaitility.core.ConditionTimeoutException ignored) {
			fail("Client cannot disconnect from server");
		}

		assertThat(client.getSocket().hasListeners("event1")).isFalse();
		assertThat(client.getSocket().hasListeners("event2")).isFalse();

		assertThat(client.isConnected()).isFalse();
	}
}
