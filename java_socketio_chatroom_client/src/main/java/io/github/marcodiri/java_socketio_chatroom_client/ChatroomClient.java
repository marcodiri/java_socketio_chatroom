package io.github.marcodiri.java_socketio_chatroom_client;

import java.net.SocketException;
import java.net.URI;
import java.util.concurrent.atomic.AtomicBoolean;

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

	ChatroomClientHandlers chatroomClientHandlers;
	private final ClientView view;

	private static final Logger LOGGER = LogManager.getLogger(ChatroomClient.class);

	final AtomicBoolean connected = new AtomicBoolean(false);

	String username = null;

	public ChatroomClient(URI uri, Options options, ClientView view) {
		this.socket = IO.socket(uri, options);
		this.chatroomClientHandlers = new ChatroomClientHandlers();
		this.view = view;
		socket.on("connected", objects -> chatroomClientHandlers.connectedHandler());
		socket.on(Socket.EVENT_DISCONNECT, objects -> chatroomClientHandlers.disconnectedHandler());
	}

	ChatroomClient(URI uri, Options options, ChatroomClientHandlers chatroomClientHandlers, ClientView view) {
		this(uri, options, view);
		this.chatroomClientHandlers = chatroomClientHandlers;
	}

	ChatroomClientHandlers getChatroomClientHandlers() {
		return chatroomClientHandlers;
	}

	public Socket getSocket() {
		return socket;
	}

	public boolean isConnected() {
		return connected.get();
	}

	public void connect(String username) {
		this.username = username;
		socket.connect();
		LOGGER.info("Socket attempting to connect to Server");
	}

	public void disconnect() {
		socket.disconnect();
		LOGGER.info("Socket attempting to disconnect from Server");
		this.username = null;
	}

	public void sendMessage(ClientMessage msg) throws SocketException {
		if (isConnected()) {
			socket.emit("msg", msg.toJSON());
			LOGGER.info("Message sent to Server");
			LOGGER.debug(() -> String.format("Sent {event: \"msg\", message: \"%s\"} to Server", msg.toJSON()));
		} else {
			throw new SocketException("Unable to send message when not connected to server");
		}
	}

	class ChatroomClientHandlers {

		void connectedHandler() throws NullPointerException {
			LOGGER.debug(() -> "Received {event: \"connected\"} from Server");
			if (username != null) {
				LOGGER.info("Socket successfully connected to Server");
				socket.on("joined", arg -> handleJoin(((JSONObject) arg[0]).getString("roomName")));
				socket.emit("join", username);
				LOGGER.info("Socket attempting to join the room");
				LOGGER.debug(() -> String.format("Sent {event: \"join\", message: \"%s\"} to Server", username));
				socket.on("msg", arg -> handleMessage(new ClientMessage((JSONObject) arg[0])));
				socket.on("error", arg -> handleError(((JSONObject) arg[0]).getString("message")));
				connected.set(true);
			} else {
				throw new NullPointerException("Username is null");
			}
		}

		void disconnectedHandler() {
			LOGGER.info("Socket successfully disconnected from Server");
			socket.off();
			connected.set(false);
		}

		void handleMessage(ClientMessage message) {
			LOGGER.info("Message received from Server");
			LOGGER.debug(() -> String.format("Received {event: \"msg\", message: \"%s\"} from Server", message));
			view.addMessage(message);
		}

		void handleJoin(String roomName) {
			LOGGER.info("Socket successfully joined the room");
			LOGGER.debug(() -> String.format("Received {event: \"joined\", message: \"%s\"} from Server", roomName));
			view.roomJoined(roomName);
		}

		void handleError(String errorMessage) {
			LOGGER.info("Error received from Server");
			LOGGER.debug(() -> String.format("Received {event: \"error\", message: \"%s\"} from Server", errorMessage));
			view.showError(errorMessage);
		}
	}

}