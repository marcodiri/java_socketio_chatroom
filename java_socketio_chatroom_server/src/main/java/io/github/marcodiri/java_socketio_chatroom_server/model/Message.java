package io.github.marcodiri.java_socketio_chatroom_server.model;

import java.sql.Timestamp;
import java.util.Objects;

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
	
	@Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("Message{");
        sb.append("timestamp=").append(timestamp);
        sb.append(", user=").append(user);
        sb.append(", message=").append(message);
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
        return Objects.equals(timestamp, msg.timestamp) && Objects.equals(user, msg.user) && Objects.equals(message, msg.message);
    }

    @Override
    public int hashCode() {
        return Objects.hash(timestamp, user, message);
    }
}
