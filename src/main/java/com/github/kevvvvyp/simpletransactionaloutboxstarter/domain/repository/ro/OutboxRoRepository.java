package io.github.kevvvvyp.simpletransactionaloutboxstarter.domain.repository.ro;

import java.time.Instant;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import io.github.kevvvvyp.simpletransactionaloutboxstarter.domain.model.OutboxEntity;

@Repository
public interface OutboxRoRepository extends JpaRepository<OutboxEntity, UUID> {

	/**
	 * True if there are messages ready to be sent.
	 */
	@Query(value = """
			SELECT (COUNT(o) > 0) FROM OutboxEntity o
			WHERE (
			    (o.scheduledAfter < :now AND o.lockedBy IS NULL AND o.type IN (:messageTypes))
			    OR
			    (o.scheduledAfter < :now AND o.updatedAt < :reclaimDate AND o.lockedBy IS NOT NULL AND o.type IN (:messageTypes))
			)
			""")
	boolean availableMessages( final Instant now, final Instant reclaimDate,
			final Collection<String> messageTypes );

	/**
	 * Look up by deduplicationKey
	 *
	 * @implNote The deduplication_key column utilises a unique index.
	 */
	Optional<OutboxEntity> findByDeduplicationKey( final String deduplicationKey );

	/**
	 * Find all the entities that match the collection of keys.
	 *
	 * @implNote The deduplication_key column utilises a unique index.
	 */
	Collection<OutboxEntity> findAllByDeduplicationKeyIn(
			final Collection<String> deduplicationKey );
}