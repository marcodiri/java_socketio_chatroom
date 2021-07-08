package io.github.marcodiri.java_socketio_chatroom_server_mock;

import io.socket.emitter.Emitter.Listener;
import io.socket.socketio.server.SocketIoNamespace;
import io.socket.socketio.server.SocketIoSocket;
import org.json.JSONObject;

import java.util.concurrent.atomic.AtomicBoolean;

public class ChatroomServerMock {

	private final ServerWrapper serverWrapper;

	private final SocketIoNamespace namespace;

	private SocketIoSocket socket;

	public JSONObject receivedMsg;

	private final AtomicBoolean socketIsInRoom = new AtomicBoolean(false);

	public ChatroomServerMock() {
		this.serverWrapper = new ServerWrapper();
		this.namespace = serverWrapper.getSocketIoServer().namespace("/");
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
			socket = (SocketIoSocket) args[0];
			handleClientJoin();
			handleClientLeave();
		});
	}

	private void handleClientJoin() throws NullPointerException {
		handleEvent("join", arg -> socketIsInRoom.set(true));
	}

	private void handleClientLeave() throws NullPointerException {
		handleEvent("leave", arg -> {
			socketIsInRoom.set(false);
		});
	}

	public void handleEvent(String event, Listener fn) throws NullPointerException {
		if (socket != null) {
			socket.on(event, fn);
		} else {
			throw new NullPointerException("socket is null");
		}
	}

	public void sendEvent(String event, String msg) throws NullPointerException {
		if (socket != null) {
			socket.send(event, msg);
		} else {
			throw new NullPointerException("socket is null");
		}
	}

	public SocketIoNamespace getNamespace() {
		return namespace;
	}

	public SocketIoSocket getSocket() {
		return socket;
	}

	public AtomicBoolean socketIsInRoom() {
		return socketIsInRoom;
	}

}
