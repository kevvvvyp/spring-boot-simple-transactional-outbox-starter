package io.github.kevvvvyp.simpletransactionaloutboxstarter.domain.repository;

/**
 * Marker interface for autoconfiguration
 */public interface OutboxRepositoryMarker {
	String RO_REPO_MARKER = "io.github.kevvvvyp.simpletransactionaloutboxstarter.domain.repository.ro";
	String RW_REPO_MARKER = "io.github.kevvvvyp.simpletransactionaloutboxstarter.domain.repository.rw";
}
