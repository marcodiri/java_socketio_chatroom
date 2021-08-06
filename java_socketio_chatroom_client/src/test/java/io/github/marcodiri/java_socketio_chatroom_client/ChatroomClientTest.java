package io.github.marcodiri.java_socketio_chatroom_client;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.*;

import java.net.SocketException;
import java.net.URI;
import java.sql.Timestamp;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import io.socket.client.Socket;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import io.github.marcodiri.java_socketio_chatroom_client.model.ClientMessage;
import io.github.marcodiri.java_socketio_chatroom_client.view.ClientView;
import io.github.marcodiri.java_socketio_chatroom_server_mock.ChatroomServerMock;
import io.socket.client.IO;

public class ChatroomClientTest {

    private ChatroomClient.ChatroomClientHandlers handlers;
    private ClientView view;

    private ChatroomClient client;

    private ChatroomServerMock serverMock;

    @Before
    public void setup() {
        view = mock(ClientView.class);
        handlers = mock(ChatroomClient.ChatroomClientHandlers.class);
        client = new ChatroomClient(URI.create("http://localhost:3000"), IO.Options.builder().build(), handlers, view);

        serverMock = new ChatroomServerMock();
        try {
            serverMock.start();
        } catch (Exception ignored) {
            fail("Server startup failed");
        }
    }

    @After
    public void closeConnections() {
        client.disconnect();
        try {
            serverMock.stop();
        } catch (Exception e) {
            fail("Failed to stop the server");
        }
    }

    @Test
    public void testInit() {
        assertThat(client.getSocket().hasListeners("connected")).isTrue();
        assertThat(client.getSocket().hasListeners(Socket.EVENT_DISCONNECT)).isTrue();

        serverMock.handleNamespaceEvent("connection", arg -> serverMock.getSocket().send("connected"));

        client.getSocket().connect();

        try {
            await().atMost(2, SECONDS).untilAsserted(() -> verify(handlers).connectedHandler());
        } catch (org.awaitility.core.ConditionTimeoutException ignored) {
            fail("connectedHandler not called");
        }

        client.getSocket().disconnect();

        try {
            await().atMost(2, SECONDS).untilAsserted(() -> verify(handlers).disconnectedHandler());
        } catch (org.awaitility.core.ConditionTimeoutException ignored) {
            fail("disconnectedHandler not called");
        }

    }

    @Test
    public void testConnect() {
        String username = "user";

        client.connect(username);

        try {
            await().atMost(2, SECONDS).until(() -> client.getSocket().connected());
        } catch (org.awaitility.core.ConditionTimeoutException ignored) {
            fail("Client cannot connect to server");
        }

        assertThat(client.username).isEqualTo(username);
    }

    @Test
    public void testDisconnect() {
        client.getSocket().connect();
        try {
            await().atMost(2, SECONDS).until(() -> client.getSocket().connected());
        } catch (org.awaitility.core.ConditionTimeoutException ignored) {
            fail("Cannot connect to server");
        }

        client.disconnect();
        try {
            await().atMost(2, SECONDS).until(() -> !client.getSocket().connected());
        } catch (org.awaitility.core.ConditionTimeoutException ignored) {
            fail("Cannot disconnect from server");
        }

        assertThat(client.username).isNull();
    }

    @Test
    public void testSendMessageWhenClientNotConnected() {
        ClientMessage msg = new ClientMessage(new Timestamp(0), "user", "message");

        assertThatThrownBy(() -> client.sendMessage(msg)).isInstanceOf(SocketException.class)
                .hasMessage("Unable to send message when not connected to server");
    }

    @Test
    public void testSendMessageWhenClientConnected() {
        AtomicReference<JSONObject> receivedMsg = new AtomicReference<>();

        serverMock.handleNamespaceEvent("connection", arg -> serverMock.getSocket().send("connected"));
        serverMock.handleEvent("msg", arg -> receivedMsg.set((JSONObject) arg[0]));

        client.getSocket().connect();
        try {
            await().atMost(2, SECONDS).until(() -> client.getSocket().connected());
        } catch (org.awaitility.core.ConditionTimeoutException ignored) {
            fail("Client could not connect to server");
        }

        client.connected.set(true);

        ClientMessage msg = new ClientMessage(new Timestamp(0), "user", "message");
        try {
            client.sendMessage(msg);
        } catch (SocketException e) {
            fail(e.getMessage());
        }

        try {
            await().atMost(2, SECONDS).untilAsserted(() -> assertThat(receivedMsg.get()).isNotNull());
            await().atMost(2, SECONDS).until(() -> msg.equals(new ClientMessage(receivedMsg.get())));
        } catch (org.awaitility.core.ConditionTimeoutException ignored) {
            fail("Server did not receive the correct message");
        }
    }

}
