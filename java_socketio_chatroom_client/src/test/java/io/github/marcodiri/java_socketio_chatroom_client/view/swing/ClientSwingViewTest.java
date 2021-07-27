package io.github.marcodiri.java_socketio_chatroom_client.view.swing;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.awt.Component;
import java.awt.event.KeyEvent;
import java.sql.Timestamp;
import javax.swing.SwingUtilities;

import org.assertj.swing.annotation.GUITest;
import org.assertj.swing.core.matcher.JButtonMatcher;
import org.assertj.swing.core.matcher.JLabelMatcher;
import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.fixture.JButtonFixture;
import org.assertj.swing.fixture.JTextComponentFixture;
import org.assertj.swing.junit.runner.GUITestRunner;
import org.assertj.swing.junit.testcase.AssertJSwingJUnitTestCase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import io.github.marcodiri.java_socketio_chatroom_client.ChatroomClient;
import io.github.marcodiri.java_socketio_chatroom_client.model.ClientMessage;
import io.github.marcodiri.java_socketio_chatroom_client.view.swing.components.MessageBoard;
import io.github.marcodiri.java_socketio_chatroom_core.model.Message;

@RunWith(GUITestRunner.class)
public class ClientSwingViewTest extends AssertJSwingJUnitTestCase {

    private FrameFixture window;

    private ClientSwingView clientSwingView;

    private ChatroomClient client;

    private MessageBoard msgsBoard;

    @Override
    protected void onSetUp() {
        GuiActionRunner.execute(() -> {
            client = mock(ChatroomClient.class);
            msgsBoard = spy(new MessageBoard());
            clientSwingView = new ClientSwingView(msgsBoard);
            clientSwingView.setClient(client);
            return clientSwingView;
        });

        window = new FrameFixture(robot(), clientSwingView);
        window.show(); // shows the frame to test
    }

    @Test
    @GUITest
    public void testControlsInitialStates() {
        window.label(JLabelMatcher.withText("Username"));
        window.label(JLabelMatcher.withText("Messages"));
        window.textBox("txtUsername").requireEnabled();
        window.textBox("txtMessage").requireDisabled();
        window.textBox("txtErrorMessage").requireNotEditable();
        window.button(JButtonMatcher.withText("Send")).requireDisabled();
        window.button(JButtonMatcher.withText("Connect")).requireDisabled();
        window.button(JButtonMatcher.withText("Disconnect")).requireDisabled();
        window.textBox("msgsTextPane").requireDisabled();
    }

    @Test
    public void testRoomJoined() {
        clientSwingView.roomJoined("RoomName");
        try {
            await().atMost(2, SECONDS).untilAsserted(() -> window.button(JButtonMatcher.withText("Disconnect")).requireEnabled());
            await().atMost(2, SECONDS).untilAsserted(() -> window.textBox("txtMessage").requireEnabled());
            await().atMost(2, SECONDS).untilAsserted(() -> window.textBox("msgsTextPane").requireEnabled());
        } catch (org.awaitility.core.ConditionTimeoutException ignored) {
            fail("Component not enabled after room joined");
        }
    }

    @Test
    public void testAddMessage() {
        Message msg = new ClientMessage(new Timestamp(0), "user", "message");
        clientSwingView.addMessage(msg);
        try {
            await().atMost(2, SECONDS).untilAsserted(() -> verify(msgsBoard).newMessageNotify(msg));
        } catch (org.awaitility.core.ConditionTimeoutException ignored) {
            fail("Expected call to newMessageNotify with message: " + msg);
        }
    }

    @Test
    public void testShowError() {
        JTextComponentFixture txtErrorMessage = window.textBox("txtErrorMessage");
        clientSwingView.snapshot = mock(ClientSwingView.ViewSnapshot.class);

        clientSwingView.showError("Error!");
        try {
            await().atMost(2, SECONDS).untilAsserted(() -> assertThat(txtErrorMessage.text()).isEqualTo("Error!"));
        } catch (org.awaitility.core.ConditionTimeoutException ignored) {
            fail("Expected [Error!] but got [" + txtErrorMessage.text() + "]");
        }
        try {
            await().atMost(2, SECONDS).untilAsserted(() -> verify(clientSwingView.snapshot).restore());
        } catch (org.awaitility.core.ConditionTimeoutException ignored) {
            fail("restore not called");
        }
    }

    @Test
    public void testConnectBtnDisablesItselfAndTxtUsernameAndConnectsToServerAndResetErrorMessage() {
        JButtonFixture btnConnect = window.button(JButtonMatcher.withText("Connect"));
        JTextComponentFixture txtUsername = window.textBox("txtUsername");
        JTextComponentFixture txtErrorMessage = window.textBox("txtErrorMessage");

        setEnabled(btnConnect.target(), true);
        txtErrorMessage.setText("Error!");
        txtUsername.setText("User");
        btnConnect.click();
        btnConnect.requireDisabled();
        txtUsername.requireDisabled();
        txtErrorMessage.requireEmpty();
        verify(client).connect("User");
    }

    @Test
    public void testConnectBtnSavesPreviousState() {
        JButtonFixture btnConnect = window.button(JButtonMatcher.withText("Connect"));
        JTextComponentFixture txtUsername = window.textBox("txtUsername");

        setEnabled(btnConnect.target(), true);
        btnConnect.click();
        SwingUtilities.invokeLater(() -> clientSwingView.snapshot.restore());
        try {
            await().atMost(2, SECONDS).untilAsserted(() -> btnConnect.requireEnabled());
            await().atMost(2, SECONDS).untilAsserted(() -> txtUsername.requireEnabled());
            await().atMost(2, SECONDS).untilAsserted(() -> verify(client).disconnect());
        } catch (org.awaitility.core.ConditionTimeoutException ignored) {
            fail("Previous state not restored");
        }
    }

    @Test
    public void testDisconnectBtnDisablesControlsAndEnablesTxtUsernameAndDisconnectsFromServer() {
        JButtonFixture btnConnect = window.button(JButtonMatcher.withText("Connect"));
        JButtonFixture btnDisconnect = window.button(JButtonMatcher.withText("Disconnect"));
        JTextComponentFixture txtMessage = window.textBox("txtMessage");
        JTextComponentFixture msgsTextPane = window.textBox("msgsTextPane");
        JTextComponentFixture txtUsername = window.textBox("txtUsername");

        setEnabled(btnConnect.target(), false);
        setEnabled(btnDisconnect.target(), true);
        setEnabled(txtMessage.target(), true);
        setEnabled(msgsTextPane.target(), true);
        setEnabled(txtUsername.target(), false);

        btnDisconnect.click();
        btnConnect.requireEnabled();
        btnDisconnect.requireDisabled();
        txtMessage.requireDisabled();
        msgsTextPane.requireDisabled();
        msgsTextPane.requireEmpty();
        txtUsername.requireEnabled();
        verify(client).disconnect();
    }

    @Test
    public void testTxtUsernameEnablesConnectBtnWhenIsNotEmpty() {
        JButtonFixture btnConnect = window.button(JButtonMatcher.withText("Connect"));
        btnConnect.requireDisabled();
        JTextComponentFixture txtConnect = window.textBox("txtUsername");

        txtConnect.enterText("Username");
        btnConnect.requireEnabled();
    }

    @Test
    public void testTxtUsernameDoesNotEnableConnectBtnWhenOnlyBlankCharsAreTyped() {
        JButtonFixture btnConnect = window.button(JButtonMatcher.withText("Connect"));
        btnConnect.requireDisabled();
        JTextComponentFixture txtConnect = window.textBox("txtUsername");

        txtConnect.enterText(" ");
        btnConnect.requireDisabled();
    }

    @Test
    public void testTxtUsernameDisablesConnectBtnWhenBecomesEmpty() {
        JTextComponentFixture txtUsername = window.textBox("txtUsername");
        JButtonFixture btnConnect = window.button(JButtonMatcher.withText("Connect"));

        txtUsername.setText("Username");

        setEnabled(btnConnect.target(), true);
        btnConnect.requireEnabled();

        txtUsername.deleteText();
        btnConnect.requireDisabled();
    }

    @Test
    public void testTxtMessageEnablesSendBtnWhenIsNotEmpty() {
        JButtonFixture btnSend = window.button(JButtonMatcher.withText("Send"));
        btnSend.requireDisabled();
        JTextComponentFixture txtMessage = window.textBox("txtMessage");
        setEnabled(txtMessage.target(), true);

        txtMessage.enterText("Text");
        btnSend.requireEnabled();
    }

    @Test
    public void testTxtMessageDoesNotEnableSendBtnWhenOnlyBlankCharsAreTyped() {
        JButtonFixture btnSend = window.button(JButtonMatcher.withText("Send"));
        btnSend.requireDisabled();
        JTextComponentFixture txtMessage = window.textBox("txtMessage");
        setEnabled(txtMessage.target(), true);

        txtMessage.enterText(" ");
        btnSend.requireDisabled();
    }

    @Test
    public void testTxtMessageDisablesSendBtnWhenBecomesEmpty() {
        JTextComponentFixture txtMessage = window.textBox("txtMessage");
        JButtonFixture btnSend = window.button(JButtonMatcher.withText("Send"));

        setEnabled(txtMessage.target(), true);
        txtMessage.setText("Text");

        setEnabled(btnSend.target(), true);
        btnSend.requireEnabled();

        txtMessage.deleteText();
        btnSend.requireDisabled();
    }

    @Test
    public void testTxtMessageDoesNotSendMessageWhenEnterIsPressedAndNoTextIsTyped() {
        JTextComponentFixture txtMessage = window.textBox("txtMessage");

        setEnabled(txtMessage.target(), true);
        txtMessage.pressAndReleaseKeys(KeyEvent.VK_ENTER);

        verifyNoInteractions(client);
    }

    @Test
    public void testTxtMessageDoesNotSendMessageWhenEnterIsPressedAndOnlyBlankCharsAreTyped() {
        JTextComponentFixture txtMessage = window.textBox("txtMessage");

        setEnabled(txtMessage.target(), true);
        txtMessage.enterText(" ");
        txtMessage.pressAndReleaseKeys(KeyEvent.VK_ENTER);

        verifyNoInteractions(client);
    }

    @Test
    public void testTxtMessageSendsMessageWhenTextIsTypedAndEnterIsPressed() {
        JTextComponentFixture txtUsername = window.textBox("txtUsername");
        JTextComponentFixture txtMessage = window.textBox("txtMessage");

        String username = "Username";
        String message = "Text";
        txtUsername.setText(username);
        setEnabled(txtMessage.target(), true);
        txtMessage.enterText(message);
        txtMessage.pressAndReleaseKeys(KeyEvent.VK_ENTER);

        ArgumentCaptor<ClientMessage> captor = ArgumentCaptor.forClass(ClientMessage.class);
        verify(client).sendMessage(captor.capture());
        txtMessage.requireEmpty();

        assertThat(captor.getValue().getUser()).isEqualTo(username);
        assertThat(captor.getValue().getUserMessage()).isEqualTo(message);
    }

    @Test
    public void testBtnSendSendsMessageAndClearsTxtMessageAndDisablesItself() {
        JTextComponentFixture txtUsername = window.textBox("txtUsername");
        JTextComponentFixture txtMessage = window.textBox("txtMessage");
        JButtonFixture btnSend = window.button(JButtonMatcher.withText("Send"));


        String username = "Username";
        String message = "Text";
        txtUsername.setText(username);
        setEnabled(txtMessage.target(), true);
        setEnabled(btnSend.target(), true);
        txtMessage.setText(message);

        ArgumentCaptor<ClientMessage> captor = ArgumentCaptor.forClass(ClientMessage.class);

        btnSend.click();
        verify(client).sendMessage(captor.capture());
        txtMessage.requireEmpty();
        btnSend.requireDisabled();

        assertThat(captor.getValue().getUser()).isEqualTo(username);
        assertThat(captor.getValue().getUserMessage()).isEqualTo(message);
    }

    private void setEnabled(Component component, boolean enable) {
        SwingUtilities.invokeLater(() -> component.setEnabled(enable));
        try {
            await().atMost(2, SECONDS).until(() -> enable == component.isEnabled());
        } catch (org.awaitility.core.ConditionTimeoutException ignored) {
            fail("Could not set component");
        }
    }

}
