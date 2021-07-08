package io.github.marcodiri.java_socketio_chatroom_server_mock;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;
import static org.awaitility.Awaitility.await;
import java.net.URI;
import java.util.concurrent.atomic.AtomicBoolean;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import io.socket.client.IO;
import io.socket.client.Socket;

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
    public void testHandleConnections() {
    	assertThat(serverMock.getNamespace().hasListeners("connection")).isTrue();
    	
    	clientSocket.connect();
		try {
            await().atMost(2, SECONDS).until(() -> clientSocket.connected());
        } catch (org.awaitility.core.ConditionTimeoutException ignored) {
        	fail("Client could not connect to server");
        }
		
		assertThat(serverMock.getSocket()).isNotNull();
		assertThat(serverMock.getSocket().getId()).isEqualTo(clientSocket.id());
		assertThat(serverMock.getSocket().hasListeners("join")).isTrue();
		assertThat(serverMock.getSocket().hasListeners("leave")).isTrue();
    }
    
    @Test
    public void testHandleClientJoin() {
    	clientSocket.connect();
		try {
            await().atMost(2, SECONDS).until(() -> clientSocket.connected());
        } catch (org.awaitility.core.ConditionTimeoutException ignored) {
        	fail("Client could not connect to server");
        }
		
		clientSocket.emit("join");

		try {
            await().atMost(2, SECONDS).untilTrue((serverMock.socketIsInRoom()));
        } catch (org.awaitility.core.ConditionTimeoutException ignored) {
        	fail("Client could not join the room");
        }
    }
    
    @Test
    public void testHandleClientLeave() {
    	clientSocket.connect();
    	try {
    		await().atMost(2, SECONDS).until(() -> clientSocket.connected());
    	} catch (org.awaitility.core.ConditionTimeoutException ignored) {
    		fail("Client could not connect to server");
    	}
    	
    	serverMock.socketIsInRoom().set(true);
    	clientSocket.emit("leave");
    	
    	try {
    		await().atMost(2, SECONDS).untilFalse((serverMock.socketIsInRoom()));
    	} catch (org.awaitility.core.ConditionTimeoutException ignored) {
    		fail("Client could not leave the room");
    	}
    }
    
    @Test
	public void testHandleEventWhenClientNotConnected() {
    	assertThat(serverMock.getSocket()).isNull();
    	
    	assertThatThrownBy(() -> serverMock.handleEvent("event", arg -> {}))
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
			serverMock.handleEvent("event", arg -> {});
		} catch (NullPointerException e) {
			fail("Socket is null");
		}
		
		assertThat(serverMock.getSocket().hasListeners("event"));
	}
	
	@Test
	public void testSendEventWhenClientNotConnected() {
		assertThat(serverMock.getSocket()).isNull();
		
		assertThatThrownBy(() -> serverMock.sendEvent("event", new JSONObject()))
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
