package io.github.marcodiri.java_socketio_chatroom_e2e;

import static org.assertj.swing.launcher.ApplicationLauncher.*;
import static org.awaitility.Awaitility.await;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.*;

import java.sql.Timestamp;
import javax.swing.JFrame;

import org.assertj.swing.annotation.GUITest;
import org.assertj.swing.core.GenericTypeMatcher;
import org.assertj.swing.core.matcher.JButtonMatcher;
import org.assertj.swing.finder.WindowFinder;
import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.fixture.JButtonFixture;
import org.assertj.swing.fixture.JTextComponentFixture;
import org.assertj.swing.junit.runner.GUITestRunner;
import org.assertj.swing.junit.testcase.AssertJSwingJUnitTestCase;
import org.bson.Document;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.mongodb.MongoClient;
import com.mongodb.ServerAddress;

@RunWith(GUITestRunner.class)
public class SingleChatroomClientE2E extends AssertJSwingJUnitTestCase {

	public static final String CHATROOM_DB_NAME = "chatroom";
	public static final String MESSAGES_COLLECTION_NAME = "messages";

	private static final Timestamp MESSAGE_1_TIMESTAMP = new Timestamp(0);
	private static final String MESSAGE_1_USER = "user1";
	private static final String MESSAGE_1_TEXT = "text1";
	private static final Timestamp MESSAGE_2_TIMESTAMP = new Timestamp(1000000);
	private static final String MESSAGE_2_USER = "user2";
	private static final String MESSAGE_2_TEXT = "text2";

	private MongoClient mongoClient;

	private FrameFixture window;

	@BeforeClass
	public static void setup() {
		// start the Server
		application("io.github.marcodiri.java_socketio_chatroom_server.App").start();
	}

	@Override
	protected void onSetUp() throws Exception {
		int mongoPort = Integer.parseInt(System.getProperty("mongo.port", "27017"));
		mongoClient = new MongoClient(new ServerAddress("localhost", mongoPort));
		// always start with an empty database
		mongoClient.getDatabase(CHATROOM_DB_NAME).drop();
		// add some messages to the database
		addTestMessageToDatabase(MESSAGE_1_TIMESTAMP, MESSAGE_1_USER, MESSAGE_1_TEXT);
		addTestMessageToDatabase(MESSAGE_2_TIMESTAMP, MESSAGE_2_USER, MESSAGE_2_TEXT);
		// start the Client
		application("io.github.marcodiri.java_socketio_chatroom_client.App").start();
		// get a reference of Client JFrame
		window = WindowFinder.findFrame(new GenericTypeMatcher<JFrame>(JFrame.class) {
			@Override
			protected boolean isMatching(JFrame frame) {
				return "Socket.io Chatroom".equals(frame.getTitle()) && frame.isShowing();
			}
		}).using(robot());
	}

	@Override
	protected void onTearDown() {
		mongoClient.close();
	}

	@Test
	@GUITest
	public void testOnConnectAllDatabaseElementsAreShown() {
		JButtonFixture btnConnect = window.button(JButtonMatcher.withText("Connect"));
		JButtonFixture btnDisconnect = window.button(JButtonMatcher.withText("Disconnect"));
		JTextComponentFixture txtUsername = window.textBox("txtUsername");
		JTextComponentFixture msgsTextPane = window.textBox("msgsTextPane");

		txtUsername.enterText("User");
		btnConnect.click();

		try {
			await().atMost(2, SECONDS).untilAsserted(() -> assertThat(msgsTextPane.text()).contains(MESSAGE_1_USER,
					MESSAGE_1_TEXT, MESSAGE_2_USER, MESSAGE_2_TEXT));
		} catch (org.awaitility.core.ConditionTimeoutException ignored) {
			fail("Saved message not displayed in message board");
		}
		
		btnDisconnect.click();
	}

	@Test
	@GUITest
	public void testSendButtonSuccess() {
		JButtonFixture btnConnect = window.button(JButtonMatcher.withText("Connect"));
		JButtonFixture btnDisconnect = window.button(JButtonMatcher.withText("Disconnect"));
		JButtonFixture btnSend = window.button(JButtonMatcher.withText("Send"));
		JTextComponentFixture txtUsername = window.textBox("txtUsername");
		JTextComponentFixture txtMessage = window.textBox("txtMessage");
		JTextComponentFixture msgsTextPane = window.textBox("msgsTextPane");


		txtUsername.enterText(MESSAGE_1_USER);
		btnConnect.click();

		try {
			await().atMost(2, SECONDS).untilAsserted(() -> txtMessage.requireEnabled());
		} catch (org.awaitility.core.ConditionTimeoutException ignored) {
			fail("Message text box not enabled");
		}

		txtMessage.enterText(MESSAGE_1_TEXT);
		btnSend.click();

		try {
			await().atMost(2, SECONDS).untilAsserted(() -> assertThat(msgsTextPane.text()).contains(MESSAGE_1_USER, MESSAGE_1_TEXT));
		} catch (org.awaitility.core.ConditionTimeoutException ignored) {
			fail("Sent message not displayed in message board");
		}

		btnDisconnect.click();
	}

	private void addTestMessageToDatabase(Timestamp timestamp, String user, String message) {
		mongoClient.getDatabase(CHATROOM_DB_NAME).getCollection(MESSAGES_COLLECTION_NAME).insertOne(new Document()
				.append("timestamp", timestamp.getTime()).append("user", user).append("message", message));
	}

}
