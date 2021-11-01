package io.github.marcodiri.java_socketio_chatroom_client;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.*;

import java.net.SocketException;
import java.net.URI;
import java.sql.Timestamp;
import java.util.concurrent.atomic.AtomicReference;

import io.socket.client.Socket;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import io.github.marcodiri.java_socketio_chatroom_client.model.ClientMessage;
import io.github.marcodiri.java_socketio_chatroom_client.view.ClientView;
import io.github.marcodiri.java_socketio_chatroom_server_mock.ChatroomServerMock;
import io.socket.client.IO;

public class ChatroomClientTest {

	private ChatroomClient.ChatroomClientHandlers handlers;

	private ChatroomClient client;

	private ChatroomServerMock serverMock;
	
	private final String USERNAME = "user";

	@Before
	public void setup() {
		ClientView view = mock(ClientView.class);
		handlers = mock(ChatroomClient.ChatroomClientHandlers.class);
		client = new ChatroomClient(URI.create("http://localhost:3000"), IO.Options.builder().build(), handlers, view);

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
	public void testInitAttachesListenersToSocket() {
		assertThat(client.getSocket().hasListeners("connected")).isTrue();
		assertThat(client.getSocket().hasListeners(Socket.EVENT_DISCONNECT)).isTrue();
	}

	@Test
	public void testInitListenerCallsConnectedHandlerOnConnectedEvent() {
		serverMock.handleNamespaceEvent("connection", arg -> serverMock.getSocket().send("connected"));

		client.getSocket().connect();

		try {
			await().atMost(2, SECONDS).untilAsserted(() -> verify(handlers).connectedHandler());
		} catch (org.awaitility.core.ConditionTimeoutException ignored) {
			fail("connectedHandler not called");
		}
		
		verifyNoMoreInteractions(handlers);
	}

	@Test
	public void testInitListenerCallsDisconnectedHandlerOnDisconnectEvent() {
		client.getSocket().connect();
		assertClientConnected();

		client.getSocket().disconnect();

		try {
			await().atMost(2, SECONDS).untilAsserted(() -> verify(handlers).disconnectedHandler());
		} catch (org.awaitility.core.ConditionTimeoutException ignored) {
			fail("disconnectedHandler not called");
		}
		
		verifyNoMoreInteractions(handlers);
	}

	@Test
	public void testConnectConnectsTheClientToServer() {
		client.connect(USERNAME);
		assertClientConnected();
	}

	@Test
	public void testConnectSetsUsername() {
		client.connect(USERNAME);
		
		assertThat(client.username).isEqualTo(USERNAME);
	}

	@Test
	public void testDisconnectDisconnectsTheClientFromServer() {
		client.getSocket().connect();
		assertClientConnected();

		client.disconnect();
		try {
			await().atMost(2, SECONDS).until(() -> !client.getSocket().connected());
		} catch (org.awaitility.core.ConditionTimeoutException ignored) {
			fail("Cannot disconnect from server");
		}
	}

	@Test
	public void testSendMessageWhenClientNotConnected() {
		ClientMessage msg = new ClientMessage(new Timestamp(0), USERNAME, "message");

		assertThatThrownBy(() -> client.sendMessage(msg)).isInstanceOf(SocketException.class)
				.hasMessage("Unable to send message when not connected to server");
	}

	@Test
	public void testSendMessageWhenClientConnected() {
		AtomicReference<JSONObject> receivedMsg = new AtomicReference<>();

		serverMock.handleNamespaceEvent("connection", arg -> serverMock.getSocket().send("connected"));
		serverMock.handleEvent("msg", arg -> receivedMsg.set((JSONObject) arg[0]));

		client.getSocket().connect();
		assertClientConnected();
		client.connected.set(true);

		ClientMessage msg = new ClientMessage(new Timestamp(0), USERNAME, "message");
		try {
			client.sendMessage(msg);
		} catch (SocketException e) {
			fail(e.getMessage());
		}

		try {
			await().atMost(2, SECONDS).untilAsserted(() -> assertThat(receivedMsg.get()).isNotNull());
			await().atMost(2, SECONDS).until(() -> msg.equals(new ClientMessage(receivedMsg.get())));
		} catch (org.awaitility.core.ConditionTimeoutException e) {
			fail(e.getMessage());
		}
	}

	private void assertClientConnected() {
		try {
			await().atMost(2, SECONDS).until(() -> client.getSocket().connected());
		} catch (org.awaitility.core.ConditionTimeoutException ignored) {
			fail("Cannot connect to server");
		}
	}

}
