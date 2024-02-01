package io.github.kevvvvyp.simpletransactionaloutboxstarter.service.registration;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

import io.github.kevvvvyp.simpletransactionaloutboxstarter.transfer.Message;

/**
 * Interface for registering new messages i.e. adding them to the outbox for delivery.
 */
public interface OutboxRegistrationService {

	/**
	 * Register a message to be sent
	 * @param message
	 * @return ID of message
	 * @implNote The message contains the date/time of when it is to be sent
	 */
	UUID register( final Message message );

	/**
	 * Bulk register
	 * @param messages
	 * @return
	 */
	Map<UUID, Message> registerAll( final Collection<Message> messages );

	/**
	 * @param messageId
	 * @return True if message is sitting in the outbox already (i.e. it has not been sent).
	 */
 	boolean isRegistered( final UUID messageId );
}