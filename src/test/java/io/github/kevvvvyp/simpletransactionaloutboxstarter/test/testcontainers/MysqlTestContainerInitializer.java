package io.github.kevvvvyp.simpletransactionaloutboxstarter.test.testcontainers;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.lifecycle.Startables;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@SuppressWarnings("resource")
public class MysqlTestContainerInitializer implements
		ApplicationContextInitializer<ConfigurableApplicationContext> {

	@Container
	static JdbcDatabaseContainer<?> DATABASE = new MySQLContainer<>(
			"mysql:8.1.0" ).withDatabaseName( "sample" )
			.withUsername( "root" )
			.withPassword( "root" )
			.withInitScript( "mysql/init.sql" );

	static {
		Startables.deepStart( DATABASE ).join();
	}

	@Override
	public void initialize( ConfigurableApplicationContext ctx ) {
		TestPropertyValues.of( "spring.datasource.url=" + DATABASE.getJdbcUrl(),
						"spring.datasource.username=" + DATABASE.getUsername(),
						"spring.datasource.password=" + DATABASE.getPassword() )
				.applyTo( ctx.getEnvironment() );
	}

}
