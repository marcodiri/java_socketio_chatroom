package io.github.marcodiri.java_socketio_chatroom_client;

import io.github.marcodiri.java_socketio_chatroom_client.view.swing.ClientSwingView;
import io.github.marcodiri.java_socketio_chatroom_client.view.swing.components.MessageBoard;
import io.socket.client.IO;

import java.awt.*;
import java.net.URI;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class App {
	private static final Logger LOGGER = LogManager.getLogger(App.class);
			
    public static void main(String[] args) {
    	LOGGER.info("Client app started");
        EventQueue.invokeLater(() -> {
            try {
                MessageBoard board = new MessageBoard();
                ClientSwingView frame = new ClientSwingView(board);
                ChatroomClient client = new ChatroomClient(URI.create("http://localhost:3000"), IO.Options.builder().build(), frame);
                frame.setClient(client);
                frame.setVisible(true);
            } catch (Exception e) {
            }
        });
    }
}
