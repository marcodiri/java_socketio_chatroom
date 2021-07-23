package io.github.marcodiri.java_socketio_chatroom_client;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.net.URI;
import java.sql.Timestamp;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import io.github.marcodiri.java_socketio_chatroom_client.model.ClientMessage;
import io.github.marcodiri.java_socketio_chatroom_client.view.ClientView;
import io.github.marcodiri.java_socketio_chatroom_server_mock.ChatroomServerMock;
import io.socket.client.IO;

public class ChatroomClientTest {

	private ClientView view;

	private ChatroomClient client;

	private ChatroomServerMock serverMock;

	@Before
	public void setup() {
		view = mock(ClientView.class);
		client = new ChatroomClient(URI.create("http://localhost:3000"), IO.Options.builder().build(), view);

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
	public void testConnect() {
		client.connect("user");
		try {
			await().atMost(2, SECONDS).untilTrue(serverMock.socketIsInRoom());
		} catch (org.awaitility.core.ConditionTimeoutException ignored) {
			fail("Socket could not join the room");
		}
		
		assertThat(client.getSocket().hasListeners("msg")).isTrue();
		assertThat(client.getSocket().hasListeners("joined")).isTrue();
	}

	@Test
	public void testSendMessageWhenClientNotConnected() {
		ClientMessage msg = new ClientMessage(new Timestamp(0), "user", "message");

		assertThatThrownBy(() -> client.sendMessage(msg)).isInstanceOf(RuntimeException.class)
				.hasMessage("Unable to send message when not connected to server");
	}

	@Test
	public void testSendMessageWhenClientConnected() {
		client.getSocket().connect();
		try {
			await().atMost(2, SECONDS).until(() -> client.isConnected());
		} catch (org.awaitility.core.ConditionTimeoutException ignored) {
			fail("Client could not connect to server");
		}

		try {
			serverMock.handleEvent("msg", arg -> serverMock.receivedMsg = (JSONObject) arg[0]);
		} catch (NullPointerException e) {
			fail("Socket is not connected to server");
		}

		ClientMessage msg = new ClientMessage(new Timestamp(0), "user", "message");
		try {
			client.sendMessage(msg);
		} catch (RuntimeException e) {
			fail(e.getMessage());
		}

		try {
			await().atMost(2, SECONDS).until(() -> msg.equals(new ClientMessage(serverMock.receivedMsg)));
		} catch (org.awaitility.core.ConditionTimeoutException ignored) {
			fail("Server did not receive the correct message");
		}
	}

	@Test
	public void testHandleMessage() {
		client.getSocket().connect();
		client.handleMessage();
		try {
			await().atMost(2, SECONDS).until(() -> client.isConnected());
		} catch (org.awaitility.core.ConditionTimeoutException ignored) {
			fail("Client could not connect to server");
		}

		ClientMessage msg = new ClientMessage(new Timestamp(0), "user", "message");
		try {
			serverMock.sendEvent("msg", msg.toJSON());
		} catch (NullPointerException e) {
			fail("Socket is null");
		}
		
		try {
			await().atMost(2, SECONDS).untilAsserted(() -> verify(view).addMessage(msg));
		} catch (org.awaitility.core.ConditionTimeoutException ignored) {
			fail("AddMessage on ClientView was not called");
		}
	}

	@Test
	public void testHandleJoin() {
		client.getSocket().connect();
		client.handleJoin();
		try {
			await().atMost(2, SECONDS).until(() -> client.isConnected());
		} catch (org.awaitility.core.ConditionTimeoutException ignored) {
			fail("Client could not connect to server");
		}

		serverMock.handleEvent("join", arg -> serverMock.sendEvent("joined", new JSONObject("{roomName: RoomName}")));
		client.getSocket().emit("join");

		try {
			await().atMost(2, SECONDS).untilAsserted(() -> verify(view).roomJoined("RoomName"));
		} catch (org.awaitility.core.ConditionTimeoutException ignored) {
			fail("RoomJoined on ClientView was not called");
		}

	}

}
