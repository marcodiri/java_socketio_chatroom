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
		assertThat(board.getHistory()).containsExactly(msg);
		
		String formattedMsg = dateFormat.format(timestamp) + " user: message";
		String x = board.getText();
		assertThat(x).isEqualTo(formattedMsg);
	}
	
	@Test
	public void testNewMessageNotifyWhenBoardIsNotEmpty() {
		Timestamp timestamp = new Timestamp(0);
		DateFormat dateFormat = new SimpleDateFormat("HH:mm");
		
		String previousText =  "Text";
		board.setText(previousText);
		Message msg = new ClientMessage(new Timestamp(0), "user", "message");
		board.newMessageNotify(msg);
		assertThat(board.getHistory()).containsExactly(msg);
		String formattedMsg = dateFormat.format(timestamp) + " user: message";
		assertThat(board.getText()).isEqualTo(previousText + System.lineSeparator() + formattedMsg);
	}
	
	@Test
	public void testClearBoard() {
		board.setText("Text");
		board.clearBoard();
		assertThat(board.getText()).isEmpty();
	}

}
