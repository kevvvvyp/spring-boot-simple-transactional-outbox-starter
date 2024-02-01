package io.github.kevvvvyp.simpletransactionaloutboxstarter.service.delivery;

/**
 * Interface for controlling the processing of messages within the outbox.
 */
public interface OutboxDeliveryService {

	/**
	 * Start the process to look for available messages and delivery them to consumers.
	 */
	void start();

	/**
	 * Stop the service, this may mean messages in the outbox build up over time.
	 */
	void stop();

	/**
	 * @return True if we are processing messages
	 */
	boolean isRunning();
}