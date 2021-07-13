package io.github.marcodiri.java_socketio_chatroom_client.model;

import org.json.JSONObject;

import io.github.marcodiri.java_socketio_chatroom_core.model.Message;

import java.sql.Timestamp;

public final class ClientMessage extends Message {
	
	public ClientMessage(Timestamp timestamp, String user, String message) {
        super(timestamp, user, message);
	}

	public ClientMessage(JSONObject jsonMsg) {
        super(new Timestamp(jsonMsg.getLong("timestamp")), jsonMsg.getString("user"), jsonMsg.getString("message"));
    }

}
