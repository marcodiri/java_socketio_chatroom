package io.github.marcodiri.java_socketio_chatroom_server.repository.mongo;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import io.github.marcodiri.java_socketio_chatroom_core.model.Message;
import io.github.marcodiri.java_socketio_chatroom_server.model.ServerMessage;
import io.github.marcodiri.java_socketio_chatroom_server.repository.ServerRepository;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.Document;

import java.sql.Timestamp;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class ServerMongoRepository implements ServerRepository {
	public static final String CHATROOM_DB_NAME = "chatroom";
	public static final String MESSAGES_COLLECTION_NAME = "messages";
	private final MongoCollection<Document> msgCollection;

	private static final Logger LOGGER = LogManager.getLogger(ServerMongoRepository.class);

	public ServerMongoRepository(MongoClient client) {
		msgCollection = client
				.getDatabase(CHATROOM_DB_NAME)
				.getCollection(MESSAGES_COLLECTION_NAME);
	}

	@Override
	public List<Message> findAll() {
		LOGGER.info("Retrieving all messages from db");
		return StreamSupport.
				stream(msgCollection.find().spliterator(), false)
				.map(this::fromDocumentToMessage)
				.collect(Collectors.toList());
	}

	@Override
	public void save(Message message) {
		msgCollection.insertOne(
				new Document()
						.append("timestamp", message.getTimestamp().getTime())
						.append("user", message.getUser())
						.append("message", message.getUserMessage()));
		LOGGER.info("Saved {} in db", message);
	}

	private Message fromDocumentToMessage(Document d) {
		return new ServerMessage(
				new Timestamp(Long.parseLong(d.get("timestamp").toString())),
				"" + d.get("user"),
				"" + d.get("message"));
	}

}
