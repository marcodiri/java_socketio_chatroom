package io.github.marcodiri.java_socketio_chatroom_server.model;

import io.github.marcodiri.java_socketio_chatroom_core.model.Message;

import java.sql.Timestamp;

public final class ServerMessage extends Message {
	
	public ServerMessage(Timestamp timestamp, String user, String message) {
        super(timestamp, user, message);
	}

	@Override
	public String getFormattedMessage() {
		return this.getTimestamp().toString() + " " + this.getUser() + ": " + this.getUserMessage();
	}

}
