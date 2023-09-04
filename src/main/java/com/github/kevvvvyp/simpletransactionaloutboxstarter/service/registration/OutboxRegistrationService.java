package com.github.kevvvvyp.simpletransactionaloutboxstarter.service.registration;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

import com.github.kevvvvyp.simpletransactionaloutboxstarter.transfer.Message;

public interface OutboxRegistrationService {
	UUID register( final Message message );

	Map<UUID, Message> registerAll( final Collection<Message> messages );

 	boolean isRegistered( final UUID messageId );
}