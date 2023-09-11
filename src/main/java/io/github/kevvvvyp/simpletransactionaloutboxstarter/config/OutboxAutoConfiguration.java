package io.github.kevvvvyp.simpletransactionaloutboxstarter.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Import;

import io.github.kevvvvyp.simpletransactionaloutboxstarter.service.SimpleTransactionalOutboxService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@AutoConfiguration
@Import({ SimpleTransactionalOutboxService.class, OutboxConfiguration.class })
public class OutboxAutoConfiguration {
	public OutboxAutoConfiguration() {
		log.info( "Loaded autoconfiguration" );
	}
}
