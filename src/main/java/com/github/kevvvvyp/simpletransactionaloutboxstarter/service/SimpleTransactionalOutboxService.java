package com.github.kevvvvyp.simpletransactionaloutboxstarter.service;

import static java.util.Objects.requireNonNull;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.github.kevvvvyp.simpletransactionaloutboxstarter.service.delivery.OutboxDeliveryService;
import com.github.kevvvvyp.simpletransactionaloutboxstarter.service.delivery.OutboxPollingDeliveryService;
import com.github.kevvvvyp.simpletransactionaloutboxstarter.service.registration.OutboxRegistrationService;
import com.github.kevvvvyp.simpletransactionaloutboxstarter.transfer.Message;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class SimpleTransactionalOutboxService implements TransactionalOutboxService {

	private final OutboxDeliveryService deliveryService;
	private final OutboxRegistrationService schedulingService;

	@Autowired
	public SimpleTransactionalOutboxService( final OutboxPollingDeliveryService deliveryService,
			final OutboxRegistrationService schedulingService ) {
		this.deliveryService = deliveryService;
		this.schedulingService = schedulingService;
	}

	@Override
	public void start() {
		requireNonNull( deliveryService, "No delivery strategy has been set." );
		deliveryService.start();
		log.info( "Started service: {}", TransactionalOutboxService.class.getSimpleName() );
	}

	@Override
	public void stop() {
		deliveryService.stop();
		log.info( "Stopped service: {}", TransactionalOutboxService.class.getSimpleName() );
	}

	@Override
	public boolean isRunning() {
		return deliveryService.isRunning();
	}

	@Override
	public UUID register( final Message message ) {
		return schedulingService.register( message );
	}

	@Override
	public Map<UUID, Message> registerAll( final Collection<Message> messages ) {
		return schedulingService.registerAll( messages );
	}

	public boolean isRegistered( final UUID messageId ) {
		return schedulingService.isRegistered( messageId );
	}
}
