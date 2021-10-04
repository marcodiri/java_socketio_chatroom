package io.github.marcodiri.java_socketio_chatroom_client.view.swing;

import io.github.marcodiri.java_socketio_chatroom_client.ChatroomClient;
import io.github.marcodiri.java_socketio_chatroom_client.view.swing.components.MessageBoard;
import io.github.marcodiri.java_socketio_chatroom_core.model.Message;
import io.github.marcodiri.java_socketio_chatroom_server.ChatroomServer;
import io.github.marcodiri.java_socketio_chatroom_server.model.ServerMessage;
import io.github.marcodiri.java_socketio_chatroom_server.repository.ServerRepository;
import io.socket.client.IO;
import org.assertj.swing.core.matcher.JButtonMatcher;
import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.fixture.JTextComponentFixture;
import org.assertj.swing.junit.testcase.AssertJSwingJUnitTestCase;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.net.URI;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ClientSwingViewIT extends AssertJSwingJUnitTestCase {
	private FrameFixture window;

	@Mock
	private ServerRepository serverRepository;
	@InjectMocks
	private ChatroomServer server;
	private AutoCloseable closeable;

	private ChatroomClient client;
	private ClientSwingView clientView;

	@Override
	public void onSetUp() {
		closeable = MockitoAnnotations.openMocks(this);

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
		closeable.close();
	}

	@Test
	public void testMessagesArePrintedOnConnect() {
		DateFormat dateFormat = new SimpleDateFormat("HH:mm");

		Timestamp timestamp1 = new Timestamp(0);
		Message olderMessage = new ServerMessage(timestamp1, "user1", "message1");

		Timestamp timestamp2 = new Timestamp(1);
		Message newerMessage = new ServerMessage(timestamp2, "user2", "message2");

		when(serverRepository.findAll()).thenReturn(asList(olderMessage, newerMessage));

		window.textBox("txtUsername").enterText("user3");
		window.button(JButtonMatcher.withText("Connect")).click();

		String expectedText = dateFormat.format(timestamp1) + " user1: message1" + System.lineSeparator()
				+ dateFormat.format(timestamp2) + " user2: message2";
		JTextComponentFixture messageBoard = window.textBox("msgsTextPane");
		try {
			await().atMost(2, SECONDS).untilAsserted(() -> assertThat(messageBoard.text()).isEqualTo(expectedText));
		} catch (org.awaitility.core.ConditionTimeoutException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testSentMessageIsSavedInDb() {
		String userTxt = "user";
		String messageTxt = "Text";
		window.textBox("txtUsername").enterText(userTxt);
		window.button(JButtonMatcher.withText("Connect")).click();

		JTextComponentFixture txtMessage = window.textBox("txtMessage");
		try {
			await().atMost(2, SECONDS).untilAsserted(txtMessage::requireEnabled);
		} catch (org.awaitility.core.ConditionTimeoutException ignored) {
			fail("Cannot connect to server");
		}
		txtMessage.enterText(messageTxt);
		window.button(JButtonMatcher.withText("Send")).click();

		ArgumentCaptor<Message> retrievedMessage = ArgumentCaptor.forClass(Message.class);
		try {
			await().atMost(2, SECONDS).untilAsserted(() -> verify(serverRepository).save(retrievedMessage.capture()));
		} catch (org.awaitility.core.ConditionTimeoutException e) {
			fail(e.getMessage());
		}
		assertThat(retrievedMessage.getValue().getUser()).isEqualTo(userTxt);
		assertThat(retrievedMessage.getValue().getUserMessage()).isEqualTo(messageTxt);
	}

	@Test
	public void testErrorMessageIfUsernameIsAlreadyTaken() {
		server.getUsernameList().put("id1", "user1");

		window.textBox("txtUsername").enterText("user1");
		window.button(JButtonMatcher.withText("Connect")).click();
		JTextComponentFixture txtErrorMessage = window.textBox("txtErrorMessage");

		try {
			await().atMost(2, SECONDS).untilAsserted(() -> txtErrorMessage.requireText("Username is already taken"));
		} catch (org.awaitility.core.ConditionTimeoutException e) {
			fail(e.getMessage());
		}
	}


}
