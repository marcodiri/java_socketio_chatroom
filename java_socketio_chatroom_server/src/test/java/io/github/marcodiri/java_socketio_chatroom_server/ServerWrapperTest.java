package io.github.marcodiri.java_socketio_chatroom_server;

import static org.assertj.core.api.Assertions.*;

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
    public void alwaysStopServer() {
        try {
            serverWrapper.stopServer();
        } catch (Exception ignored) {

        }
    }


    @Test
    public void testStartServer() {
        try {
            serverWrapper.startServer();
        } catch (Exception ignored) {

        }
        assertThat(serverWrapper.getStatus()).isEqualTo("STARTED");
    }

    @Test
    public void testStopServerWhenServerIsRunning() {
        try {
            serverWrapper.startServer();
            serverWrapper.stopServer();
        } catch (Exception ignored) {

        }
        assertThat(serverWrapper.getStatus()).isEqualTo("STOPPED");
    }

    @Test
    public void testServerNamespaceAcceptsListeners() {
        try {
            serverWrapper.startServer();
        } catch (Exception ignored) {

        }
        SocketIoNamespace namespace = serverWrapper.getSocketIoServer().namespace("/");
        namespace.on("test", args -> {
        });

        assertThat(namespace.hasListeners("test")).isTrue();
    }

    @Test
    public void testServerCanHandleRequest() throws InterruptedException {
        AtomicBoolean connected = new AtomicBoolean(false);
        try {
            serverWrapper.startServer();
        } catch (Exception ignored) {

        }
        SocketIoNamespace namespace = serverWrapper.getSocketIoServer().namespace("/");
        namespace.on("connection", args -> {
            connected.set(true);
        });

        Socket socket = IO.socket(URI.create("http://localhost:3000"), IO.Options.builder().build());

        socket.connect();
        Thread.sleep(1000);
        socket.disconnect();

        assertThat(connected.get()).isTrue();
    }


}
