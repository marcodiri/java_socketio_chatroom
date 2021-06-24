package io.github.marcodiri.java_socketio_chatroom_server;

import io.github.marcodiri.java_socketio_chatroom_server.model.Message;
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

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.awaitility.Awaitility.await;

public class ChatroomServerIT {

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
        Message msg1 = new Message(new Timestamp(System.currentTimeMillis()), "user1", "message1");
        Message msg2 = new Message(new Timestamp(System.currentTimeMillis()), "user2", "message2");
        history.add(msg1);
        history.add(msg2);
        when(serverRepository.findAll()).thenReturn(history);

        List<Message> retrievedMessages = new ArrayList<>();
        clientSocket.on("msg", arg -> {
            JSONObject jsonMsg = (JSONObject) arg[0];
            Message incomingMessage = new Message(new Timestamp(jsonMsg.getLong("timestamp")), jsonMsg.getString("user"), jsonMsg.getString("message"));
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
        Message originalMessage1 = new Message(new Timestamp(System.currentTimeMillis()), "user1", "message1");
        Message originalMessage2 = new Message(new Timestamp(System.currentTimeMillis()), "user2", "message2");

        when(serverRepository.findAll()).thenReturn(new ArrayList<>());

        List<Message> retrievedMessages = new ArrayList<>();
        clientSocket.on("msg", arg -> {
            JSONObject jsonMsg = (JSONObject) arg[0];
            Message incomingMessage = new Message(new Timestamp(jsonMsg.getLong("timestamp")), jsonMsg.getString("user"), jsonMsg.getString("message"));
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
        Message originalMessage1 = new Message(new Timestamp(System.currentTimeMillis()), "user1", "message1");
        Message originalMessage2 = new Message(new Timestamp(System.currentTimeMillis()), "user2", "message2");

        when(serverRepository.findAll()).thenReturn(new ArrayList<>());

        List<Message> retrievedMessages = new ArrayList<>();
        clientSocket.on("msg", arg -> {
            JSONObject jsonMsg = (JSONObject) arg[0];
            Message incomingMessage = new Message(new Timestamp(jsonMsg.getLong("timestamp")), jsonMsg.getString("user"), jsonMsg.getString("message"));
            retrievedMessages.add(incomingMessage);
        });
        clientSocket.on(Socket.EVENT_CONNECT, objects -> {
            clientSocket.emit("join");
            clientSocket.emit("msg", originalMessage1.toJSON());
            clientSocket.emit("msg", originalMessage2.toJSON());
        });
        clientSocket.connect();

        ArgumentCaptor<Message> argumentCaptor = ArgumentCaptor.forClass(Message.class);
        try {
            await().atMost(2, SECONDS).until(() -> !retrievedMessages.isEmpty());
        } catch (org.awaitility.core.ConditionTimeoutException ignored) {
            fail("Expected " + asList(originalMessage1, originalMessage2) + " but got " + retrievedMessages);
        }
        verify(serverRepository, times(2)).save(argumentCaptor.capture());
        List<Message> capturedArgument = argumentCaptor.getAllValues();

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
}