package io.github.marcodiri.java_socketio_chatroom_server;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.await;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.socketio.server.SocketIoNamespace;
import org.junit.After;
import org.junit.Test;
import org.junit.Before;

import java.net.URI;
import java.util.concurrent.atomic.AtomicBoolean;


public class ServerWrapperTest {
    private ServerWrapper serverWrapper;

    @Before
    public void setup() {
        serverWrapper = new ServerWrapper();
    }

    @After
    public void alwaysStopServer() throws Exception {
        serverWrapper.stopServer();
    }


    @Test
    public void testStartServer() {
        try {
            serverWrapper.startServer();
        } catch (Exception ignored) {
            fail("Jetty server startup failed");
        }
        assertThat(serverWrapper.getStatus()).isEqualTo("STARTED");
    }

    @Test
    public void testStopServerWhenServerIsRunning() {
        try {
            serverWrapper.startServer();
            serverWrapper.stopServer();
        } catch (Exception ignored) {
            fail("Jetty server startup failed");
        }
        assertThat(serverWrapper.getStatus()).isEqualTo("STOPPED");
    }

    @Test
    public void testServerNamespaceAcceptsListeners() {
        try {
            serverWrapper.startServer();
        } catch (Exception ignored) {
            fail("Jetty server startup failed");
        }
        SocketIoNamespace namespace = serverWrapper.getSocketIoServer().namespace("/");
        namespace.on("test", args -> {
        });

        assertThat(namespace.hasListeners("test")).isTrue();
    }

    @Test
    public void testServerCanHandleRequest() {
        AtomicBoolean connected = new AtomicBoolean(false);
        try {
            serverWrapper.startServer();
        } catch (Exception ignored) {
            fail("Jetty server startup failed");
        }
        SocketIoNamespace namespace = serverWrapper.getSocketIoServer().namespace("/");
        namespace.on("connection", args -> connected.set(true));

        Socket socket = IO.socket(URI.create("http://localhost:3000"), IO.Options.builder().build());

        socket.connect();
        try {
            await().atMost(2, SECONDS).untilTrue(connected);
        } catch (org.awaitility.core.ConditionTimeoutException ignored) {
            fail("Expected true but got " + connected.get());
        }
        socket.disconnect();

    }


}
