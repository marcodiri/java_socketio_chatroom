package io.github.marcodiri.java_socketio_chatroom_server_mock;

import io.socket.emitter.Emitter.Listener;
import io.socket.socketio.server.SocketIoNamespace;
import io.socket.socketio.server.SocketIoSocket;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

public class ChatroomServerMock {

    private final ServerWrapper serverWrapper;

    private final SocketIoNamespace namespace;

    private SocketIoSocket socket;
    
    private ConcurrentMap<String, List<Listener>> handlersToAttach = new ConcurrentHashMap<>();

    private static final Logger LOGGER = LogManager.getLogger(ChatroomServerMock.class);

    public ChatroomServerMock() {
        this.serverWrapper = new ServerWrapper();
        this.namespace = serverWrapper.getSocketIoServer().namespace("/");
    }

    public void start() throws Exception {
        handleConnections();
        serverWrapper.startServer();
        LOGGER.info("Server started");
    }

    public void stop() throws Exception {
        serverWrapper.stopServer();
        LOGGER.info("Server stopped");
    }

    private void handleConnections() {
		handleNamespaceEvent("connection", args -> {
			socket = (SocketIoSocket) args[0];
			LOGGER.info(String.format("New incoming connection from %s", socket.getId()));
			for (String event : handlersToAttach.keySet()) {
				for (Listener fn : handlersToAttach.get(event)) {
					socket.on(event, fn);
				}
			}
			handlersToAttach.clear();
		});
    }
    
    public void handleNamespaceEvent(String event, Listener fn) {
        namespace.on(event, fn);
        LOGGER.info("Added listener to server namespace for event: {}", event);
    }

    public void handleEvent(String event, Listener fn) throws NullPointerException {
        if (socket != null) {
            socket.on(event, fn);
            LOGGER.info("Added listener to server for event: {}", event);
        } else {
        	if (handlersToAttach.containsKey(event)) {
        		handlersToAttach.get(event).add(fn);
        	} else {
        		handlersToAttach.put(event, new ArrayList<>(Arrays.asList(fn)));
        	}
        }
    }

	public void sendEvent(String event, JSONObject msg) throws NullPointerException {
        if (socket != null) {
            socket.send(event, msg);
            LOGGER.info("Sent {event: \"{}\", message: \"{}\"} to Socket {}", event, msg, socket.getId());
        } else {
            throw new NullPointerException("socket is null");
        }
    }
    
    public boolean isStarted() {
    	return serverWrapper.isStarted();
    }


    public SocketIoNamespace getNamespace() {
        return namespace;
    }

    public SocketIoSocket getSocket() {
        return socket;
    }

    public ConcurrentMap<String, List<Listener>> getHandlersToAttach() {
		return handlersToAttach;
	}

}
