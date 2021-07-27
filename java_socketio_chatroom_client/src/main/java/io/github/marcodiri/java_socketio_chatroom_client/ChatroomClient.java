package io.github.marcodiri.java_socketio_chatroom_client;

import java.net.URI;

import org.json.JSONObject;

import io.github.marcodiri.java_socketio_chatroom_client.model.ClientMessage;
import io.github.marcodiri.java_socketio_chatroom_client.view.ClientView;
import io.socket.client.IO;
import io.socket.client.IO.Options;
import io.socket.client.Socket;

public class ChatroomClient {

    private final Socket socket;

    private final ClientView view;

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
            handleJoin();
            socket.emit("join", username);
            handleMessage();
            handleError();
        });
        socket.connect();
    }

    public void disconnect() {
        socket.on(Socket.EVENT_DISCONNECT, objects -> socket.off());
        socket.disconnect();
    }

    public void sendMessage(ClientMessage msg) throws RuntimeException {
        if (isConnected()) {
            socket.emit("msg", msg.toJSON());
        } else {
            throw new RuntimeException("Unable to send message when not connected to server");
        }
    }

    void handleMessage() {
        socket.on("msg", arg -> view.addMessage(new ClientMessage((JSONObject) arg[0])));
    }

    void handleJoin() {
        socket.on("joined", args -> view.roomJoined(((JSONObject) args[0]).getString("roomName")));
    }

    void handleError() {
        socket.on("error", args -> view.showError(((JSONObject) args[0]).getString("message")));
    }
}
