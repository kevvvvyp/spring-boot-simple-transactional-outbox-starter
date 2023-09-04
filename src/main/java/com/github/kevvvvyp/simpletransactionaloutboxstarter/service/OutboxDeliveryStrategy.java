package com.github.kevvvvyp.simpletransactionaloutboxstarter.service;

import java.util.Collection;

import com.github.kevvvvyp.simpletransactionaloutboxstarter.transfer.Message;

public interface OutboxDeliveryStrategy {
	void send( final Collection<Message> messages );
	String messageType();
}
