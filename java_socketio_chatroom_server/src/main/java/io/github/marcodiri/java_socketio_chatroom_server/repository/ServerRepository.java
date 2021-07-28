package io.github.marcodiri.java_socketio_chatroom_server.repository;

import io.github.marcodiri.java_socketio_chatroom_core.model.Message;

import java.util.List;

public interface ServerRepository {
	public List<Message> findAll();

	public void save(Message message);
}
