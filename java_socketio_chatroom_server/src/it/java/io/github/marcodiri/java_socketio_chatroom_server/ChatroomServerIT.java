package io.github.marcodiri.java_socketio_chatroom_server;

import io.github.marcodiri.java_socketio_chatroom_server.model.Message;
import io.github.marcodiri.java_socketio_chatroom_server.repository.ServerRepository;

import io.socket.client.IO;
import io.socket.client.Socket;
import org.json.JSONObject;
import org.junit.*;
import org.mockito.*;

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
    ServerRepository serverRepository;

    @InjectMocks
    private ChatroomServer chatroomServer;

    private AutoCloseable closeable;

    @BeforeClass
    public static void socketSetup() {

    }

    @AfterClass
    public static void socketDisconnect() {
    }

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
        clientSocket.connect();
        clientSocket.emit("join");


        await().atMost(2, SECONDS).until(() -> retrievedMessages.contains(msg1) && retrievedMessages.contains(msg2));
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
        clientSocket.connect();
        clientSocket.emit("join");
        clientSocket.emit("msg", originalMessage1.toJSON());
        clientSocket.emit("msg", originalMessage2.toJSON());

        await().atMost(2, SECONDS).until(() -> retrievedMessages.contains(originalMessage1) && retrievedMessages.contains(originalMessage2));
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
        clientSocket.connect();
        clientSocket.emit("join");
        clientSocket.emit("msg", originalMessage1.toJSON());
        clientSocket.emit("msg", originalMessage2.toJSON());

        ArgumentCaptor<Message> argumentCaptor = ArgumentCaptor.forClass(Message.class);
        await().atMost(2, SECONDS).until(() -> !retrievedMessages.isEmpty());
        verify(serverRepository, times(2)).save(argumentCaptor.capture());
        List<Message> capturedArgument = argumentCaptor.getAllValues();

        assertThat(capturedArgument.contains(originalMessage1) && capturedArgument.contains(originalMessage2)).isTrue();
    }

    @Test
    public void testRoomSizeWhenClientJoinsAndWhenLeaves() {
        clientSocket.connect();
        clientSocket.emit("join");
        await().atMost(2, SECONDS).until(() -> chatroomServer.getNamespace().getAdapter().listClients("Chatroom").length == 1);
        clientSocket.emit("leave");
        await().atMost(2, SECONDS).until(() -> chatroomServer.getNamespace().getAdapter().listClients("Chatroom").length == 0);

    }
}