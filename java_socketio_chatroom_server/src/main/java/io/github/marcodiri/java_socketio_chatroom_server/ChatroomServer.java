package io.github.marcodiri.java_socketio_chatroom_server;

import io.github.marcodiri.java_socketio_chatroom_core.model.Message;
import io.github.marcodiri.java_socketio_chatroom_server.model.ServerMessage;
import io.github.marcodiri.java_socketio_chatroom_server.repository.ServerRepository;
import io.socket.socketio.server.SocketIoNamespace;
import io.socket.socketio.server.SocketIoSocket;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatroomServer {

    private static final String CHATROOM_NAME = "Chatroom";

    private final ServerWrapper serverWrapper;

    private final SocketIoNamespace namespace;
    private final ServerRepository repository;

    private final HashMap<String, String> usernameList;

	private static final Logger LOGGER = LogManager.getLogger(ChatroomServer.class);

    public ChatroomServer(ServerRepository repository) {
        this.serverWrapper = new ServerWrapper();
        this.namespace = serverWrapper.getSocketIoServer().namespace("/");
        this.repository = repository;
        this.usernameList = new HashMap<>();
    }

    public void start() throws Exception {
        handleConnections();
        serverWrapper.startServer();
        LOGGER.info("Server started");
    }

    public void stop() throws Exception {
        serverWrapper.stopServer();
        usernameList.clear();
        LOGGER.info("Server stopped");
    }

    private void handleConnections() {
        namespace.on("connection", args -> {
            final SocketIoSocket socket = (SocketIoSocket) args[0];
            LOGGER.info(String.format("New incoming connection from %s", socket.getId()));
            handleClientJoin(socket);
            handleClientMessage(socket, namespace);
            handleClientLeave(socket);
        });
    }

    private void handleClientJoin(SocketIoSocket socket) {
        socket.on("join", arg -> {
            LOGGER.info(String.format("Socket %s is trying to join the room", socket.getId()));
            if (!socketIsInRoom(socket)) {
                if (usernameList.containsValue(arg[0].toString())) {
                    sendError(socket, "Username is already taken");
                } else {
                    socket.joinRoom(CHATROOM_NAME);
                    usernameList.put(socket.getId(), (String) arg[0]);
                    socket.send("joined", new JSONObject("{roomName: " + CHATROOM_NAME + "}"));
                    LOGGER.info(String.format("Socket %s joined the room", socket.getId()));
                    LOGGER.debug(() -> String.format("Sent {event: \"joined\", message: \"{roomName: %s}\"} to Socket %s", CHATROOM_NAME, socket.getId()));
                    List<Message> history = repository.findAll();
                    for (Message message : history) {
                        socket.send("msg", message.toJSON());
                        LOGGER.debug(() -> String.format("Sent {event: \"msg\", message: \"%s\"} to Socket %s", message.toJSON(), socket.getId()));
                    }
                }
            }
        });
    }

    private void handleClientMessage(SocketIoSocket socket, SocketIoNamespace namespace) {
        socket.on("msg", arg -> {
            LOGGER.info(String.format("Message received from Socket %s", socket.getId()));
            LOGGER.debug(() -> String.format("Received {event: \"msg\", message: \"%s\"} to Socket %s", arg[0], socket.getId()));
            if (socketIsInRoom(socket)) {
                namespace.broadcast(CHATROOM_NAME, "msg", arg[0]);
                LOGGER.info(String.format("Message broadcasted to clients"));
                JSONObject jsonMsg = (JSONObject) arg[0];
                Message incomingMessage = new ServerMessage(new Timestamp(jsonMsg.getLong("timestamp")), jsonMsg.getString("user"), jsonMsg.getString("message"));
                repository.save(incomingMessage);
            }
        });
    }

    private void handleClientLeave(SocketIoSocket socket) {
        socket.on("disconnect", arg -> {
            LOGGER.debug(() -> String.format("Received {event: \"disconnect\"} from Socket %s", socket.getId()));
            socket.leaveRoom(CHATROOM_NAME);
            usernameList.remove(socket.getId());
            LOGGER.info(String.format("Socket %s removed from room", socket.getId()));
        });
    }

    SocketIoNamespace getNamespace() {
        return namespace;
    }

    public Map<String, String> getUsernameList() {
        return usernameList;
    }

    private boolean socketIsInRoom(SocketIoSocket socket) {
        return Arrays.asList(namespace.getAdapter().listClientRooms(socket)).contains(CHATROOM_NAME);
    }

    private void sendError(SocketIoSocket socket, String errorMessage) {
        socket.send("error", new JSONObject("{message: " + errorMessage + "}"));
        LOGGER.info(String.format("Sent error [%s] to Socket %s", errorMessage, socket.getId()));
        LOGGER.debug(() -> String.format("Sent {event: \"error\", message: \"%s\"} to Socket %s", errorMessage, socket.getId()));
    }

}
