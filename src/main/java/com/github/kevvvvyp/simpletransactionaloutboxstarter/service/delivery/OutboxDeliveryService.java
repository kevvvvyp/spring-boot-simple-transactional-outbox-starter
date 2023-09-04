package com.github.kevvvvyp.simpletransactionaloutboxstarter.service.delivery;

/**
 * Transactional Outbox Service
 */
public interface OutboxDeliveryService {

	void start();

	void stop();

	boolean isRunning();
}