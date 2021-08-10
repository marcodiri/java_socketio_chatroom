package io.github.marcodiri.java_socketio_chatroom_server_mock;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter.Listener;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.net.URI;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.await;

public class ChatroomServerMockTest {

	private ChatroomServerMock serverMock;

	private Socket clientSocket;

	@Before
	public void setup() {
		serverMock = new ChatroomServerMock();
		try {
			serverMock.start();
		} catch (Exception ignored) {
			fail("ServerWrapper startup failed");
		}

		clientSocket = IO.socket(URI.create("http://localhost:3000"), IO.Options.builder().build());
	}

	@After
	public void closeConnections() throws Exception {
		clientSocket.disconnect();
		serverMock.stop();
	}

	@Test
	public void testStartStartsTheServer() {
		try {
			await().atMost(2, SECONDS).until(() -> serverMock.isStarted());
		} catch (org.awaitility.core.ConditionTimeoutException ignored) {
			fail("Server cannot be started");
		}
	}

	@Test
	public void testStartAttachesConnectionListenerToNamespace() {
		assertThat(serverMock.getNamespace().hasListeners("connection")).isTrue();
	}

	@Test
	public void testStopStopsTheServer() throws Exception {
		serverMock.stop();
		try {
			await().atMost(2, SECONDS).until(() -> !serverMock.isStarted());
		} catch (org.awaitility.core.ConditionTimeoutException ignored) {
			fail("Server cannot be stopped");
		}
	}

	@Test
	public void testHandleConnectionsSavesTheConnectedClientSocket() {
		clientSocket.connect();
		waitClientConnected();

		assertThat(serverMock.getSocket().getId()).isEqualTo(clientSocket.id());
	}

	@Test
	public void testHandleConnectionsAttachesSavedListenersToClientOnConnectAndClearsTheList() {
		String event = "event";
		Listener listener1 = arg -> {
		};
		Listener listener2 = arg -> {
		};

		serverMock.getHandlersToAttach().put(event, new ArrayList<>());
		serverMock.getHandlersToAttach().get(event).add(listener1);
		serverMock.getHandlersToAttach().get(event).add(listener2);

		clientSocket.connect();
		waitClientConnected();

		assertThat(serverMock.getSocket().listeners(event)).containsExactly(listener1, listener2);
		assertThat(serverMock.getHandlersToAttach()).isEmpty();
	}

	@Test
	public void testHandleNamespaceEvent() {
		serverMock.handleNamespaceEvent("event", args -> {
		});

		assertThat(serverMock.getNamespace().hasListeners("event")).isTrue();
	}

	@Test
	public void testHandleEventWhenClientNotConnected() {
		assertThat(serverMock.getSocket()).isNull();

		String event = "event";
		Listener listener1 = arg -> {
		};
		Listener listener2 = arg -> {
		};

		serverMock.handleEvent(event, listener1);
		serverMock.handleEvent(event, listener2);

		assertThat(serverMock.getHandlersToAttach()).containsKey(event);
		assertThat(serverMock.getHandlersToAttach().get(event)).containsExactly(listener1, listener2);

	}

	@Test
	public void testHandleEventWhenClientConnected() {
		clientSocket.connect();
		waitClientConnected();

		try {
			serverMock.handleEvent("event", args -> {
			});
		} catch (NullPointerException e) {
			fail("Socket is null");
		}

		assertThat(serverMock.getSocket().hasListeners("event")).isTrue();
	}

	@Test
	public void testSendEventWhenClientNotConnected() {
		assertThat(serverMock.getSocket()).isNull();

		assertThatThrownBy(() -> serverMock.sendEvent("event", null)).isInstanceOf(NullPointerException.class)
				.hasMessage("socket is null");
	}

	@Test
	public void testSendEventWhenClientConnected() {
		AtomicBoolean eventReceived = new AtomicBoolean(false);

		clientSocket.on("event", args -> eventReceived.set(true));

		clientSocket.connect();
		waitClientConnected();

		try {
			serverMock.sendEvent("event", new JSONObject());
		} catch (NullPointerException e) {
			fail("Socket is null");
		}

		try {
			await().atMost(2, SECONDS).untilTrue(eventReceived);
		} catch (org.awaitility.core.ConditionTimeoutException ignored) {
			fail("Client did not receive the event");
		}
	}

	private void waitClientConnected() {
		try {
			await().atMost(2, SECONDS).until(() -> clientSocket.connected());
		} catch (org.awaitility.core.ConditionTimeoutException ignored) {
			fail("Client could not connect to server");
		}
	}

}
