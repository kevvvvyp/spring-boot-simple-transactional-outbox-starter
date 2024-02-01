package io.github.kevvvvyp.simpletransactionaloutboxstarter.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

/**
 * Library configuration properties
 */
@Data
@ConfigurationProperties("simple.outbox")
public class OutboxConfiguration {
	/**
	 * simple.outbox.enabled - True if you wish the library to deliver messages stored in the
	 * database, false if you wish the message to remain undelivered (i.e. no database reads/polling).
	 */
	boolean enabled;

	/**
	 * simple.outbox.batchSize - The number of messages you wish the library to consume at once &
	 * attempt to deliver.
	 */
	int batchSize;

	/**
	 * simple.outbox.pollingPool - The size of the polling thread pool attempting to find
	 * undeliverable messages, must be at least 1.
	 */
	int pollingPool;

	/**
	 * simple.outbox.jitter - Apply Jitter to the database polling, this is useful so that all
	 * threads don't attempt to consume the same set of messages all at once, it splays the load
	 * on the db read instance. If set to 0 no jitter is applied.
	 */
	double jitter;

	/**
	 * simple.outbox.lock - The duration to 'lock' messages for. Once the lock duration is exceeded
	 * another process is free to then lock those messages if they remain undelivered.
	 */
	Duration lock;

	/**
	 * simple.outbox.idleBackoff - The duration to wait between polling the database for
	 * undelivered messages, it is applied when there are no messages to process.
	 * Can be combined with Jitter to splay database load. Setting this to 0 results in no backoff
	 * being applied.
	 */
	Duration idleBackoff;

	/**
	 * simple.outbox.processingBackoff - The duration to wait between polling the database for
	 * undelivered messages, it is applied when there are messages to be processed. Can be combined with Jitter to splay database load. Setting this to 0 results in no backoff being applied.
	 */
	Duration processingBackoff;
}
