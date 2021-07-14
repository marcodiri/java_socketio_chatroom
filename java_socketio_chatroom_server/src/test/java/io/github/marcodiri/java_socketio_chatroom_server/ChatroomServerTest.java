package io.github.marcodiri.java_socketio_chatroom_server;

import io.github.marcodiri.java_socketio_chatroom_core.model.Message;
import io.github.marcodiri.java_socketio_chatroom_server.model.ServerMessage;
import io.github.marcodiri.java_socketio_chatroom_server.repository.ServerRepository;

import io.socket.client.IO;
import io.socket.client.Socket;
import org.json.JSONObject;
import org.junit.*;
import org.mockito.*;

import static java.util.Arrays.asList;
import static org.mockito.Mockito.*;

import java.net.URI;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.awaitility.Awaitility.await;

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
    public void testClientJoinRetrievesMessagesFromDb() {
        List<Message> history = new ArrayList<>();
        ServerMessage msg1 = new ServerMessage(new Timestamp(System.currentTimeMillis()), "user1", "message1");
        ServerMessage msg2 = new ServerMessage(new Timestamp(System.currentTimeMillis()), "user2", "message2");
        history.add(msg1);
        history.add(msg2);
        when(serverRepository.findAll()).thenReturn(history);

        List<ServerMessage> retrievedMessages = new ArrayList<>();
        clientSocket.on("msg", arg -> {
            JSONObject jsonMsg = (JSONObject) arg[0];
            ServerMessage incomingMessage = new ServerMessage(new Timestamp(jsonMsg.getLong("timestamp")), jsonMsg.getString("user"), jsonMsg.getString("message"));
            retrievedMessages.add(incomingMessage);
        });
        clientSocket.on(Socket.EVENT_CONNECT, objects -> clientSocket.emit("join"));
        clientSocket.connect();

        try {
            await().atMost(2, SECONDS).until(() -> retrievedMessages.containsAll(asList(msg1, msg2)));
        } catch (org.awaitility.core.ConditionTimeoutException ignored) {
            fail("Expected " + asList(msg1, msg2) + " but got " + retrievedMessages);
        }
        verify(serverRepository, times(1)).findAll();
    }

    @Test
    public void testClientReceivesItsMessages() {
        ServerMessage originalMessage1 = new ServerMessage(new Timestamp(System.currentTimeMillis()), "user1", "message1");
        ServerMessage originalMessage2 = new ServerMessage(new Timestamp(System.currentTimeMillis()), "user2", "message2");

        when(serverRepository.findAll()).thenReturn(new ArrayList<>());

        List<ServerMessage> retrievedMessages = new ArrayList<>();
        clientSocket.on("msg", arg -> {
            JSONObject jsonMsg = (JSONObject) arg[0];
            ServerMessage incomingMessage = new ServerMessage(new Timestamp(jsonMsg.getLong("timestamp")), jsonMsg.getString("user"), jsonMsg.getString("message"));
            retrievedMessages.add(incomingMessage);
        });
        clientSocket.on(Socket.EVENT_CONNECT, objects -> {
            clientSocket.emit("join");
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
        ServerMessage originalMessage1 = new ServerMessage(new Timestamp(System.currentTimeMillis()), "user1", "message1");
        ServerMessage originalMessage2 = new ServerMessage(new Timestamp(System.currentTimeMillis()), "user2", "message2");

        when(serverRepository.findAll()).thenReturn(new ArrayList<>());

        List<ServerMessage> retrievedMessages = new ArrayList<>();
        clientSocket.on("msg", arg -> {
            JSONObject jsonMsg = (JSONObject) arg[0];
            ServerMessage incomingMessage = new ServerMessage(new Timestamp(jsonMsg.getLong("timestamp")), jsonMsg.getString("user"), jsonMsg.getString("message"));
            retrievedMessages.add(incomingMessage);
        });
        clientSocket.on(Socket.EVENT_CONNECT, objects -> {
            clientSocket.emit("join");
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
    public void testRoomSizeWhenClientJoinsAndWhenLeaves() {
        clientSocket.on(Socket.EVENT_CONNECT, objects -> clientSocket.emit("join"));
        clientSocket.connect();

        try {
            await().atMost(2, SECONDS).until(() -> chatroomServer.getNamespace().getAdapter().listClients("Chatroom").length == 1);
        } catch (org.awaitility.core.ConditionTimeoutException ignored) {
            fail("Expected 1 but got " + chatroomServer.getNamespace().getAdapter().listClients("Chatroom").length);
        }
        clientSocket.emit("leave");
        try {
            await().atMost(2, SECONDS).until(() -> chatroomServer.getNamespace().getAdapter().listClients("Chatroom").length == 0);
        } catch (org.awaitility.core.ConditionTimeoutException ignored) {
            fail("Expected 0 but got " + chatroomServer.getNamespace().getAdapter().listClients("Chatroom").length);
        }

    }

    @Test
    public void testServerIgnoresJoinFromAnInRoomClient() {
        when(serverRepository.findAll()).thenReturn(new ArrayList<>());

        clientSocket.on(Socket.EVENT_CONNECT, objects -> {
            clientSocket.emit("join");

            clientSocket.emit("join");
        });
        clientSocket.connect();

        try {
            await().during(2, SECONDS).atMost(3, SECONDS).until(() -> chatroomServer.getNamespace().getAdapter().listClients("Chatroom").length == 1);
        } catch (org.awaitility.core.ConditionTimeoutException ignored) {
            fail("Expected 1 but got " + chatroomServer.getNamespace().getAdapter().listClients("Chatroom").length);
        }

        verify(serverRepository, times(1)).findAll();
    }

    @Test
    public void testServerIgnoresMessagesFromANonInRoomClient() {
        AtomicBoolean msgReceived = new AtomicBoolean(false);

        clientSocket.on(Socket.EVENT_CONNECT, objects -> {
            Message msg = new ServerMessage(new Timestamp(System.currentTimeMillis()), "user", "message");
            clientSocket.emit("msg", msg.toJSON());
        });
        clientSocket.on("msg", arg -> msgReceived.set(true));
        clientSocket.connect();

        try {
            await().during(2, SECONDS).atMost(3, SECONDS).untilFalse(msgReceived);
        } catch (org.awaitility.core.ConditionTimeoutException ignored) {
            fail("Expected no msg received but got instead");
        }
        verifyNoInteractions(serverRepository);
    }
}