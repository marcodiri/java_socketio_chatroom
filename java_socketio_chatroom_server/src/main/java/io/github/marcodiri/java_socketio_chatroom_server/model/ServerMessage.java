package io.github.marcodiri.java_socketio_chatroom_server.model;

import org.json.JSONObject;

import io.github.marcodiri.java_socketio_chatroom_core.model.Message;

import java.sql.Timestamp;
import java.util.Objects;

public final class ServerMessage extends Message {
	
	public ServerMessage(Timestamp timestamp, String user, String message) {
        super(timestamp, user, message);
	}

	@Override
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
}
