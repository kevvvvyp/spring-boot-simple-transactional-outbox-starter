package io.github.kevvvvyp.simpletransactionaloutboxstarter.service.delivery;

import static java.time.Instant.now;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toMap;

import static org.springframework.data.domain.PageRequest.of;
import static org.springframework.transaction.annotation.Isolation.READ_COMMITTED;

import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import io.github.kevvvvyp.simpletransactionaloutboxstarter.config.OutboxConfiguration;
import io.github.kevvvvyp.simpletransactionaloutboxstarter.domain.model.OutboxEntity;
import io.github.kevvvvyp.simpletransactionaloutboxstarter.domain.repository.ro.OutboxRoRepository;
import io.github.kevvvvyp.simpletransactionaloutboxstarter.domain.repository.rw.OutboxRwRepository;
import io.github.kevvvvyp.simpletransactionaloutboxstarter.service.MessageOutboxEntityTransformer;
import io.github.kevvvvyp.simpletransactionaloutboxstarter.service.OutboxDeliveryStrategy;
import io.github.kevvvvyp.simpletransactionaloutboxstarter.transfer.Message;

import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;

/**
 * Transactional Outbox Service DAO
 */
@Slf4j
@Service
public class OutboxDao {
	private final OutboxRoRepository readOnlyRepository;
	private final OutboxRwRepository readWriteRepository;
	private final OutboxConfiguration config;
	private final Map<String, OutboxDeliveryStrategy> deliveryStrategyByMessageType;

	/**
	 * @param readOnlyRepository
	 * @param readWriteRepository
	 * @param config
	 * @param deliveryStrategies
	 */
	@Autowired
	protected OutboxDao( @NotNull final OutboxRoRepository readOnlyRepository,
			@NotNull final OutboxRwRepository readWriteRepository,
			@NotNull final OutboxConfiguration config,
			@NotNull final Collection<OutboxDeliveryStrategy> deliveryStrategies ) {
		this.config = config;
		this.readOnlyRepository = readOnlyRepository;
		this.readWriteRepository = readWriteRepository;
		this.deliveryStrategyByMessageType = deliveryStrategies.stream()
				.collect( toMap( OutboxDeliveryStrategy::messageType, strategy -> strategy ) );
	}

	/**
	 * @return True if empty, false if otherwise
	 */
	public boolean isNotEmpty() {
		return !deliveryStrategyByMessageType.isEmpty() && readOnlyRepository.availableMessages(
				now(), now().minus( config.getLock() ), deliveryStrategyByMessageType.keySet() );
	}

	/**
	 * Lock one or more messages in a batch. This ties these messages to a single thread until
	 * they are either processed or the claim expiry elapses.
	 *
	 * @param lockId
	 * @return Map of the messages by their unique identifiers
	 */
	@Transactional(rollbackFor = Exception.class,
			propagation = Propagation.REQUIRED,
			isolation = READ_COMMITTED)
	public Map<UUID, Message> lock( final UUID lockId ) {
		final var page = readWriteRepository.findAvailable( now(), now().minus( config.getLock() ),
				of( 0, config.getBatchSize() ), deliveryStrategyByMessageType.keySet() );

		if ( !page.hasContent() ) {
			return Map.of();

		} else {
			final var messagesByIds = page.getContent()
					.stream()
					.collect( toMap( OutboxEntity::getId,
							MessageOutboxEntityTransformer::toMessage ) );

			// Lock messages
			final int locked = readWriteRepository.lock( lockId, messagesByIds.keySet(),
					Instant.now() );

			readWriteRepository.flush();

			if ( messagesByIds.size() != locked ) {
				throw new RuntimeException( "Failed to obtain lock for all ids" );
			}

			return messagesByIds;
		}
	}

	/**
	 * Send messages to the delivery strategy, if they are successfully consumed then we delete
	 * these messages from our outbox.
	 *
	 * @param messagesById
	 */
	public void send( final Map<UUID, Message> messagesById ) {

		if ( deliveryStrategyByMessageType.isEmpty() ) {
			throw new UnsupportedOperationException( "No delivery strategies registered" );
		}

		final var messagesByType = messagesById.values()
				.stream()
				.collect( groupingBy( Message::type ) );

		final var messagesByDeliveryStrategy = messagesByType.entrySet()
				.stream()
				.collect( toMap( entry -> deliveryStrategyByMessageType.get( entry.getKey() ),
						Map.Entry::getValue ) );

		messagesByDeliveryStrategy.forEach( OutboxDeliveryStrategy::send );
	}

	/**
	 * Delete messages by their ids.
	 *
	 * @param messagesById
	 */
	@Transactional(rollbackFor = Exception.class)
	public void delete( final Map<UUID, Message> messagesById ) {
		readWriteRepository.deleteAllById( messagesById.keySet() );
	}
}