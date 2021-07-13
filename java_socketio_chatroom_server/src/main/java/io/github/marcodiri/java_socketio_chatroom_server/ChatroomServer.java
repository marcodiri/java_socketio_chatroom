package io.github.marcodiri.java_socketio_chatroom_server;

import io.github.marcodiri.java_socketio_chatroom_server.model.ServerMessage;
import io.github.marcodiri.java_socketio_chatroom_server.repository.ServerRepository;

import io.socket.socketio.server.SocketIoNamespace;
import io.socket.socketio.server.SocketIoSocket;
import org.json.JSONObject;

import java.sql.Timestamp;
import java.util.List;

public class ChatroomServer {

    private static final String CHATROOM_NAME = "Chatroom";

    private final ServerWrapper serverWrapper;

    private final SocketIoNamespace namespace;
    private final ServerRepository repository;

    public ChatroomServer(ServerRepository repository) {
        this.serverWrapper = new ServerWrapper();
        this.namespace = serverWrapper.getSocketIoServer().namespace("/");
        this.repository = repository;
    }

    public void start() throws Exception {
        handleConnections();
        serverWrapper.startServer();
    }

    public void stop() throws Exception {
        serverWrapper.stopServer();
    }

    private void handleConnections() {
        namespace.on("connection", args -> {
            final SocketIoSocket socket = (SocketIoSocket) args[0];
            handleClientJoin(socket);
            handleClientMessage(socket, namespace);
            handleClientLeave(socket);
        });
    }

    private void handleClientJoin(SocketIoSocket socket) {
        socket.on("join", arg -> {
            socket.joinRoom(CHATROOM_NAME);
            List<ServerMessage> history = repository.findAll();
            for (ServerMessage message : history) {
                socket.send("msg", message.toJSON());
            }
        });
    }

    private void handleClientMessage(SocketIoSocket socket, SocketIoNamespace namespace) {
        socket.on("msg", arg -> {
            namespace.broadcast(CHATROOM_NAME, "msg", arg[0]);
            JSONObject jsonMsg = (JSONObject) arg[0];
            ServerMessage incomingMessage = new ServerMessage(new Timestamp(jsonMsg.getLong("timestamp")), jsonMsg.getString("user"), jsonMsg.getString("message"));
            repository.save(incomingMessage);
        });
    }

    private void handleClientLeave(SocketIoSocket socket) {
        socket.on("leave", arg -> socket.leaveRoom(CHATROOM_NAME));
    }

    SocketIoNamespace getNamespace() {
        return namespace;
    }

}
