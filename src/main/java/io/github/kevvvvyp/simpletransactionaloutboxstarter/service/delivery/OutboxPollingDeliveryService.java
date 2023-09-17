package io.github.kevvvvyp.simpletransactionaloutboxstarter.service.delivery;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.stereotype.Service;

import io.github.kevvvvyp.simpletransactionaloutboxstarter.config.OutboxConfiguration;
import io.github.kevvvvyp.simpletransactionaloutboxstarter.service.TransactionalOutboxService;
import io.github.kevvvvyp.simpletransactionaloutboxstarter.transfer.Message;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class OutboxPollingDeliveryService implements OutboxDeliveryService {
	private final AtomicBoolean state;
	private final ExecutorService executorService;
	private final OutboxConfiguration config;
	private final OutboxDao outboxDao;
	private final Counter lockedCounter;
	private final Counter sentCounter;
	private final Counter deletedCounter;
	private final Counter errorCounter;

	public OutboxPollingDeliveryService( final OutboxConfiguration config,
			final OutboxDao outboxDao, final MeterRegistry meterRegistry ) {

		this.config = config;
		this.state = new AtomicBoolean();
		this.outboxDao = outboxDao;
		this.executorService = Executors.newFixedThreadPool( config.getPollingPool() );

		this.lockedCounter = meterRegistry.counter(
				TransactionalOutboxService.class.getName() + "_locked" );
		this.sentCounter = meterRegistry.counter(
				TransactionalOutboxService.class.getName() + "_sent" );
		this.deletedCounter = meterRegistry.counter(
				TransactionalOutboxService.class.getName() + "_deleted" );
		this.errorCounter = meterRegistry.counter(
				TransactionalOutboxService.class.getName() + "_error" );
	}

	private static Duration applyJitter( final Duration duration, final double factor ) {
		final double d = duration.toMillis();
		final long min = ( (Double) ( d - d * factor ) ).longValue();
		final long max = ( (Double) ( d + d * factor ) ).longValue();
		return Duration.ofMillis( ThreadLocalRandom.current().nextLong( min, max + 1 ) );
	}

	public void start() {
		if ( !isRunning() ) {
			state.compareAndSet( false, true );

			for ( int i = 0; i < config.getPollingPool(); i++ ) {
				executorService.submit( this::process );
			}
		}
	}

	public void stop() {
		state.compareAndSet( true, false );
	}

	public boolean isRunning() {
		return state.get();
	}

	public void process() {
		final UUID pid = UUID.randomUUID();
		log.info( "OutboxPoller {} is ON", pid );
		boolean isIdle = true;

		while ( isRunning() ) {
			try {
				if ( outboxDao.isNotEmpty() ) {
					final Map<UUID, Message> messagesById = outboxDao.lock( pid );
					isIdle = messagesById.isEmpty();

					final int n = messagesById.size();
					lockedCounter.increment( n );

					outboxDao.send( messagesById );
					sentCounter.increment( n );

					outboxDao.delete( messagesById );
					deletedCounter.increment( n );

					if ( log.isTraceEnabled() ) {
						log.trace( "OutboxPoller {} processed {} messages : {}", pid, n,
								messagesById.values()
										.stream()
										.map( Message::deduplicationKey )
										.toList() );
					}
				}
			} catch ( final Exception e ) {
				log.error( "Polling process failed", e );
				errorCounter.increment();
			} finally {
				if ( isIdle ) {
					applyBackOff( config.getIdleBackoff() ); // Idle Back Off
				} else {
					applyBackOff( config.getProcessingBackoff() ); // Busy back off
				}
			}
		}

		log.info( "OutboxPoller {} is OFF", pid );
	}

	private void applyBackOff( final Duration backoff ) {
		if ( !Duration.ZERO.equals( backoff ) ) {
			try {
				final double jitter = config.getJitter();
				final Duration d = jitter > 0 ? applyJitter( backoff,
						config.getJitter() ) : backoff;
				Thread.sleep( d.toMillis() );
			} catch ( final InterruptedException e ) {
				throw new RuntimeException( "Failed to apply back off", e );
			}
		}
	}
}
