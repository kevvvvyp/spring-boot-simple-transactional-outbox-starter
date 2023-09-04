package com.github.kevvvvyp.simpletransactionaloutboxstarter.service;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

import com.github.kevvvvyp.simpletransactionaloutboxstarter.transfer.Message;

public interface TransactionalOutboxService {
	/**
	 * By invoking the service will begin deliver any available messages.
	 */
	void start();

	/**
	 * By invoking the service will cease to deliver any available messages.
	 * Any new messages will be persisted in the outbox.
	 */
	void stop();

	/**
	 * @return True if the service is running.
	 */
	boolean isRunning();

	/**
	 * Registers messages to be sent according to their `scheduled_at` date
	 *
	 * @return The message's unique identifier
	 */
	UUID register( final Message message );

	/**
	 * Bulk registers messages to be sent according to their `scheduled_at` date
	 *
	 * @return A Map of messages by their unique identifiers
	 */
	Map<UUID, Message> registerAll( final Collection<Message> messages );

	/**
	 * @return True if the outbox contains a message with this id.
	 * @implNote The intended usage is to look up scheduled messages yet to be delivered, otherwise we would
	 * likely not find a message due to the constant processing.
	 */
	boolean isRegistered( final UUID messageId );
}
