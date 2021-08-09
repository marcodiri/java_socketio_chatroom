package io.github.marcodiri.java_socketio_chatroom_client.view.swing.components;

import io.github.marcodiri.java_socketio_chatroom_core.model.Message;

import javax.swing.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@SuppressWarnings("serial")
public class MessageBoard extends JTextPane {

	private final transient List<Message> history = new ArrayList<>();

	private static final Logger LOGGER = LogManager.getLogger(MessageBoard.class);

	List<Message> getHistory() {
		return history;
	}

	public void newMessageNotify(Message msg) {
		history.add(msg);
		history.sort(Comparator.comparingLong(lhs -> lhs.getTimestamp().getTime()));
		StringBuilder formattedText = new StringBuilder();
		for (Message m : history) {
			if (formattedText.length() > 0) {
				formattedText.insert(formattedText.length(), System.lineSeparator());
			}
			formattedText.append(m.getFormattedMessage());
		}
		setText(formattedText.toString());
		LOGGER.info("Message added to board");
	}

	public void clearBoard() {
		setText("");
		history.clear();
		LOGGER.info("Board cleared");
	}

}
