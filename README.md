# Spring-boot-simple-transactional-outbox-starter
  <!-- Java version -->
  <a href="https://img.shields.io/badge/Java-21-blue.svg?logo=Java">
    <img src="https://img.shields.io/badge/Java-21-blue.svg?logo=Java"
      alt="Java version" />
  </a>
  <!-- Spring Boot -->
  <a href="https://github.com/spring-projects/spring-boot/releases">
    <img src="https://img.shields.io/badge/SpringBoot-3.2.x-blue.svg?logo=Spring"
      alt="Spring Boot" />
  </a>

A convenient Spring Boot Starter for publishing messages via the [Transactional Outbox Pattern](https://microservices.io/patterns/data/transactional-outbox.html). This library guarantees delivery but **does not** guarantee that messages are only delivered once.

## Getting Started
* Add the starter dependency to your `build.gradle` or `pom.xml` file - see [here](https://mvnrepository.com/artifact/io.github.kevvvvyp/spring-boot-simple-transactional-outbox-starter/).
* Add the Outbox table to your database e.g. [db_scripts/mysql/init.sql](db_scripts/mysql/init.sql). (Please see the db_scripts folder for other database table definitions).
* Add the following to your `application.yml` file within your project. Please note the following are default values.
```
simple:
  outbox:
    enabled: true
    batchSize: 1
    pollingPool: 1
    jitter: 0.5
    lock: 60s
    backoff: 1s

spring:
  datasource:
    url: 'SET ME'
    username: 'SET ME'
    password: 'SET ME'
  jpa:
    open-in-view: false
    properties:
      hibernate:
        globally_quoted_identifiers: true
        type:
          preferred_uuid_jdbc_type: CHAR
        jdbc:
          batch_size: 20
        show_sql: false

logging:
  level:
    io.github.kevvvvyp.simpletransactionaloutboxstarter: INFO

```
* This library needs to know where to delivery messages to. Define a singleton within your application...
```
@Component
public class TestInbox implements OutboxDeliveryStrategy {
	@Override
	public void send( final Collection<Message> messages ) {
		// Do something with the message. 
		// If an exception is raised then the message will be reattempted.
	}

	@Override
	public String messageType() {
		return "MyMessageType";
	}
```
* Finally, simply start the OutboxService from within your application e.g.
```
@SpringBootApplication
@EnableJpaRepositories(basePackages = { YourRepositoryMarker.REPO_MARKER })
@EntityScan(basePackages = YourEntityMarker.class)
public class MyApplication {
	public static void main( String[] args ) {
		final ConfigurableApplicationContext ctx = SpringApplication.run(
				MyApplication.class, args );

		final TransactionalOutboxService outboxService = ctx.getBean(
				TransactionalOutboxService.class );
		
		// Start the service
		outboxService.start();
		
		// Schedule a message to be sent in an hour's time
		Message message = Message.builder()
 		    .type( "MyMessageType" )
 		    .sender( "sender@email.com" )
 		    .recipient( "recipient@email.com" )
 		    .subject( "Important" )
 		    .deduplicationKey( "hhdaslkjklajwe3k4 )
 		    .scheduleAfter( Instant.now().plus( 1, ChronoUnit.HOURS ) )
 		    .body( "example" )
 		    .build();
 		    
		outboxService.register( message );
	}
}
```

## Database compatability 
This project utilises Spring Data JPA (& Hibernate) so theoretically this library has the potential to support multiple different databases however only the following have been tested. 

| Database | Supported Versions |
|----------|--------------------|
| MySQL    | 8+                 |
| Postgres | 15+                |

If you find that this library works on another version please reach out or raise a PR to update the above table.

## Configurable Properties
- `simple.outbox.enabled` - True if you wish the library to deliver messages stored in the database, false if you wish the message to remain undelivered (i.e. no database reads/polling).
- `simple.outbox.batchSize` - The number of messages you wish the library to consume at once & attempt to deliver.
- `simple.outbox.pollingPool` - The size of the polling thread pool attempting to find undeliverable messages, must be at least 1.
- `simple.outbox.jitter` - Apply Jitter to the database polling, this is useful so that all threads don't attempt to consume the same set of messages all at once, it splays the load on the db read instance. If set to 0 no jitter is applied.
- `simple.outbox.lock` - The duration to 'lock' messages for. Once the lock duration is exceeded another process is free to then lock those messages if they remain undelivered.
- `simple.outbox.idleBackoff` - The duration to wait between polling the database for undelivered messages, it is applied when there are no messages to process. Can be combined with Jitter to splay database load. Setting this to 0 results in no backoff being applied.
- `simple.outbox.processingBackoff` - The duration to wait between polling the database for undelivered messages, it is applied when there are messages to be processed. Can be combined with Jitter to splay database load. Setting this to 0 results in no backoff being applied.

## Build & CI
This project utilises GitHub actions for automation.
When a new PR is raised, `SNAPSHOT` build are published [here](https://s01.oss.sonatype.org/#view-repositories;snapshots~browsestorage~io/github/kevvvvyp/spring-boot-simple-transactional-outbox-starter) (n.b. the snapshot is overridden after each push per PR)
