package io.github.marcodiri.java_socketio_chatroom_core.model;

import org.json.JSONObject;

import java.sql.Timestamp;
import java.util.Objects;

public abstract class Message {
	protected final Timestamp timestamp;
	protected final String user;
	protected final String userMessage;

	protected Message(Timestamp timestamp, String user, String message) {
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

	public abstract String getFormattedMessage();

	public JSONObject toJSON() {
		JSONObject obj = new JSONObject();

		obj.put("timestamp", timestamp.getTime());
		obj.put("user", user);
		obj.put("message", userMessage);

		return obj;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder("Message{");
		sb.append("timestamp=").append(timestamp);
		sb.append(", user=").append(user);
		sb.append(", message=").append(userMessage);
		sb.append('}');
		return sb.toString();
	}

	@Override
	public int hashCode() {
		return Objects.hash(timestamp, user, userMessage);
	}

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
