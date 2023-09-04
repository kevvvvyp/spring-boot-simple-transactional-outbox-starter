package com.github.kevvvvyp.simpletransactionaloutboxstarter.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

@Data
@ConfigurationProperties("simple.outbox")
public class OutboxConfiguration {
	boolean enabled;
	int batchSize;
	int pollingPool;
	double jitter;
	Duration lock;
	Duration backoff;
}
