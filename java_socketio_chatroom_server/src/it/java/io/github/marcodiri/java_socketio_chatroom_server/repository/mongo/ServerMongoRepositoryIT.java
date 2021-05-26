package io.github.marcodiri.java_socketio_chatroom_server.repository.mongo;

import static io.github.marcodiri.java_socketio_chatroom_server.repository.mongo.ServerMongoRepository.CHATROOM_DB_NAME;
import static io.github.marcodiri.java_socketio_chatroom_server.repository.mongo.ServerMongoRepository.MESSAGES_COLLECTION_NAME;

import java.sql.Timestamp;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

import org.bson.Document;
import org.junit.Before;
import org.junit.Test;

import com.mongodb.MongoClient;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import io.github.marcodiri.java_socketio_chatroom_server.model.Message;

public class ServerMongoRepositoryIT {
	private static int mongoPort = Integer.parseInt(System.getProperty("mongo.port", "27017"));
	private MongoClient client;
	private ServerMongoRepository serverRepository;
	private MongoCollection<Document> messagesCollection;

	@Before
	public void setup() {
		client = new MongoClient(
				new ServerAddress("localhost", mongoPort)
				);
		serverRepository = new ServerMongoRepository(client);
		MongoDatabase database = client.getDatabase(CHATROOM_DB_NAME);
		database.drop();
		messagesCollection = database.getCollection(MESSAGES_COLLECTION_NAME);
	}

	@Test
	public void testFindAll() {
		Timestamp ts1 = new Timestamp(System.currentTimeMillis());
		Timestamp ts2 = new Timestamp(System.currentTimeMillis());
		
		messagesCollection.insertMany(asList(
				new Document()
				.append("timestamp", ts1.getTime())
				.append("user", "user1")
				.append("message", "message1"),
				new Document()
				.append("timestamp", ts2.getTime())
				.append("user", "user2")
				.append("message", "message2")
				));

		assertThat(serverRepository.findAll())
			.containsExactly(
					new Message(ts1, "user1", "message1"),
					new Message(ts2, "user2", "message2")
					);
	}
}
