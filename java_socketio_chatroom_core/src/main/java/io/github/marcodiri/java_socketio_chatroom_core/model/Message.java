package io.github.marcodiri.java_socketio_chatroom_core.model;

import java.sql.Timestamp;
import java.util.Objects;

import org.json.JSONObject;

public abstract class Message {
	protected final Timestamp timestamp;
	protected final String user;
	protected final String userMessage;
	
	public Message(Timestamp timestamp, String user, String message) {
		this.timestamp = new Timestamp(timestamp.getTime());
		this.user = user;
		this.userMessage = message;
	}

	public Timestamp getTimestamp() {
		return new Timestamp(timestamp.getTime());
	}

	public String getUser() {
		return user;
	}

	public String getUserMessage() {
		return userMessage;
	}
	
	public abstract JSONObject toJSON();
	
	@Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Message msg = (Message) o;
        return Objects.equals(timestamp, msg.timestamp) && Objects.equals(user, msg.user) && Objects.equals(userMessage, msg.userMessage);
    }
}
