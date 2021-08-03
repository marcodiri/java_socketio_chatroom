package io.github.marcodiri.java_socketio_chatroom_e2e;

import static org.assertj.swing.launcher.ApplicationLauncher.*;
import static org.awaitility.Awaitility.await;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.*;

import java.awt.Frame;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

import io.github.marcodiri.java_socketio_chatroom_server.App;
import org.assertj.swing.annotation.GUITest;
import org.assertj.swing.core.matcher.JButtonMatcher;
import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.fixture.JButtonFixture;
import org.assertj.swing.fixture.JTextComponentFixture;
import org.assertj.swing.junit.runner.GUITestRunner;
import org.assertj.swing.junit.testcase.AssertJSwingJUnitTestCase;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.mongodb.MongoClient;
import com.mongodb.ServerAddress;

@RunWith(GUITestRunner.class)
public class MultipleChatroomClientE2E extends AssertJSwingJUnitTestCase {

	public static final String CHATROOM_DB_NAME = "chatroom";
	public static final String MESSAGES_COLLECTION_NAME = "messages";

	private static final String MESSAGE_1_USER = "user1";
	private static final String MESSAGE_1_TEXT = "text1";
	private static final String MESSAGE_2_USER = "user2";
	private static final String MESSAGE_2_TEXT = "text2";

	private MongoClient mongoClient;

	private static Thread serverThread;

	List<Frame> frames;

	@BeforeClass
	public static void setup() {
		// start the Server
		serverThread = new Thread(() -> App.main(null));
		serverThread.start();
	}

	@AfterClass
	public static void stopServer() throws InterruptedException {
		serverThread.interrupt();
		serverThread.join();
	}

	@Override
	protected void onSetUp() {
		int mongoPort = Integer.parseInt(System.getProperty("mongo.port", "27017"));
		mongoClient = new MongoClient(new ServerAddress("localhost", mongoPort));
		// always start with an empty database
		mongoClient.getDatabase(CHATROOM_DB_NAME).drop();

		// start the Clients
		application("io.github.marcodiri.java_socketio_chatroom_client.App").start();
		application("io.github.marcodiri.java_socketio_chatroom_client.App").start();

		// get a reference of Clients JFrame
		try {
            await().atMost(5, SECONDS).until(() -> {
            	frames = new ArrayList<>();
            	Frame[] fs = Frame.getFrames();
            	for (Frame f : fs) {
					if ("Socket.io Chatroom".equals(f.getTitle()) && f.isShowing()) {
            			frames.add(f);
					}
            	}
            	return frames.size() == 2;
            });
        } catch (org.awaitility.core.ConditionTimeoutException ignored) {
            fail(String.format("Expected [2] frames but got [%d]", frames.size()));
        }
	}

	@Override
	protected void onTearDown() {
		mongoClient.close();
	}

	@Test
	@GUITest
	public void testConnectedClientsCanExchangeMessages() {
		FrameFixture window1 = new FrameFixture(robot(), frames.get(0));
		FrameFixture window2 = new FrameFixture(robot(), frames.get(1));
		window1.moveTo(new Point(0, 0));
		window2.moveTo(new Point(500, 0));

		JButtonFixture btnConnect1 = window1.button(JButtonMatcher.withText("Connect"));
		JButtonFixture btnDisconnect1 = window1.button(JButtonMatcher.withText("Disconnect"));
		JButtonFixture btnSend1 = window1.button(JButtonMatcher.withText("Send"));
		JTextComponentFixture txtUsername1 = window1.textBox("txtUsername");
		JTextComponentFixture txtMessage1 = window1.textBox("txtMessage");
		JTextComponentFixture msgsTextPane1 = window1.textBox("msgsTextPane");

		JButtonFixture btnConnect2 = window2.button(JButtonMatcher.withText("Connect"));
		JButtonFixture btnDisconnect2 = window2.button(JButtonMatcher.withText("Disconnect"));
		JButtonFixture btnSend2 = window2.button(JButtonMatcher.withText("Send"));
		JTextComponentFixture txtUsername2 = window2.textBox("txtUsername");
		JTextComponentFixture txtMessage2 = window2.textBox("txtMessage");
		JTextComponentFixture msgsTextPane2 = window2.textBox("msgsTextPane");

		txtUsername1.enterText(MESSAGE_1_USER);
		btnConnect1.click();
		
		try {
			await().atMost(2, SECONDS).untilAsserted(() -> txtMessage1.requireEnabled());
		} catch (org.awaitility.core.ConditionTimeoutException ignored) {
			fail("Client1 message text box not enabled");
		}
		
		txtUsername2.enterText(MESSAGE_2_USER);
		btnConnect2.click();
		
		try {
			await().atMost(2, SECONDS).untilAsserted(() -> txtMessage2.requireEnabled());
		} catch (org.awaitility.core.ConditionTimeoutException ignored) {
			fail("Client2 message text box not enabled");
		}

		// Client1 sends message
		txtMessage1.enterText(MESSAGE_1_TEXT);
		btnSend1.click();

		try {
			await().atMost(2, SECONDS).untilAsserted(() -> assertThat(msgsTextPane2.text()).contains(MESSAGE_1_USER, MESSAGE_1_TEXT));
		} catch (org.awaitility.core.ConditionTimeoutException ignored) {
			fail("Client1 message not displayed in message board of Client2");
		}

		// Client2 sends message
		txtMessage2.enterText(MESSAGE_2_TEXT);
		btnSend2.click();

		try {
			await().atMost(2, SECONDS).untilAsserted(() -> assertThat(msgsTextPane1.text()).contains(MESSAGE_2_USER, MESSAGE_2_TEXT));
		} catch (org.awaitility.core.ConditionTimeoutException ignored) {
			fail("Client2 message not displayed in message board of Client1");
		}

		btnDisconnect1.click();
		btnDisconnect2.click();
	}

	@Test
	@GUITest
	public void testErrorIfUsernameIsAlreadyTaken() {
		FrameFixture window1 = new FrameFixture(robot(), frames.get(0));
		FrameFixture window2 = new FrameFixture(robot(), frames.get(1));
		window1.moveTo(new Point(0, 0));
		window2.moveTo(new Point(500, 0));

		JButtonFixture btnConnect1 = window1.button(JButtonMatcher.withText("Connect"));
		JButtonFixture btnDisconnect1 = window1.button(JButtonMatcher.withText("Disconnect"));
		JTextComponentFixture txtUsername1 = window1.textBox("txtUsername");

		JButtonFixture btnConnect2 = window2.button(JButtonMatcher.withText("Connect"));
		JTextComponentFixture txtUsername2 = window2.textBox("txtUsername");
        JTextComponentFixture txtErrorMessage2 = window2.textBox("txtErrorMessage");

		txtUsername1.enterText(MESSAGE_1_USER);
		btnConnect1.click();

		try {
			await().atMost(2, SECONDS).untilAsserted(() -> btnDisconnect1.requireEnabled());
		} catch (org.awaitility.core.ConditionTimeoutException ignored) {
			fail("Client1 message disconnect button not enabled");
		}

		txtUsername2.enterText(MESSAGE_1_USER);
		btnConnect2.click();

		try {
			await().atMost(2, SECONDS).untilAsserted(() -> assertThat(txtErrorMessage2.text()).isNotEmpty());
		} catch (org.awaitility.core.ConditionTimeoutException ignored) {
			fail("Error not shown");
		}

		btnDisconnect1.click();
	}

}
