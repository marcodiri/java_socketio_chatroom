package io.github.marcodiri.java_socketio_chatroom_server;

import io.socket.socketio.server.SocketIoNamespace;

public class ChatroomServer {

    private final ServerWrapper serverWrapper;
    private final SocketIoNamespace namespace;

    public ChatroomServer() {
        serverWrapper = new ServerWrapper();
        namespace = serverWrapper.getSocketIoServer().namespace("/");
    }
}
