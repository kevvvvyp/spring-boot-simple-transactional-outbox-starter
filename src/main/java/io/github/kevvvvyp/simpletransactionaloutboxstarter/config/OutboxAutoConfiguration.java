package io.github.kevvvvyp.simpletransactionaloutboxstarter.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import io.github.kevvvvyp.simpletransactionaloutboxstarter.domain.model.OutboxEntityMarker;
import io.github.kevvvvyp.simpletransactionaloutboxstarter.domain.repository.OutboxRepositoryMarker;
import io.github.kevvvvyp.simpletransactionaloutboxstarter.service.SimpleTransactionalOutboxService;
import io.github.kevvvvyp.simpletransactionaloutboxstarter.service.delivery.OutboxDao;
import io.github.kevvvvyp.simpletransactionaloutboxstarter.service.delivery.OutboxPollingDeliveryService;
import io.github.kevvvvyp.simpletransactionaloutboxstarter.service.registration.OutboxRegistrationServiceImpl;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@AutoConfiguration
@Import({ SimpleTransactionalOutboxService.class,
		  OutboxPollingDeliveryService.class,
		  OutboxDao.class,
		  OutboxConfiguration.class,
		  OutboxRegistrationServiceImpl.class })
@EnableJpaRepositories(basePackages = { OutboxRepositoryMarker.RO_REPO_MARKER,
										OutboxRepositoryMarker.RW_REPO_MARKER })
@EntityScan(basePackages = { OutboxEntityMarker.ENTITY_MARKER })
public class OutboxAutoConfiguration {
	public OutboxAutoConfiguration() {
		log.info( "Loaded autoconfiguration" );
	}
}
