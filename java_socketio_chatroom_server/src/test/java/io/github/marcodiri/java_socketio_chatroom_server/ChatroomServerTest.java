package io.github.marcodiri.java_socketio_chatroom_server;

import io.github.marcodiri.java_socketio_chatroom_core.model.Message;
import io.github.marcodiri.java_socketio_chatroom_server.model.ServerMessage;
import io.github.marcodiri.java_socketio_chatroom_server.repository.ServerRepository;
import io.socket.client.IO;
import io.socket.client.Socket;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.net.URI;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.*;

public class ChatroomServerTest {

	private Socket clientSocket;

	@Mock
	private ServerRepository serverRepository;

	@InjectMocks
	private ChatroomServer chatroomServer;

	private AutoCloseable closeable;

	@Before
	public void setup() {
		closeable = MockitoAnnotations.openMocks(this);
		try {
			chatroomServer.start();
		} catch (Exception ignored) {
			fail("ServerWrapper startup failed");
		}

		clientSocket = IO.socket(URI.create("http://localhost:3000"), IO.Options.builder().build());
	}

	@After
	public void closeConnectionsAndReleaseMocks() throws Exception {
		clientSocket.disconnect();
		chatroomServer.stop();
		closeable.close();
	}

	@Test
	public void testStartStartsTheServer() {
		try {
			await().atMost(2, SECONDS).until(() -> chatroomServer.isStarted());
		} catch (org.awaitility.core.ConditionTimeoutException ignored) {
			fail("Cannot start the server");
		}
	}

	@Test
	public void testStartAttachesConnectionListenerToNamespace() {
		assertThat(chatroomServer.getNamespace().hasListeners("connection")).isTrue();
	}

	@Test
	public void testStartSendsConnectedEventToConnectedClients() {
		AtomicBoolean connectedReceived = new AtomicBoolean(false);

		clientSocket.on("connected", args -> connectedReceived.set(true));
		clientSocket.connect();

		try {
			await().atMost(5, SECONDS).untilTrue(connectedReceived);
		} catch (org.awaitility.core.ConditionTimeoutException ignored) {
			fail("Client did not receive \"connected\" event");
		}
	}

	@Test
	public void testStopStopsTheServer() throws Exception {
		chatroomServer.stop();
		try {
			await().atMost(2, SECONDS).until(() -> !chatroomServer.isStarted());
		} catch (org.awaitility.core.ConditionTimeoutException ignored) {
			fail("Cannot stop the server");
		}
	}

	@Test
	public void testStopStopsClearsTheUsernameList() throws Exception {
		chatroomServer.getUsernameList().put("Id", "user1");
		chatroomServer.stop();
		assertThat(chatroomServer.getUsernameList()).isEmpty();
	}

	@Test
	public void testServerRetrievesMessagesFromDbWhenClientJoinsWithFreeUsername() {
		List<Message> history = new ArrayList<>();
		ServerMessage msg1 = new ServerMessage(new Timestamp(0), "user1", "message1");
		ServerMessage msg2 = new ServerMessage(new Timestamp(1), "user2", "message2");
		history.add(msg1);
		history.add(msg2);
		when(serverRepository.findAll()).thenReturn(history);

		List<ServerMessage> retrievedMessages = new ArrayList<>();
		clientSocket.on("msg", args -> {
			JSONObject jsonMsg = (JSONObject) args[0];
			ServerMessage incomingMessage = new ServerMessage(new Timestamp(jsonMsg.getLong("timestamp")), jsonMsg.getString("user"), jsonMsg.getString("message"));
			retrievedMessages.add(incomingMessage);
		});
		clientSocket.on("connected", args -> clientSocket.emit("join", "user"));
		clientSocket.connect();

		try {
			await().atMost(2, SECONDS).until(() -> retrievedMessages.containsAll(asList(msg1, msg2)));
		} catch (org.awaitility.core.ConditionTimeoutException ignored) {
			fail("Expected " + asList(msg1, msg2) + " but got " + retrievedMessages);
		}
		verify(serverRepository, times(1)).findAll();
	}

	@Test
	public void testServerAddsUsernameToListWhenClientJoinsWithFreeUsername() {
		clientSocket.on("connected", args -> clientSocket.emit("join", "user"));
		clientSocket.connect();

		AtomicReference<Map<String, String>> usernameList = new AtomicReference<>();
		try {
			await().atMost(2, SECONDS).untilAsserted(() -> {
				usernameList.set(chatroomServer.getUsernameList());
				assertThat(usernameList.get()).containsExactly(entry(clientSocket.id(), "user"));
			});
		} catch (org.awaitility.core.ConditionTimeoutException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testUsernameIsRemovedFromListWhenClientDisconnects() {
		clientSocket.connect();
		try {
			await().atMost(2, SECONDS).until(() -> clientSocket.connected());
		} catch (org.awaitility.core.ConditionTimeoutException ignored) {
			fail("Client cannot connect to server");
		}

		chatroomServer.getUsernameList().put(clientSocket.id(), "user1");
		chatroomServer.getUsernameList().put("123", "user2");

		clientSocket.disconnect();
		try {
			await().atMost(2, SECONDS).untilAsserted(() ->
					assertThat(chatroomServer.getUsernameList().keySet()).doesNotContain(clientSocket.id()));
		} catch (org.awaitility.core.ConditionTimeoutException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testClientJoinWithOccupiedUsernameSendsErrorEvent() {
		chatroomServer.getUsernameList().put("Id", "user1");

		AtomicReference<String> errorMessage = new AtomicReference<>();

		clientSocket.on("error", args -> {
			JSONObject jsonMsg = (JSONObject) args[0];
			errorMessage.set(jsonMsg.getString("message"));
		});
		clientSocket.on("connected", args -> clientSocket.emit("join", "user1"));
		clientSocket.connect();

		try {
			await().atMost(2, SECONDS).untilAsserted(() -> assertThat(errorMessage.get()).isEqualTo("Username is already taken"));
		} catch (org.awaitility.core.ConditionTimeoutException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testHandleClientJoinSendsJoinedEvent() {
		AtomicBoolean joinedReceived = new AtomicBoolean(false);
		AtomicReference<String> chatRoomNameReceived = new AtomicReference<>();

		clientSocket.on("joined", args -> {
			chatRoomNameReceived.set(((JSONObject) args[0]).getString("roomName"));
			joinedReceived.set(true);
		});
		clientSocket.on("connected", args -> clientSocket.emit("join", "user"));
		clientSocket.connect();

		try {
			await().atMost(2, SECONDS).untilTrue(joinedReceived);
		} catch (org.awaitility.core.ConditionTimeoutException e) {
			fail(e.getMessage());
		}

		assertThat(chatRoomNameReceived.get()).isEqualTo("Chatroom");
	}

	@Test
	public void testClientReceivesItsMessages() {
		ServerMessage originalMessage1 = new ServerMessage(new Timestamp(0), "user1", "message1");
		ServerMessage originalMessage2 = new ServerMessage(new Timestamp(1), "user2", "message2");

		when(serverRepository.findAll()).thenReturn(new ArrayList<>());

		List<ServerMessage> retrievedMessages = new ArrayList<>();
		clientSocket.on("msg", args -> {
			JSONObject jsonMsg = (JSONObject) args[0];
			ServerMessage incomingMessage = new ServerMessage(new Timestamp(jsonMsg.getLong("timestamp")), jsonMsg.getString("user"), jsonMsg.getString("message"));
			retrievedMessages.add(incomingMessage);
		});
		clientSocket.on("connected", args -> {
			clientSocket.emit("join", "user");
			clientSocket.emit("msg", originalMessage1.toJSON());
			clientSocket.emit("msg", originalMessage2.toJSON());
		});
		clientSocket.connect();

		try {
			await().atMost(2, SECONDS).until(() -> retrievedMessages.containsAll(asList(originalMessage1, originalMessage2)));
		} catch (org.awaitility.core.ConditionTimeoutException ignored) {
			fail("Expected " + asList(originalMessage1, originalMessage2) + " but got " + retrievedMessages);
		}
	}

	@Test
	public void testMessagesAreSavedInDb() {
		ServerMessage originalMessage1 = new ServerMessage(new Timestamp(0), "user1", "message1");
		ServerMessage originalMessage2 = new ServerMessage(new Timestamp(1), "user2", "message2");

		when(serverRepository.findAll()).thenReturn(new ArrayList<>());

		List<ServerMessage> retrievedMessages = new ArrayList<>();
		clientSocket.on("msg", args -> {
			JSONObject jsonMsg = (JSONObject) args[0];
			ServerMessage incomingMessage = new ServerMessage(new Timestamp(jsonMsg.getLong("timestamp")), jsonMsg.getString("user"), jsonMsg.getString("message"));
			retrievedMessages.add(incomingMessage);
		});
		clientSocket.on("connected", args -> {
			clientSocket.emit("join", "user");
			clientSocket.emit("msg", originalMessage1.toJSON());
			clientSocket.emit("msg", originalMessage2.toJSON());
		});
		clientSocket.connect();

		ArgumentCaptor<ServerMessage> argumentCaptor = ArgumentCaptor.forClass(ServerMessage.class);
		try {
			await().atMost(2, SECONDS).until(() -> !retrievedMessages.isEmpty());
		} catch (org.awaitility.core.ConditionTimeoutException ignored) {
			fail("Expected " + asList(originalMessage1, originalMessage2) + " but got " + retrievedMessages);
		}
		verify(serverRepository, times(2)).save(argumentCaptor.capture());
		List<ServerMessage> capturedArgument = argumentCaptor.getAllValues();

		assertThat(capturedArgument).contains(originalMessage1, originalMessage2);
	}

	@Test
	public void testRoomSizeWhenClientJoinsAndWhenDisconnects() {
		clientSocket.on("connected", args -> clientSocket.emit("join", "user"));
		clientSocket.connect();

		try {
			await().atMost(2, SECONDS).until(() -> chatroomServer.getNamespace().getAdapter().listClients("Chatroom").length == 1);
		} catch (org.awaitility.core.ConditionTimeoutException ignored) {
			fail("Expected 1 but got " + chatroomServer.getNamespace().getAdapter().listClients("Chatroom").length);
		}
		clientSocket.disconnect();
		try {
			await().atMost(2, SECONDS).until(() -> chatroomServer.getNamespace().getAdapter().listClients("Chatroom").length == 0);
		} catch (org.awaitility.core.ConditionTimeoutException ignored) {
			fail("Expected 0 but got " + chatroomServer.getNamespace().getAdapter().listClients("Chatroom").length);
		}
	}

	@Test
	public void testServerIgnoresJoinFromAnInRoomClient() {
		when(serverRepository.findAll()).thenReturn(new ArrayList<>());

		clientSocket.on("connected", args -> {
			clientSocket.emit("join", "user1");

			clientSocket.emit("join", "user2");
		});
		clientSocket.connect();

		AtomicInteger roomSize = new AtomicInteger();
		try {
			await().during(2, SECONDS).atMost(5, SECONDS).until(() -> {
				roomSize.set(chatroomServer.getNamespace().getAdapter().listClients("Chatroom").length);
				return roomSize.get() == 1;
			});
		} catch (org.awaitility.core.ConditionTimeoutException ignored) {
			fail("Expected 1 but got " + roomSize.get());
		}

		verify(serverRepository, times(1)).findAll();
	}

	@Test
	public void testServerDoesNotBroadcastMessagesFromANotInRoomClient() {
		AtomicBoolean msgReceived = new AtomicBoolean(false);

		Socket clientSocket2 = IO.socket(URI.create("http://localhost:3000"), IO.Options.builder().build());
		clientSocket2.on("connected", args -> clientSocket2.emit("join", "user1"));
		clientSocket2.on("msg", args -> msgReceived.set(true));
		clientSocket2.connect();

		try {
			await().atMost(2, SECONDS).until(clientSocket2::connected);
		} catch (org.awaitility.core.ConditionTimeoutException e) {
			fail("Cannot connect to server");
		}

		clientSocket.on("connected", args -> {
			Message msg = new ServerMessage(new Timestamp(0), "user", "message");
			clientSocket.emit("msg", msg.toJSON());
		});
		clientSocket.connect();

		try {
			await().during(2, SECONDS).atMost(3, SECONDS).untilFalse(msgReceived);
		} catch (org.awaitility.core.ConditionTimeoutException ignored) {
			fail("An in room Client received a message from an out of room Client");
		}
	}

	@Test
	public void testServerDoesNotSaveMessagesFromANotInRoomClient() {
		clientSocket.on("connected", args -> {
			Message msg = new ServerMessage(new Timestamp(0), "user", "message");
			clientSocket.emit("msg", msg.toJSON());
		});
		clientSocket.connect();

		try {
			await().during(2, SECONDS).atMost(3, SECONDS).untilAsserted(() ->
					verifyNoInteractions(serverRepository));
		} catch (org.awaitility.core.ConditionTimeoutException e) {
			fail(e.getMessage());
		}
	}

}