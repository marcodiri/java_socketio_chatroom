package io.github.marcodiri.java_socketio_chatroom_client.view.swing.components;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.swing.JTextPane;

import io.github.marcodiri.java_socketio_chatroom_core.model.Message;

@SuppressWarnings("serial")
public class MessageBoard extends JTextPane {

    private final transient List<Message> history = new ArrayList<>();

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
    }

    public void clearBoard() {
        setText("");
        history.clear();
    }

}
