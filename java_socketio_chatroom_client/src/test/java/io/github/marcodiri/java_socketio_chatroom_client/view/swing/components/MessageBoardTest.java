package io.github.marcodiri.java_socketio_chatroom_client.view.swing.components;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import org.junit.Before;
import org.junit.Test;

import io.github.marcodiri.java_socketio_chatroom_client.model.ClientMessage;
import io.github.marcodiri.java_socketio_chatroom_core.model.Message;

public class MessageBoardTest {

    private MessageBoard board;

    @Before
    public void setup() {
        board = new MessageBoard();
    }

    @Test
    public void testNewMessageNotifyWhenBoardIsEmpty() {
        Timestamp timestamp = new Timestamp(0);
        DateFormat dateFormat = new SimpleDateFormat("HH:mm");

        Message msg = new ClientMessage(timestamp, "user", "message");
        board.newMessageNotify(msg);
        assertThat(board.getHistory()).contains(msg);

        String formattedMsg = dateFormat.format(timestamp) + " user: message";
        String x = board.getText();
        assertThat(x).isEqualTo(formattedMsg);
    }

    @Test
    public void testNewMessageNotifyWhenHistoryIsNotEmpty() {
        DateFormat dateFormat = new SimpleDateFormat("HH:mm");
        Timestamp timestamp = new Timestamp(0);

        Message previousMessage = new ClientMessage(timestamp, "user1", "message1");
        board.getHistory().add(previousMessage);
        Message msg = new ClientMessage(timestamp, "user2", "message2");
        board.newMessageNotify(msg);
        assertThat(board.getHistory()).contains(previousMessage, msg);
        String expectedText = dateFormat.format(timestamp) + " user1: message1" + System.lineSeparator()
                + dateFormat.format(timestamp) + " user2: message2";
        assertThat(board.getText()).isEqualTo(expectedText);
    }

    @Test
    public void testNewMessageNotifyPrintsMessagesInSortedOrder() {
        DateFormat dateFormat = new SimpleDateFormat("HH:mm");

        Timestamp timestamp1 = new Timestamp(0);
        Message olderMessage = new ClientMessage(timestamp1, "user1", "message1");

        Timestamp timestamp2 = new Timestamp(1);
        Message newerMessage = new ClientMessage(timestamp2, "user2", "message2");

        board.newMessageNotify(newerMessage);
        board.newMessageNotify(olderMessage);

        assertThat(board.getHistory()).containsExactly(olderMessage, newerMessage);
        String expectedText = dateFormat.format(timestamp1) + " user1: message1" + System.lineSeparator()
                + dateFormat.format(timestamp2) + " user2: message2";
        assertThat(board.getText()).isEqualTo(expectedText);
    }

    @Test
    public void testClearBoard() {
        board.setText("Text");
        board.clearBoard();
        assertThat(board.getText()).isEmpty();
    }

}
