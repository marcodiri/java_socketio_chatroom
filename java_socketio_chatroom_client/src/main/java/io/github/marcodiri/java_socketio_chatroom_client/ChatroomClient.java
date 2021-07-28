package io.github.marcodiri.java_socketio_chatroom_client;

import java.net.URI;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import io.github.marcodiri.java_socketio_chatroom_client.model.ClientMessage;
import io.github.marcodiri.java_socketio_chatroom_client.view.ClientView;
import io.socket.client.IO;
import io.socket.client.IO.Options;
import io.socket.client.Socket;

public class ChatroomClient {

    private final Socket socket;

    private final ClientView view;
    
    private static final Logger LOGGER = LogManager.getLogger(ChatroomClient.class);

    public ChatroomClient(URI uri, Options options, ClientView view) {
        this.socket = IO.socket(uri, options);
        this.view = view;
    }

    public Socket getSocket() {
        return socket;
    }

    public boolean isConnected() {
        return socket.connected();
    }

    public void connect(String username) {
        socket.on(Socket.EVENT_CONNECT, objects -> {
        	LOGGER.info(String.format("Socket succesfully connected to Server"));
            handleJoin();
            socket.emit("join", username);
        	LOGGER.info(String.format("Socket attempting to join the room"));
            LOGGER.debug(() -> String.format("Sent {event: \"join\", message: \"%s\"} to Server", username));
            handleMessage();
            handleError();
        });
        socket.connect();
    	LOGGER.info(String.format("Socket attempting to connect to Server"));
    }

    public void disconnect() {
        socket.on(Socket.EVENT_DISCONNECT, objects -> {
        	LOGGER.info(String.format("Socket succesfully disconnected from Server"));
        	socket.off();
        });
        socket.disconnect();
    	LOGGER.info(String.format("Socket attempting to disconnect from Server"));
    }

    public void sendMessage(ClientMessage msg) throws RuntimeException {
        if (isConnected()) {
            socket.emit("msg", msg.toJSON());
        	LOGGER.info(String.format("Message sent to Server"));
            LOGGER.debug(() -> String.format("Sent {event: \"msg\", message: \"%s\"} to Server", msg.toJSON()));
        } else {
            throw new RuntimeException("Unable to send message when not connected to server");
        }
    }

    void handleMessage() {
        socket.on("msg", arg -> {
        	LOGGER.info(String.format("Message received from Server"));
            LOGGER.debug(() -> String.format("Received {event: \"msg\", message: \"%s\"} from Server", arg[0]));
        	view.addMessage(new ClientMessage((JSONObject) arg[0]));
        });
    }

    void handleJoin() {
        socket.on("joined", args -> {
        	LOGGER.info(String.format("Socket succesfully joined the room"));
            LOGGER.debug(() -> String.format("Received {event: \"joined\", message: \"%s\"} from Server", args[0]));
        	view.roomJoined(((JSONObject) args[0]).getString("roomName"));
        });
    }

    void handleError() {
        socket.on("error", args -> {
        	LOGGER.info(String.format("Error received from Server"));
            LOGGER.debug(() -> String.format("Received {event: \"error\", message: \"%s\"} from Server", args[0]));
        	view.showError(((JSONObject) args[0]).getString("message"));
        });
    }
}
