package io.github.marcodiri.java_socketio_chatroom_server.model;

import java.sql.Timestamp;

public final class Message {
	private final Timestamp timestamp;
	private final String user;
	private final String message;
	
	public Message(Timestamp timestamp, String user, String message) {
		this.timestamp = new Timestamp(timestamp.getTime());
		this.user = user;
		this.message = message;
	}

	public Timestamp getTimestamp() {
		return new Timestamp(timestamp.getTime());
	}

	public String getUser() {
		return user;
	}

	public String getMessage() {
		return message;
	}
}
