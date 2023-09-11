package io.github.kevvvvyp.simpletransactionaloutboxstarter.service;

import static java.util.Optional.ofNullable;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import io.github.kevvvvyp.simpletransactionaloutboxstarter.domain.model.OutboxEntity;
import io.github.kevvvvyp.simpletransactionaloutboxstarter.transfer.Message;

public class MessageOutboxEntityTransformer {
	public static OutboxEntity toEntity( final Message message ) {
		final Instant scheduledAfter = ofNullable( message.scheduleAfter() ).orElseGet(
				Instant::now );

		return OutboxEntity.builder()
				.type( message.type() )
				.sender( message.sender() )
				.recipient( message.recipient() )
				.subject( message.subject() )
				.body( message.body() )
				.updatedAt( Instant.now() )
				.scheduledAfter( scheduledAfter.truncatedTo( ChronoUnit.SECONDS ) )
				.deduplicationKey( message.deduplicationKey() )
				.build();
	}

	public static Message toMessage( final OutboxEntity entity ) {
		return Message.builder()
				.type( entity.getType() )
				.sender( entity.getSender() )
				.recipient( entity.getRecipient() )
				.subject( entity.getSubject() )
				.body( entity.getBody() )
				.scheduleAfter( entity.getScheduledAfter().truncatedTo( ChronoUnit.SECONDS ) )
				.deduplicationKey( entity.getDeduplicationKey() )
				.build();
	}
}
