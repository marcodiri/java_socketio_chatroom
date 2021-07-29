package io.github.marcodiri.java_socketio_chatroom_client;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.net.SocketException;
import java.net.URI;
import java.sql.Timestamp;
import java.util.concurrent.atomic.AtomicReference;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import io.github.marcodiri.java_socketio_chatroom_client.model.ClientMessage;
import io.github.marcodiri.java_socketio_chatroom_client.view.ClientView;
import io.github.marcodiri.java_socketio_chatroom_server_mock.ChatroomServerMock;
import io.socket.client.IO;

public class ChatroomClientTest {

    private ClientView view;

    private ChatroomClient client;

    private ChatroomServerMock serverMock;

    @Before
    public void setup() {
        view = mock(ClientView.class);
        client = new ChatroomClient(URI.create("http://localhost:3000"), IO.Options.builder().build(), view);

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
    public void testConnect() {
        client.connect("user");
        try {
            await().atMost(2, SECONDS).untilTrue(serverMock.socketIsInRoom());
        } catch (org.awaitility.core.ConditionTimeoutException ignored) {
            fail("Socket could not join the room");
        }

        assertThat(client.getSocket().hasListeners("msg")).isTrue();
        assertThat(client.getSocket().hasListeners("joined")).isTrue();
        assertThat(client.getSocket().hasListeners("error")).isTrue();
    }

    @Test
    public void testDisconnect() {
        client.getSocket().on("event1", args -> {
        });
        client.getSocket().on("event2", args -> {
        });
        client.getSocket().connect();
        try {
            await().atMost(2, SECONDS).until(() -> client.isConnected());
        } catch (org.awaitility.core.ConditionTimeoutException ignored) {
            fail("Cannot connect to server");
        }

        client.disconnect();
        try {
            await().atMost(2, SECONDS).until(() -> !client.isConnected());
        } catch (org.awaitility.core.ConditionTimeoutException ignored) {
            fail("Cannot disconnect from server");
        }

        assertThat(client.getSocket().hasListeners("event1")).isFalse();
        assertThat(client.getSocket().hasListeners("event2")).isFalse();
    }

    @Test
    public void testSendMessageWhenClientNotConnected() {
        ClientMessage msg = new ClientMessage(new Timestamp(0), "user", "message");

        assertThatThrownBy(() -> client.sendMessage(msg)).isInstanceOf(SocketException.class)
                .hasMessage("Unable to send message when not connected to server");
    }

    @Test
    public void testSendMessageWhenClientConnected() {
        client.getSocket().connect();
        try {
            await().atMost(2, SECONDS).until(() -> client.isConnected());
        } catch (org.awaitility.core.ConditionTimeoutException ignored) {
            fail("Client could not connect to server");
        }

        AtomicReference<JSONObject> receivedMsg = new AtomicReference<>();

        try {
            serverMock.handleEvent("msg", arg -> receivedMsg.set((JSONObject) arg[0]));
        } catch (NullPointerException e) {
            fail("Socket is not connected to server");
        }

        ClientMessage msg = new ClientMessage(new Timestamp(0), "user", "message");
        try {
            client.sendMessage(msg);
        } catch (SocketException e) {
            fail(e.getMessage());
        }

        try {
            await().atMost(2, SECONDS).until(() -> msg.equals(new ClientMessage(receivedMsg.get())));
        } catch (org.awaitility.core.ConditionTimeoutException ignored) {
            fail("Server did not receive the correct message");
        }
    }

    @Test
    public void testHandleMessage() {
        client.getSocket().connect();
        client.handleMessage();
        try {
            await().atMost(2, SECONDS).until(() -> client.isConnected());
        } catch (org.awaitility.core.ConditionTimeoutException ignored) {
            fail("Client could not connect to server");
        }

        ClientMessage msg = new ClientMessage(new Timestamp(0), "user", "message");
        try {
            serverMock.sendEvent("msg", msg.toJSON());
        } catch (NullPointerException e) {
            fail("Socket is null");
        }

        try {
            await().atMost(2, SECONDS).untilAsserted(() -> verify(view).addMessage(msg));
        } catch (org.awaitility.core.ConditionTimeoutException ignored) {
            fail("AddMessage on ClientView was not called");
        }
    }

    @Test
    public void testHandleJoin() {
        client.getSocket().connect();
        client.handleJoin();
        try {
            await().atMost(2, SECONDS).until(() -> client.isConnected());
        } catch (org.awaitility.core.ConditionTimeoutException ignored) {
            fail("Client could not connect to server");
        }

        serverMock.handleEvent("join", arg -> serverMock.sendEvent("joined", new JSONObject("{roomName: RoomName}")));
        client.getSocket().emit("join");

        try {
            await().atMost(2, SECONDS).untilAsserted(() -> verify(view).roomJoined("RoomName"));
        } catch (org.awaitility.core.ConditionTimeoutException ignored) {
            fail("roomJoined on ClientView was not called");
        }

    }

    @Test
    public void testHandleError() {
        client.getSocket().connect();
        client.handleError();
        try {
            await().atMost(2, SECONDS).until(() -> client.isConnected());
        } catch (org.awaitility.core.ConditionTimeoutException ignored) {
            fail("Client could not connect to server");
        }

        String errorMessage = "Username is already taken";
        serverMock.sendEvent("error", new JSONObject("{message: " + errorMessage + "}"));

        try {
            await().atMost(2, SECONDS).untilAsserted(() -> verify(view).showError(errorMessage));
        } catch (org.awaitility.core.ConditionTimeoutException ignored) {
            fail("showError on ClientView was not called or message is invalid");
        }
    }

}
