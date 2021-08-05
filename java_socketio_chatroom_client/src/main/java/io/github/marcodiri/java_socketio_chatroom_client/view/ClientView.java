package io.github.marcodiri.java_socketio_chatroom_client.view;

import io.github.marcodiri.java_socketio_chatroom_core.model.Message;

public interface ClientView {
    void addMessage(Message msg);
    
    void roomJoined(String roomName);

    void showError(String errorMsg);
}
