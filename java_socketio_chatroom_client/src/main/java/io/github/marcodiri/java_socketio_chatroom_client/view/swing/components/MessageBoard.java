package io.github.marcodiri.java_socketio_chatroom_client.view.swing.components;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JTextPane;

import io.github.marcodiri.java_socketio_chatroom_client.model.ClientMessage;
import io.github.marcodiri.java_socketio_chatroom_core.model.Message;

@SuppressWarnings("serial")
public class MessageBoard extends JTextPane {

	private final transient List<Message> history = new ArrayList<>();

	public List<Message> getHistory() {
		return history;
	}

	public void newMessageNotify(Message msg) {
		history.add(msg);
		String formattedMsg = msg.getFormattedMessage();
		if (!getText().isEmpty()) {
			formattedMsg = System.lineSeparator() + formattedMsg;
		}
		setText(getText() + formattedMsg);
	}

	public void clearBoard() {
		setText("");
	}

}
