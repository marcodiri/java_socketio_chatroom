package io.github.marcodiri.java_socketio_chatroom_server;

import io.github.marcodiri.java_socketio_chatroom_core.model.Message;
import io.github.marcodiri.java_socketio_chatroom_server.model.ServerMessage;
import io.github.marcodiri.java_socketio_chatroom_server.repository.ServerRepository;

import io.socket.socketio.server.SocketIoNamespace;
import io.socket.socketio.server.SocketIoSocket;
import org.json.JSONObject;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class ChatroomServer {

    private static final String CHATROOM_NAME = "Chatroom";

    private final ServerWrapper serverWrapper;

    private final SocketIoNamespace namespace;
    private final ServerRepository repository;

    private final HashMap<String, String> usernameList;

    public ChatroomServer(ServerRepository repository) {
        this.serverWrapper = new ServerWrapper();
        this.namespace = serverWrapper.getSocketIoServer().namespace("/");
        this.repository = repository;
        this.usernameList = new HashMap<>();
    }

    public void start() throws Exception {
        handleConnections();
        serverWrapper.startServer();
    }

    public void stop() throws Exception {
        serverWrapper.stopServer();
        usernameList.clear();
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
            if (!socketIsInRoom(socket)) {
                if (usernameList.containsValue(arg[0].toString())) {
                    sendError(socket, "Username is already taken");
                } else {
                    socket.joinRoom(CHATROOM_NAME);
                    usernameList.put(socket.getId(), (String) arg[0]);
                    socket.send("joined", new JSONObject("{roomName: " + CHATROOM_NAME + "}"));
                    List<Message> history = repository.findAll();
                    for (Message message : history) {
                        socket.send("msg", message.toJSON());
                    }
                }
            }
        });
    }

    private void handleClientMessage(SocketIoSocket socket, SocketIoNamespace namespace) {
        socket.on("msg", arg -> {
            if (socketIsInRoom(socket)) {
                namespace.broadcast(CHATROOM_NAME, "msg", arg[0]);
                JSONObject jsonMsg = (JSONObject) arg[0];
                Message incomingMessage = new ServerMessage(new Timestamp(jsonMsg.getLong("timestamp")), jsonMsg.getString("user"), jsonMsg.getString("message"));
                repository.save(incomingMessage);
            }
        });
    }

    private void handleClientLeave(SocketIoSocket socket) {
        socket.on("disconnect", arg -> {
            socket.leaveRoom(CHATROOM_NAME);
            usernameList.remove(socket.getId());
        });
    }

    SocketIoNamespace getNamespace() {
        return namespace;
    }

    public HashMap<String, String> getUsernameList() {
        return usernameList;
    }

    private boolean socketIsInRoom(SocketIoSocket socket) {
        return Arrays.asList(namespace.getAdapter().listClientRooms(socket)).contains(CHATROOM_NAME);
    }

    private void sendError(SocketIoSocket socket, String errorMessage) {
        socket.send("error", new JSONObject("{message: " + errorMessage + "}"));
    }

}
