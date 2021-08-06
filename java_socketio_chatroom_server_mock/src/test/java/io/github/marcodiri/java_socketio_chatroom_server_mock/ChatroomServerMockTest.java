package io.github.marcodiri.java_socketio_chatroom_server_mock;

import io.socket.client.IO;
import io.socket.client.Socket;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.net.URI;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.await;

public class ChatroomServerMockTest {

    private ChatroomServerMock serverMock;

    private Socket clientSocket;

    @Before
    public void setup() {
        serverMock = new ChatroomServerMock();
        try {
            serverMock.start();
        } catch (Exception ignored) {
            fail("ServerWrapper startup failed");
        }

        clientSocket = IO.socket(URI.create("http://localhost:3000"), IO.Options.builder().build());
    }

    @After
    public void closeConnections() throws Exception {
        clientSocket.disconnect();
        serverMock.stop();
    }
    
    @Test
    public void testStart() {
    	assertThat(serverMock.getNamespace().hasListeners("connection")).isTrue();

    	try {
    		await().atMost(2, SECONDS).until(() -> serverMock.isStarted());
    	} catch (org.awaitility.core.ConditionTimeoutException ignored) {
    		fail("Server cannot be started");
    	}
    	
    	clientSocket.connect();
    	try {
    		await().atMost(2, SECONDS).until(() -> clientSocket.connected());
    	} catch (org.awaitility.core.ConditionTimeoutException ignored) {
    		fail("Client could not connect to server");
    	}
    	
    	assertThat(serverMock.getSocket()).isNotNull();
    	assertThat(serverMock.getSocket().getId()).isEqualTo(clientSocket.id());
    }
    
    @Test
    public void testStop() throws Exception {
    	serverMock.stop();
    	try {
    		await().atMost(2, SECONDS).until(() -> !serverMock.isStarted());
    	} catch (org.awaitility.core.ConditionTimeoutException ignored) {
    		fail("Server cannot be stopped");
    	}
    }

    @Test
    public void testHandleNamespaceEvent() {
    	serverMock.handleNamespaceEvent("event", args -> {});
    	
        assertThat(serverMock.getNamespace().hasListeners("event")).isTrue();
    }

    @Test
    public void testHandleEventWhenClientNotConnected() {
        assertThat(serverMock.getSocket()).isNull();

        assertThatThrownBy(() -> serverMock.handleEvent("event", arg -> {
        }))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("socket is null");
    }

    @Test
    public void testHandleEventWhenClientConnected() {
        clientSocket.connect();
        try {
            await().atMost(2, SECONDS).until(() -> clientSocket.connected());
        } catch (org.awaitility.core.ConditionTimeoutException ignored) {
            fail("Client could not connect to server");
        }

        try {
            serverMock.handleEvent("event", arg -> {
            });
        } catch (NullPointerException e) {
            fail("Socket is null");
        }

        assertThat(serverMock.getSocket().hasListeners("event")).isTrue();
    }

    @Test
    public void testSendEventWhenClientNotConnected() {
        assertThat(serverMock.getSocket()).isNull();

        assertThatThrownBy(() -> serverMock.sendEvent("event", null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("socket is null");
    }

    @Test
    public void testSendEventWhenClientConnected() {
        AtomicBoolean eventReceived = new AtomicBoolean(false);

        clientSocket.on("event", arg -> eventReceived.set(true));

        clientSocket.connect();
        try {
            await().atMost(2, SECONDS).until(() -> clientSocket.connected());
        } catch (org.awaitility.core.ConditionTimeoutException ignored) {
            fail("Client could not connect to server");
        }

        try {
            serverMock.sendEvent("event", new JSONObject());
        } catch (NullPointerException e) {
            fail("Socket is null");
        }


        try {
            await().atMost(2, SECONDS).untilTrue(eventReceived);
        } catch (org.awaitility.core.ConditionTimeoutException ignored) {
            fail("Client did not receive the event");
        }
    }

}
