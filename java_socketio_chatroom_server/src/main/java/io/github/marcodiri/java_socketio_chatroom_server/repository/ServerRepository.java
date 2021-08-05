package io.github.marcodiri.java_socketio_chatroom_server.repository;

import io.github.marcodiri.java_socketio_chatroom_core.model.Message;

import java.util.List;

public interface ServerRepository {
	List<Message> findAll();

	void save(Message message);
}
