package io.github.marcodiri.java_socketio_chatroom_server.repository.mongo;

import java.sql.Timestamp;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.bson.Document;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import io.github.marcodiri.java_socketio_chatroom_server.model.Message;
import io.github.marcodiri.java_socketio_chatroom_server.repository.ServerRepository;

public class ServerMongoRepository implements ServerRepository {
	public static final String CHATROOM_DB_NAME = "chatroom";
	public static final String MESSAGES_COLLECTION_NAME = "messages";
	private final MongoCollection<Document> msgCollection;

	public ServerMongoRepository(MongoClient client) {
		msgCollection = client
				.getDatabase(CHATROOM_DB_NAME)
				.getCollection(MESSAGES_COLLECTION_NAME);
	}

	@Override
	public List<Message> findAll() {
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
	}

	private Message fromDocumentToMessage(Document d) {
		return new Message(
				new Timestamp(Long.parseLong(d.get("timestamp").toString())), 
				""+d.get("user"), 
				""+d.get("message"));
	}

}
