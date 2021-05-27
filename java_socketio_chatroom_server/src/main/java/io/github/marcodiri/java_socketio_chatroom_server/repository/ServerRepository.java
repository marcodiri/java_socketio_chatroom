package io.github.marcodiri.java_socketio_chatroom_server.repository;

import java.util.List;

import io.github.marcodiri.java_socketio_chatroom_server.model.Message;

public interface ServerRepository {
	public List<Message> findAll();

	public void save(Message message);
}
