package io.github.marcodiri.java_socketio_chatroom_client.view.swing;

import com.mongodb.MongoClient;
import com.mongodb.ServerAddress;
import io.github.marcodiri.java_socketio_chatroom_client.ChatroomClient;
import io.github.marcodiri.java_socketio_chatroom_client.view.swing.components.MessageBoard;
import io.github.marcodiri.java_socketio_chatroom_core.model.Message;
import io.github.marcodiri.java_socketio_chatroom_server.ChatroomServer;
import io.github.marcodiri.java_socketio_chatroom_server.model.ServerMessage;
import io.github.marcodiri.java_socketio_chatroom_server.repository.ServerRepository;
import io.github.marcodiri.java_socketio_chatroom_server.repository.mongo.ServerMongoRepository;
import io.socket.client.IO;
import org.assertj.swing.core.matcher.JButtonMatcher;
import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.fixture.JTextComponentFixture;
import org.assertj.swing.junit.testcase.AssertJSwingJUnitTestCase;
import org.junit.Test;

import java.net.URI;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.awaitility.Awaitility.await;

public class ClientSwingViewIT extends AssertJSwingJUnitTestCase {
    private FrameFixture window;

    private ServerRepository serverRepository;
    private MongoClient mongoClient;
    private ChatroomServer server;

    private ChatroomClient client;
    private ClientSwingView clientView;

    @Override
    public void onSetUp() {
        int mongoPort = Integer.parseInt(System.getProperty("mongo.port", "27017"));
        mongoClient = new MongoClient(new ServerAddress("localhost", mongoPort));

        serverRepository = new ServerMongoRepository(mongoClient);
        server = new ChatroomServer(serverRepository);

        try {
            server.start();
        } catch (Exception ignored) {
            fail("ServerWrapper startup failed");
        }

        GuiActionRunner.execute(() -> {
            clientView = new ClientSwingView(new MessageBoard());
            client = new ChatroomClient(URI.create("http://localhost:3000"), IO.Options.builder().build(), clientView);
            clientView.setClient(client);
            return clientView;
        });

        window = new FrameFixture(robot(), clientView);
        window.show(); // shows the frame to test
    }

    @Override
    public void onTearDown() throws Exception {
        server.stop();
        mongoClient.getDatabase(ServerMongoRepository.CHATROOM_DB_NAME).drop();
        mongoClient.close();
    }

    @Test
    public void testMessagesArePrintedOnConnect() {
        DateFormat dateFormat = new SimpleDateFormat("HH:mm");

        Timestamp timestamp1 = new Timestamp(0);
        Message olderMessage = new ServerMessage(timestamp1, "user1", "message1");

        Timestamp timestamp2 = new Timestamp(1);
        Message newerMessage = new ServerMessage(timestamp2, "user2", "message2");

        serverRepository.save(olderMessage);
        serverRepository.save(newerMessage);

        window.textBox("txtUsername").enterText("user3");
        window.button(JButtonMatcher.withText("Connect")).click();

        String expectedText = dateFormat.format(timestamp1) + " user1: message1" + System.lineSeparator()
                + dateFormat.format(timestamp2) + " user2: message2";
        JTextComponentFixture messageBoard = window.textBox("msgsTextPane");
        try {
            await().atMost(2, SECONDS).untilAsserted(() -> assertThat(messageBoard.text()).isEqualTo(expectedText));
        } catch (org.awaitility.core.ConditionTimeoutException ignored) {
            fail("Expected: " + expectedText + " but got: " + messageBoard.text());
        }
    }

    @Test
    public void testSentMessageIsSavedInDb() {
        window.textBox("txtUsername").enterText("user");
        window.button(JButtonMatcher.withText("Connect")).click();

        JTextComponentFixture txtMessage = window.textBox("txtMessage");
        try {
            await().atMost(2, SECONDS).untilAsserted(() -> txtMessage.requireEnabled());
        } catch (org.awaitility.core.ConditionTimeoutException ignored) {
            fail("Cannot connect to server");
        }
        txtMessage.enterText("Text");
        window.button(JButtonMatcher.withText("Send")).click();

        AtomicReference<Message> retrievedMessage = new AtomicReference<>();
        try {
            await().atMost(2, SECONDS).until(() -> {
                retrievedMessage.set(serverRepository.findAll().get(0));
                return retrievedMessage.get().getUser().equals("user") && retrievedMessage.get().getUserMessage().equals("Text");
            });
        } catch (org.awaitility.core.ConditionTimeoutException ignored) {
            fail("Expected: {user: user, message: Text} " +
                    "but got {user: " + retrievedMessage.get().getUser() + ", message: " + retrievedMessage.get().getUserMessage() + "}");
        }
    }

    @Test
    public void testErrorMessageIfUsernameIsAlreadyTaken() {
        server.getUsernameList().put("id1", "user1");

        window.textBox("txtUsername").enterText("user1");
        window.button(JButtonMatcher.withText("Connect")).click();
        JTextComponentFixture txtErrorMessage = window.textBox("txtErrorMessage");

        try {
            await().atMost(2, SECONDS).untilAsserted(() -> txtErrorMessage.requireText("Username is already taken"));
        } catch (org.awaitility.core.ConditionTimeoutException ignored) {
            fail("Expected [Username is already taken] but got [" + txtErrorMessage.text() + "]");
        }
    }


}
