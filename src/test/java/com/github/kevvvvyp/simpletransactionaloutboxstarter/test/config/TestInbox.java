package com.github.kevvvvyp.simpletransactionaloutboxstarter.test.config;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.stereotype.Component;

import com.github.kevvvvyp.simpletransactionaloutboxstarter.service.OutboxDeliveryStrategy;
import com.github.kevvvvyp.simpletransactionaloutboxstarter.transfer.Message;

import lombok.Getter;

@Component
public class TestInbox implements OutboxDeliveryStrategy {

	@Getter
	private final List<Message> receivedMessages;
	private final AtomicBoolean simulateFailure;

	public TestInbox() {
		this.receivedMessages = new CopyOnWriteArrayList<>();
		this.simulateFailure = new AtomicBoolean( false );
	}

	@Override
	public void send( final Collection<Message> messages ) {
		if ( simulateFailure.get() ) {
			throw new RuntimeException( "Simulated delivery failure" );
		} else {
			receivedMessages.addAll( messages );
		}
	}

	@Override
	public String messageType() {
		return "Simple";
	}

	public void simulateDeliveryFailure() {
		simulateFailure.compareAndSet( false, true );
	}

	public void stopSimulatingDeliveryFailure() {
		simulateFailure.compareAndSet( true, false );
	}
}
