package io.github.marcodiri.java_socketio_chatroom_server;

import com.mongodb.MongoClient;
import com.mongodb.ServerAddress;
import io.github.marcodiri.java_socketio_chatroom_core.model.Message;
import io.github.marcodiri.java_socketio_chatroom_server.model.ServerMessage;
import io.github.marcodiri.java_socketio_chatroom_server.repository.ServerRepository;
import io.github.marcodiri.java_socketio_chatroom_server.repository.mongo.ServerMongoRepository;
import io.socket.client.IO;
import io.socket.client.Socket;
import org.json.JSONObject;
import org.junit.*;

import java.net.URI;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.awaitility.Awaitility.await;

public class ChatroomServerIT {

    private ChatroomServer chatroomServer;
    private Socket clientSocket;

    private static ServerRepository serverRepository;
    private static MongoClient mongoClient;

    @BeforeClass
    public static void setupMongoDb() {
        int mongoPort = Integer.parseInt(System.getProperty("mongo.port", "27017"));
        mongoClient = new MongoClient(new ServerAddress("localhost", mongoPort));

        serverRepository = new ServerMongoRepository(mongoClient);
    }

    @AfterClass
    public static void closeMongoDb() {
        mongoClient.close();
    }

    @Before
    public void setup() {
        chatroomServer = new ChatroomServer(serverRepository);

        try {
            chatroomServer.start();
        } catch (Exception ignored) {
            fail("ServerWrapper startup failed");
        }

        clientSocket = IO.socket(URI.create("http://localhost:3000"), IO.Options.builder().build());
    }

    @After
    public void closeConnections() throws Exception {
        mongoClient.getDatabase(ServerMongoRepository.CHATROOM_DB_NAME).drop();
        clientSocket.disconnect();
        chatroomServer.stop();
    }

    @Test
    public void testClientJoinRetrievesMessagesFromMongoDb() {
        ServerMessage msg1 = new ServerMessage(new Timestamp(0), "user1", "message1");
        ServerMessage msg2 = new ServerMessage(new Timestamp(1), "user2", "message2");
        serverRepository.save(msg1);
        serverRepository.save(msg2);

        List<ServerMessage> retrievedMessages = new ArrayList<>();
        clientSocket.on("msg", arg -> {
            JSONObject jsonMsg = (JSONObject) arg[0];
            ServerMessage incomingMessage = new ServerMessage(new Timestamp(jsonMsg.getLong("timestamp")), jsonMsg.getString("user"), jsonMsg.getString("message"));
            retrievedMessages.add(incomingMessage);
        });
        clientSocket.on("connected", objects -> clientSocket.emit("join", "user"));
        clientSocket.connect();

        try {
            await().atMost(2, SECONDS).until(() -> retrievedMessages.containsAll(asList(msg1, msg2)));
        } catch (org.awaitility.core.ConditionTimeoutException ignored) {
            fail("Expected " + asList(msg1, msg2) + " but got " + retrievedMessages);
        }

    }

    @Test
    public void testMessagesAreSavedInMongoDb() {
        ServerMessage originalMessage1 = new ServerMessage(new Timestamp(0), "user1", "message1");
        ServerMessage originalMessage2 = new ServerMessage(new Timestamp(1), "user2", "message2");

        clientSocket.on("connected", objects -> {
            clientSocket.emit("join", "user");
            clientSocket.emit("msg", originalMessage1.toJSON());
            clientSocket.emit("msg", originalMessage2.toJSON());
        });
        clientSocket.connect();

        AtomicReference<List<Message>> retrievedMessages = new AtomicReference<>();

        try {
            await().atMost(2, SECONDS).until(() -> {
                retrievedMessages.set(serverRepository.findAll());
                return retrievedMessages.get().size() == 2;
            });
        } catch (org.awaitility.core.ConditionTimeoutException ignored) {
            fail("Expected " + asList(originalMessage1, originalMessage2) + " but got " + retrievedMessages);
        }

        assertThat(retrievedMessages.get()).contains(originalMessage1, originalMessage2);
    }
}
