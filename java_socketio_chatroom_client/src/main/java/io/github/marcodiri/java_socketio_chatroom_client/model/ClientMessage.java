package io.github.marcodiri.java_socketio_chatroom_client.model;

import io.github.marcodiri.java_socketio_chatroom_core.model.Message;
import org.json.JSONObject;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

public final class ClientMessage extends Message {

	public ClientMessage(Timestamp timestamp, String user, String message) {
		super(timestamp, user, message);
	}

	public ClientMessage(JSONObject jsonMsg) {
		super(new Timestamp(jsonMsg.getLong("timestamp")), jsonMsg.getString("user"), jsonMsg.getString("message"));
	}

	@Override
	public String getFormattedMessage() {
		DateFormat dateFormat = new SimpleDateFormat("HH:mm");
		return dateFormat.format(this.getTimestamp()) + " " + this.getUser() + ": " + this.getUserMessage();
	}

}
