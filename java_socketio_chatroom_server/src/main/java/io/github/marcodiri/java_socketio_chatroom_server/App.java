package io.github.marcodiri.java_socketio_chatroom_server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mongodb.MongoClient;
import com.mongodb.ServerAddress;
import io.github.marcodiri.java_socketio_chatroom_server.repository.ServerRepository;
import io.github.marcodiri.java_socketio_chatroom_server.repository.mongo.ServerMongoRepository;

public class App {

	private static final Logger LOGGER = LogManager.getLogger(App.class);

    public static void main(String[] args) {
        int mongoPort = Integer.parseInt(System.getProperty("mongo.port", "27017"));
        MongoClient client = new MongoClient(new ServerAddress("localhost", mongoPort));

        ServerRepository mongoRepository = new ServerMongoRepository(client);

        ChatroomServer chatroomServer = new ChatroomServer(mongoRepository);
        try {
            chatroomServer.start();
        } catch (Exception e) {
        	LOGGER.fatal("Server could not be started: {}", e.getMessage());
        }
        
        Runtime.getRuntime().addShutdownHook(new Thread(() -> stopServer(chatroomServer)));
        
        try {
            Thread.sleep(Long.MAX_VALUE);
        } catch (InterruptedException e) {
            stopServer(chatroomServer);
            Thread.currentThread().interrupt();
        }
    }

	private static void stopServer(ChatroomServer chatroomServer) {
		try {
		    chatroomServer.stop();
		} catch (Exception e) {
		    LOGGER.fatal("Server could not be stopped: {}", e.getMessage());
		}
	}
}
