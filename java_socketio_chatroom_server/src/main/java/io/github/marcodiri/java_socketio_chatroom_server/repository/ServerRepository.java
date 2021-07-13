package io.github.marcodiri.java_socketio_chatroom_server.repository;

import java.util.List;

import io.github.marcodiri.java_socketio_chatroom_server.model.ServerMessage;

public interface ServerRepository {
	public List<ServerMessage> findAll();

	public void save(ServerMessage message);
}
