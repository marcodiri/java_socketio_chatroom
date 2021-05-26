package io.github.marcodiri.java_socketio_chatroom_server;

import static org.assertj.core.api.Assertions.*;

import org.junit.After;
import org.junit.Test;
import org.junit.Before;

import static org.junit.Assert.*;

public class ServerWrapperTest {
    private ServerWrapper serverWrapper;

    @Before
    public void setup() {
        serverWrapper = new ServerWrapper();
    }


    @Test
    public void testStartServer() {
        try {
            serverWrapper.startServer();
        } catch (Exception ignored) {

        }
        assertThat(serverWrapper.getStatus()).isEqualTo("STARTED");
    }

}
