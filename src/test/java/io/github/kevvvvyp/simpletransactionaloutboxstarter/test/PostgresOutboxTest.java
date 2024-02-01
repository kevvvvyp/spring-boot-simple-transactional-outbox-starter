package io.github.kevvvvyp.simpletransactionaloutboxstarter.test;

import org.springframework.beans.factory.annotation.Autowired;

import io.github.kevvvvyp.simpletransactionaloutboxstarter.config.OutboxConfiguration;
import io.github.kevvvvyp.simpletransactionaloutboxstarter.service.TransactionalOutboxService;
import io.github.kevvvvyp.simpletransactionaloutboxstarter.test.config.TestInbox;
import io.github.kevvvvyp.simpletransactionaloutboxstarter.test.testcontainers.UsePostgresDatabase;

@UsePostgresDatabase
public class PostgresOutboxTest extends OutboxServiceIntegrationTest {
	@Autowired
	public PostgresOutboxTest( final TransactionalOutboxService outboxService,
			final TestInbox inbox, final OutboxConfiguration outboxConfiguration ) {
		super( outboxService, inbox, outboxConfiguration );
	}
}
