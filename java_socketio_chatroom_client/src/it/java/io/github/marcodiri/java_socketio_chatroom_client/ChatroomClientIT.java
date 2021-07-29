package io.github.marcodiri.java_socketio_chatroom_client;

import io.github.marcodiri.java_socketio_chatroom_client.model.ClientMessage;
import io.github.marcodiri.java_socketio_chatroom_client.view.ClientView;
import io.github.marcodiri.java_socketio_chatroom_core.model.Message;
import io.github.marcodiri.java_socketio_chatroom_server.ChatroomServer;
import io.github.marcodiri.java_socketio_chatroom_server.model.ServerMessage;
import io.github.marcodiri.java_socketio_chatroom_server.repository.ServerRepository;
import io.socket.client.IO;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.net.SocketException;
import java.net.URI;
import java.sql.Timestamp;
import java.util.Collections;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.fail;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ChatroomClientIT {

    @Mock
    private ServerRepository mongoRepository;
    @InjectMocks
    private ChatroomServer chatroomServer;
    private AutoCloseable closeable;

    @Mock
    private ClientView view;
    private ChatroomClient chatroomClient;

    @Before
    public void setup() {
        closeable = MockitoAnnotations.openMocks(this);

        try {
            chatroomServer.start();
        } catch (Exception ignored) {
            fail("ServerWrapper startup failed");
        }

        chatroomClient = new ChatroomClient(URI.create("http://localhost:3000"), IO.Options.builder().build(), view);
    }

    @After
    public void closeConnections() throws Exception {
        chatroomClient.disconnect();
        chatroomServer.stop();
        closeable.close();
    }

    @Test
    public void testRetrieveMsgInRepositoryOnConnection() {
        Message serverMessage = new ServerMessage(new Timestamp(0), "user", "message");
        when(mongoRepository.findAll()).thenReturn(Collections.singletonList(serverMessage));

        chatroomClient.connect("user");

        Message clientMessage = new ClientMessage(serverMessage.toJSON());
        try {
            await().atMost(2, SECONDS).untilAsserted(() -> verify(view).addMessage(clientMessage));
        } catch (org.awaitility.core.ConditionTimeoutException ignored) {
            fail("Expected " + clientMessage);
        }
    }

    @Test
    public void testSentMessageAreSavedInRepository() {
        chatroomClient.connect("user");
        try {
            await().atMost(2, SECONDS).until(() -> chatroomClient.isConnected());
        } catch (org.awaitility.core.ConditionTimeoutException ignored) {
            fail("Client cannot connect to server");
        }

        ClientMessage clientMessage = new ClientMessage(new Timestamp(0), "user", "message");
        try {
            chatroomClient.sendMessage(clientMessage);
        } catch (SocketException e) {
            fail(e.getMessage());
        }

        Message serverMessage = new ServerMessage(clientMessage.getTimestamp(), clientMessage.getUser(), clientMessage.getUserMessage());

        try {
            await().atMost(2, SECONDS).untilAsserted(() -> verify(mongoRepository).save(serverMessage));
        } catch (org.awaitility.core.ConditionTimeoutException ignored) {
            fail("Expected " + serverMessage);
        }
    }

}
