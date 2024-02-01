package io.github.kevvvvyp.simpletransactionaloutboxstarter.service;

import java.util.Collection;

import io.github.kevvvvyp.simpletransactionaloutboxstarter.transfer.Message;

/**
 * An interface defining where the messages should be delivered to after being consumed from
 * the outbox.
 */
public interface OutboxDeliveryStrategy {

	/**
	 * Send a collection of messsages. If an exception is encountered the messages remain in the
	 * outbox to be retried by a thread (or another service instance).
	 *
	 * @param messages
	 */
	void send( final Collection<Message> messages );

	/**
	 * This maps to the Outbox tables `type` field, if no strategy is defined for a given type then
	 * those messages are ignored during polling.
	 *
	 * @return Type String
	 */
	String messageType();
}
