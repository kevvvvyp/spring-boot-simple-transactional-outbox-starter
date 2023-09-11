package io.github.kevvvvyp.simpletransactionaloutboxstarter.service;

import static java.time.Duration.between;
import static java.time.Duration.ofMillis;
import static java.time.Duration.ofSeconds;
import static java.time.Instant.now;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.TestInstance.Lifecycle;

import static io.github.kevvvvyp.simpletransactionaloutboxstarter.test.helpers.AwaitUtils.doUntil;
import static io.github.kevvvvyp.simpletransactionaloutboxstarter.test.helpers.MessageGenerator.FAKER;
import static io.github.kevvvvyp.simpletransactionaloutboxstarter.test.helpers.MessageGenerator.addDelay;
import static io.github.kevvvvyp.simpletransactionaloutboxstarter.test.helpers.MessageGenerator.generateDuplicateMessages;
import static io.github.kevvvvyp.simpletransactionaloutboxstarter.test.helpers.MessageGenerator.generateRandomMessage;
import static io.github.kevvvvyp.simpletransactionaloutboxstarter.test.helpers.MessageGenerator.generateRandomMessages;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import io.github.kevvvvyp.simpletransactionaloutboxstarter.config.OutboxConfiguration;
import io.github.kevvvvyp.simpletransactionaloutboxstarter.test.config.TestInbox;
import io.github.kevvvvyp.simpletransactionaloutboxstarter.test.testcontainers.UseMysqlDatabase;
import io.github.kevvvvyp.simpletransactionaloutboxstarter.transfer.Message;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@SpringBootTest
@UseMysqlDatabase
@TestInstance(Lifecycle.PER_CLASS)
public class MysqlOutboxServiceIntegrationTest {
	public final TransactionalOutboxService outboxService;
	private final TestInbox inbox;
	private final OutboxConfiguration outboxConfiguration;

	@Autowired
	public MysqlOutboxServiceIntegrationTest( final TransactionalOutboxService outboxService,
			final TestInbox inbox, final OutboxConfiguration outboxConfiguration ) {
		this.outboxService = outboxService;
		this.inbox = inbox;
		this.outboxConfiguration = outboxConfiguration;
		this.outboxService.start();
	}

	@BeforeEach
	public void beforeEach() {
		outboxConfiguration.setBackoff( Duration.ofMillis( 50 ) );
		outboxConfiguration.setLock( Duration.ofSeconds( 5 ) );
		outboxConfiguration.setBatchSize( 3 );
		outboxService.start();
	}

	@AfterEach
	public void afterEach() {
		outboxService.stop();
		inbox.stopSimulatingDeliveryFailure();
		inbox.getReceivedMessages().clear();
	}

	@Test
	@DisplayName("Can we successfully deliver a single message?")
	void canSuccessfullyDeliverAMessage() {
		final Message message = generateRandomMessage();

		final UUID id = outboxService.register( message );

		doUntil( () -> {
			assertThat( inbox.getReceivedMessages() ).contains( message );
			assertThat( id ).isNotNull();
			assertThat( outboxService.isRegistered( id ) ).isFalse();
		} );
	}

	@Test
	@DisplayName("Can we successfully bulk deliver messages?")
	void canSuccessfullyBulkDeliverMessages() {
		final Collection<Message> messages = generateRandomMessages( 42 );

		final Map<UUID, Message> sentById = outboxService.registerAll( messages );

		doUntil( () -> {
			messages.forEach( m -> assertThat( inbox.getReceivedMessages() ).contains( m ) );
			assertThat( sentById ).hasSameSizeAs( messages );
			assertThat(
					sentById.keySet().stream().noneMatch( outboxService::isRegistered ) ).isTrue();
		} );
	}

	@Test
	@DisplayName("Can we schedule messages and are they processed in order?")
	void canWeScheduleMessages() {
		final Instant start = now();

		final int maxSeconds = 10;
		final List<Message> delayedMessages = IntStream.rangeClosed( 0, maxSeconds )
				.sequential()
				.mapToObj( i -> addDelay( generateRandomMessage(), ofSeconds( i ) ) )
				.toList();

		final Map<UUID, Message> sentById = outboxService.registerAll( delayedMessages );

		doUntil( () -> {
			delayedMessages.forEach( m -> assertThat( inbox.getReceivedMessages() ).contains( m ) );

			assertThat( inbox.getReceivedMessages() ).isSortedAccordingTo(
					Comparator.comparing( Message::scheduleAfter ) );

			assertThat( sentById ).hasSameSizeAs( delayedMessages );
			assertThat(
					sentById.keySet().stream().noneMatch( outboxService::isRegistered ) ).isTrue();
		} );

		assertThat( between( start, now() ) ).isCloseTo( Duration.ofSeconds( maxSeconds ),
				Duration.ofSeconds( 1 ) );
	}

	@Test
	@DisplayName("Can we de-duplicate messages over a 5 second period?")
	void canDedupMessages() {
		final List<Message> markerMessages = addDelay( generateRandomMessages( 10 ),
				ofSeconds( 5 ) );
		final List<Message> duplicates = addDelay( generateDuplicateMessages( 10 ),
				ofSeconds( 5 ) );
		final List<Message> allMessages = Stream.concat( duplicates.stream(),
				markerMessages.stream() ).toList();

		final Map<UUID, Message> sentById = outboxService.registerAll( allMessages );

		doUntil( () -> {
			markerMessages.forEach(
					m -> assertThat( inbox.getReceivedMessages() ).containsOnlyOnce( m ) );
			assertThat( inbox.getReceivedMessages() ).containsOnlyOnce( duplicates.get( 0 ) );
			assertThat( sentById ).hasSize( markerMessages.size() + 1 );
			assertThat(
					sentById.keySet().stream().noneMatch( outboxService::isRegistered ) ).isTrue();
		} );
	}

	@Test
	@DisplayName("If we send a large body there should be no size restrictions?")
	void noSizeRestrictionsOnBody() {
		final Message message = generateRandomMessage().toBuilder()
				.body( FAKER.lorem().characters( 20000 ) )
				.build();

		final UUID id = outboxService.register( message );

		doUntil( () -> {
			assertThat( inbox.getReceivedMessages() ).contains( message );
			assertThat( id ).isNotNull();
			assertThat( outboxService.isRegistered( id ) ).isFalse();
		} );
	}

	@Test
	@DisplayName("If we send too large a message it should fail?")
	void messageSizeRestrictionsApply() {
		final Message subjectTooBig = generateRandomMessage().toBuilder()
				.subject( FAKER.lorem().characters( 500 ) )
				.build();

		final Message dedupeKeyTooBig = generateRandomMessage().toBuilder()
				.deduplicationKey( FAKER.lorem().characters( 500 ) )
				.build();

		final Message senderTooBig = generateRandomMessage().toBuilder()
				.sender( FAKER.lorem().characters( 500 ) )
				.build();

		final Message recipientTooBig = generateRandomMessage().toBuilder()
				.recipient( FAKER.lorem().characters( 500 ) )
				.build();

		assertThatThrownBy( () -> outboxService.register( subjectTooBig ) );
		assertThatThrownBy( () -> outboxService.register( dedupeKeyTooBig ) );
		assertThatThrownBy( () -> outboxService.register( senderTooBig ) );
		assertThatThrownBy( () -> outboxService.register( recipientTooBig ) );
	}

	@Test
	@DisplayName("Do we retain data on delivery failure?")
	void noDataLossOnDeliveryFailure() {
		inbox.simulateDeliveryFailure();

		final Collection<Message> messages = generateRandomMessages( 10 );
		final Map<UUID, Message> sentById = outboxService.registerAll( messages );

		doUntil( () -> {
			assertThat( sentById ).hasSameSizeAs( messages );
			assertThat( inbox.getReceivedMessages() ).doesNotContainAnyElementsOf( messages );
		} );

		inbox.stopSimulatingDeliveryFailure();
		doUntil( () -> {
			messages.forEach( m -> assertThat( inbox.getReceivedMessages() ).contains( m ) );
			assertThat(
					sentById.keySet().stream().noneMatch( outboxService::isRegistered ) ).isTrue();
		} );
	}

	@Test
	@DisplayName("Can we stop the delivery of new messages?")
	void canStop() throws InterruptedException {
		outboxService.stop();
		doUntil( () -> assertThat( outboxService.isRunning() ).isFalse() );

		final List<Message> messages = generateDuplicateMessages( 10 );
		final Map<UUID, Message> sentById = outboxService.registerAll( messages );

		// Wait
		log.info( "Waiting..." );
		Thread.sleep( Duration.ofSeconds( 5 ).toMillis() );

		// Verify messages are sitting undelivered
		doUntil( () -> assertThat(
				sentById.keySet().stream().allMatch( outboxService::isRegistered ) ).isTrue() );
		assertThat( inbox.getReceivedMessages() ).doesNotContainAnyElementsOf( sentById.values() );
	}

	@Test
	@DisplayName("If we start the service are existing messages sent?")
	void canStartAgain() {
		outboxService.stop();
		doUntil( () -> assertThat( outboxService.isRunning() ).isFalse() );

		final List<Message> messages = generateDuplicateMessages( 10 );
		final Map<UUID, Message> sentById = outboxService.registerAll( messages );

		outboxService.start();

		// Wait
		doUntil( () -> assertThat( inbox.getReceivedMessages() ).containsAll( sentById.values() ) );
	}

	@Test
	@DisplayName("Are locked messages successfully reclaimed?")
	void canReclaimLockedMessages() throws InterruptedException {
		// Config
		final Duration temporaryLockDuration = ofSeconds( 20 );
		final Duration tolerance = ofMillis( 1500 );
		outboxConfiguration.setBackoff( Duration.ofMillis( 50 ) );
		outboxConfiguration.setLock( temporaryLockDuration );
		inbox.simulateDeliveryFailure();

		final Instant start = now();
		final List<Message> messages = generateRandomMessages( 10 );
		final Map<UUID, Message> sentById = outboxService.registerAll( messages );

		// Wait for a bit for delivery attempt
		Thread.sleep( Duration.ofSeconds( 5 ).toMillis() );

		// Allow successful delivery
		outboxService.stop();
		doUntil( () -> assertThat( outboxService.isRunning() ).isFalse() );
		inbox.stopSimulatingDeliveryFailure();
		outboxService.start();

		// Wait for existing messages to be reclaimed after 20s & successful delivered
		doUntil( () -> assertThat( inbox.getReceivedMessages() ).containsAll( sentById.values() ) );
		assertThat( between( start, now() ) ).isGreaterThanOrEqualTo(
				temporaryLockDuration.minus( tolerance ) );
	}

	@Test
	@DisplayName("If no delivery strategy has been defined for that message type then do we throw?")
	void weCantRegisterMessagesIfThereIsNoDeliveryStrategy() {
		final Message message = generateRandomMessage().toBuilder().type( "Ignore" ).build();
		assertThatThrownBy( () -> outboxService.register( message ) );
		assertThatThrownBy( () -> outboxService.registerAll( Set.of( message ) ) );
	}
}
