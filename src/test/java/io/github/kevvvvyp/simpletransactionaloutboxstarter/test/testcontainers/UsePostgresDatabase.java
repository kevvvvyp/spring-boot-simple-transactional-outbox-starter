package io.github.kevvvvyp.simpletransactionaloutboxstarter.test.testcontainers;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.test.context.ContextConfiguration;

@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@ContextConfiguration(initializers = PostgresTestContainerInitializer.class)
public @interface UsePostgresDatabase {}