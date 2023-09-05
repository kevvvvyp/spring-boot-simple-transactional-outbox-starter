package io.github.kevvvvyp.simpletransactionaloutboxstarter.service.registration;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

import io.github.kevvvvyp.simpletransactionaloutboxstarter.domain.model.OutboxEntity;
import io.github.kevvvvyp.simpletransactionaloutboxstarter.domain.repository.ro.OutboxRoRepository;
import io.github.kevvvvyp.simpletransactionaloutboxstarter.domain.repository.rw.OutboxRwRepository;
import io.github.kevvvvyp.simpletransactionaloutboxstarter.service.MessageOutboxEntityTransformer;
import io.github.kevvvvyp.simpletransactionaloutboxstarter.service.OutboxDeliveryStrategy;
import io.github.kevvvvyp.simpletransactionaloutboxstarter.service.TransactionalOutboxService;
import io.github.kevvvvyp.simpletransactionaloutboxstarter.transfer.Message;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class OutboxRegistrationServiceImpl implements OutboxRegistrationService {

	private final OutboxRwRepository readWriteRepository;
	private final OutboxRoRepository readOnlyRepository;
	private final Counter registeredMessagesCounter;
	private final Set<String> deliverableMessageTypes;

	public OutboxRegistrationServiceImpl( final OutboxRwRepository readWriteRepository,
			final OutboxRoRepository readOnlyRepository, final MeterRegistry meterRegistry,
			final Collection<OutboxDeliveryStrategy> deliveryStrategies ) {
		this.readWriteRepository = readWriteRepository;
		this.readOnlyRepository = readOnlyRepository;
		this.deliverableMessageTypes = deliveryStrategies.stream()
				.map( OutboxDeliveryStrategy::messageType )
				.collect( Collectors.toSet() );
		this.registeredMessagesCounter = meterRegistry.counter(
				TransactionalOutboxService.class.getName() + "_registered" );
	}

	private void throwIfSomeMessagesAreUndeliverable( final Collection<Message> messages ) {
		final List<String> missingTypes = messages.stream()
				.map( Message::type )
				.filter( t -> !deliverableMessageTypes.contains( t ) )
				.toList();

		if ( !missingTypes.isEmpty() ) {
			final String errorMsg = String.format(
					"Cannot register messages as there is no corresponding delivery strategy set for types: %s",
					missingTypes );
			log.error( errorMsg );
			throw new UnsupportedOperationException( errorMsg );
		}
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public UUID register( final Message message ) {
		throwIfSomeMessagesAreUndeliverable( List.of( message ) );

		final OutboxEntity entity = MessageOutboxEntityTransformer.toEntity( message );
		final Optional<OutboxEntity> existingEntity = readOnlyRepository.findByDeduplicationKey(
				message.deduplicationKey() );

		if ( existingEntity.isPresent() ) {
			return null;
		} else {
			final UUID saved = readWriteRepository.save( entity ).getId();
			registeredMessagesCounter.increment();
			return saved;
		}
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public Map<UUID, Message> registerAll( final Collection<Message> messages ) {
		throwIfSomeMessagesAreUndeliverable( messages );

		// Filter duplications in software
		final Map<String, OutboxEntity> entitiesByDedupKey = messages.stream()
				.map( MessageOutboxEntityTransformer::toEntity )
				.collect( Collectors.toMap( OutboxEntity::getDeduplicationKey, o -> o,
						( t, t2 ) -> t ) );

		// Filter out duplicates that already existing in the database
		readOnlyRepository.findAllByDeduplicationKeyIn( entitiesByDedupKey.keySet() )
				.stream()
				.map( OutboxEntity::getDeduplicationKey )
				.forEach( entitiesByDedupKey::remove );

		// Save whats left
		final Map<UUID, Message> saved = readWriteRepository.saveAllAndFlush(
						entitiesByDedupKey.values() )
				.stream()
				.collect( Collectors.toMap( OutboxEntity::getId,
						MessageOutboxEntityTransformer::toMessage ) );

		registeredMessagesCounter.increment( saved.size() );

		saved.entrySet()
				.stream()
				.filter( entry -> !deliverableMessageTypes.contains( entry.getValue().type() ) )
				.forEach( entry -> log.warn(
						"Message {} is undeliverable as no delivery strategy has been registered for type: {}",
						entry.getKey(), entry.getValue().type() ) );

		return saved;
	}

	@Override
	public boolean isRegistered( final UUID messageId ) {
		return readOnlyRepository.existsById( messageId );
	}
}
