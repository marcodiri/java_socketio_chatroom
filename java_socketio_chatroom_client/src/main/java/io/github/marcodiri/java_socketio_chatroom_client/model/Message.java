package io.github.marcodiri.java_socketio_chatroom_client.model;

import org.json.JSONObject;

import java.sql.Timestamp;
import java.util.Objects;

public final class Message {
	private final Timestamp timestamp;
	private final String user;
	private final String userMessage;
	
	public Message(Timestamp timestamp, String user, String message) {
		this.timestamp = new Timestamp(timestamp.getTime());
		this.user = user;
		this.userMessage = message;
	}

	public Message(JSONObject jsonMsg) {
        this.timestamp = new Timestamp(jsonMsg.getLong("timestamp"));
        this.user = jsonMsg.getString("user");
        this.userMessage = jsonMsg.getString("message");
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
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Message msg = (Message) o;
        return Objects.equals(timestamp, msg.timestamp) && Objects.equals(user, msg.user) && Objects.equals(userMessage, msg.userMessage);
    }

    @Override
    public int hashCode() {
        return Objects.hash(timestamp, user, userMessage);
    }
}
