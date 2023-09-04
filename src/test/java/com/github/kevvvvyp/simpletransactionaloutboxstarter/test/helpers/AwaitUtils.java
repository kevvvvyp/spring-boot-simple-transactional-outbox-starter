package com.github.kevvvvyp.simpletransactionaloutboxstarter.test.helpers;

import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AwaitUtils {
	private static final Duration TIMEOUT = Duration.ofSeconds( 5000 );
	private static final Duration POLL_DURATION = Duration.ofMillis( 10 );
	private static final ExecutorService POLLING_EXECUTOR_SERVICE = Executors.newFixedThreadPool(
			3 );

	public static void doUntil( final Runnable awaitCondition ) {
		await().atMost( TIMEOUT )
				.pollInterval( POLL_DURATION )
				.pollExecutorService( POLLING_EXECUTOR_SERVICE )
				.untilAsserted( awaitCondition::run );
	}
}
