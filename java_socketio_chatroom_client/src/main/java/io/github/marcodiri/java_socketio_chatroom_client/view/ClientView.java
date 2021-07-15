package io.github.marcodiri.java_socketio_chatroom_client.view;

import io.github.marcodiri.java_socketio_chatroom_core.model.Message;

public interface ClientView {
    public void addMessage(Message msg);
    
    public void connectedToServer();

    public void roomJoined(String roomName);
}
