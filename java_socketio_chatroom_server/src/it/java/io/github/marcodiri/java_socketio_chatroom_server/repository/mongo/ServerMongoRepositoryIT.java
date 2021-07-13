package io.github.marcodiri.java_socketio_chatroom_server.repository.mongo;

import static io.github.marcodiri.java_socketio_chatroom_server.repository.mongo.ServerMongoRepository.CHATROOM_DB_NAME;
import static io.github.marcodiri.java_socketio_chatroom_server.repository.mongo.ServerMongoRepository.MESSAGES_COLLECTION_NAME;

import java.sql.Timestamp;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import org.bson.Document;
import org.junit.*;

import com.mongodb.MongoClient;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import io.github.marcodiri.java_socketio_chatroom_server.model.ServerMessage;

public class ServerMongoRepositoryIT {
	private static final int mongoPort = Integer.parseInt(System.getProperty("mongo.port", "27017"));
	private static ServerMongoRepository serverRepository;
	private static MongoCollection<Document> messagesCollection;

	private static MongoClient client;


	@BeforeClass
	public static void setup() {
		client = new MongoClient(
				new ServerAddress("localhost", mongoPort)
		);
		serverRepository = new ServerMongoRepository(client);
		MongoDatabase database = client.getDatabase(CHATROOM_DB_NAME);
		messagesCollection = database.getCollection(MESSAGES_COLLECTION_NAME);
	}

	@AfterClass
	public static void closeConnection() {
		client.close();
	}

	@After
	public void dropMongoDb() {
		client.getDatabase(CHATROOM_DB_NAME).drop();
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
					new ServerMessage(ts1, "user1", "message1"),
					new ServerMessage(ts2, "user2", "message2")
					);
	}
	
	@Test
	public void testSave() {
		ServerMessage msg = new ServerMessage(new Timestamp(System.currentTimeMillis()), "user", "message");
		serverRepository.save(msg);
		assertThat(readAllMessages()).containsExactly(msg);
	}
	
	private List<ServerMessage> readAllMessages() {
		return StreamSupport.stream(messagesCollection.find().spliterator(), false)
				.map(d -> new ServerMessage(
						new Timestamp(Long.parseLong(d.get("timestamp").toString())), 
						"" + d.get("user"),
						"" + d.get("message")))
				.collect(Collectors.toList());
	}
}
