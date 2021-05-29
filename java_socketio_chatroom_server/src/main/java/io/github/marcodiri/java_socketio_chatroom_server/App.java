package io.github.marcodiri.java_socketio_chatroom_server;

import com.mongodb.MongoClient;
import com.mongodb.ServerAddress;
import io.github.marcodiri.java_socketio_chatroom_server.repository.ServerRepository;
import io.github.marcodiri.java_socketio_chatroom_server.repository.mongo.ServerMongoRepository;

public class App {
    public static void main(String[] args) {
        int mongoPort = Integer.parseInt(System.getProperty("mongo.port", "27017"));
        MongoClient client = new MongoClient(new ServerAddress("localhost", mongoPort));

        ServerRepository mongoRepository = new ServerMongoRepository(client);

        ChatroomServer chatroomServer = new ChatroomServer(mongoRepository);
        try {
            chatroomServer.start();
        } catch (Exception e) {

        }
    }
}
