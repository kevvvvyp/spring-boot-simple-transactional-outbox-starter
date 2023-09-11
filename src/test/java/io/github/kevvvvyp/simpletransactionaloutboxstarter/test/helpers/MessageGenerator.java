package io.github.kevvvvyp.simpletransactionaloutboxstarter.test.helpers;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import com.github.javafaker.Faker;
import io.github.kevvvvyp.simpletransactionaloutboxstarter.transfer.Message;

public class MessageGenerator {
	public static final Faker FAKER = new Faker();
	private static final AtomicInteger COUNTER = new AtomicInteger();

	public static List<Message> generateDuplicateMessages( final int number ) {
		final Message message = generateRandomMessage();

		return IntStream.rangeClosed( 1, number ).mapToObj( value -> message ).toList();
	}

	public static List<Message> generateRandomMessages( final int number ) {
		return IntStream.rangeClosed( 1, number )
				.mapToObj( value -> generateRandomMessage() )
				.toList();
	}

	public static Message generateRandomMessage() {
		final String subject = FAKER.color().name();
		final String recipient = FAKER.internet().emailAddress();

		return Message.builder()
				.type( "Simple" )
				.sender( FAKER.internet().emailAddress() )
				.recipient( recipient )
				.subject( subject )
				.deduplicationKey( String.valueOf( COUNTER.getAndIncrement() ) )
				.scheduleAfter( Instant.now().truncatedTo( ChronoUnit.SECONDS ) )
				.body( String.format( "{\"email\":\"%s\"}", recipient ) )
				.build();
	}

	public static Message addDelay( final Message message, final Duration delay ) {
		return message.toBuilder()
				.scheduleAfter(
						message.scheduleAfter().plus( delay ).truncatedTo( ChronoUnit.SECONDS ) )
				.build();
	}

	public static List<Message> addDelay( final List<Message> messages, final Duration delay ) {
		return messages.stream()
				.map( message -> message.toBuilder()
						.scheduleAfter( message.scheduleAfter()
								.plus( delay )
								.truncatedTo( ChronoUnit.SECONDS ) )
						.build() )
				.toList();
	}
}
