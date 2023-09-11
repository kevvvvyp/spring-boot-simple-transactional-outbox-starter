# Spring-boot-simple-transactional-outbox-starter
[![CircleCI](https://dl.circleci.com/status-badge/img/gh/kevvvvyp/spring-boot-simple-transactional-outbox-starter/tree/main.svg?style=svg&circle-token=fb0235ac2ad18482ddebc4d504a0e23cca9e1891)](https://dl.circleci.com/status-badge/redirect/gh/kevvvvyp/spring-boot-simple-transactional-outbox-starter/tree/main)
[![Generic badge](https://img.shields.io/badge/STATUS-EXPERIMENTAL-orange.svg)](https://shields.io/)

A convenient Spring Boot Starter for publishing messages via the [Transactional Outbox Pattern](https://microservices.io/patterns/data/transactional-outbox.html). This library guarantees delivery but **does not** guarantee that messages are only delivered once.

## Getting Started
* Add the starter dependency to your `build.gradle` or `pom.xml` file
   1. TODO upload to MVN central
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
		return "Example";
	}
```
* Finally, simply start the OutboxService from within your application e.g.
```
@SpringBootApplication
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
				.type( "Simple" )
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
- `simple.outbox.jitter` - Apply Jitter to the database polling, this is useful so that all threads don't attempt to consume the same set of messages all at once, it splays the load on the db read instance.
- `simple.outbox.lock` - The duration to 'lock' messages for. Once the lock duration is exceeded another process is free to then lock those messages if they remain undelivered.
- `simple.outbox.backoff` - The duration to wait between polling the database for undelivered messages. Can be combined with Jitter to splay database load.