package io.github.kevvvvyp.simpletransactionaloutboxstarter.domain.repository.ro;

import java.time.Instant;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import io.github.kevvvvyp.simpletransactionaloutboxstarter.domain.model.OutboxEntity;

/**
 * Read only
 */
@Repository
public interface OutboxRoRepository extends JpaRepository<OutboxEntity, UUID> {

	/**
	 * True if there are messages ready to be sent.
	 *
	 * @param now
	 * @param reclaimDate
	 * @param messageTypes
	 * @return
	 */
	@Query(value = """
			SELECT (COUNT(o) > 0) FROM OutboxEntity o
			WHERE (
			    (o.scheduledAfter < :now AND o.lockedBy IS NULL AND o.type IN (:messageTypes))
			    OR
			    (o.scheduledAfter < :now AND o.updatedAt < :reclaimDate AND o.lockedBy IS NOT NULL AND o.type IN (:messageTypes))
			)
			""")
	boolean availableMessages( @Param("now") final Instant now,
			@Param("reclaimDate") final Instant reclaimDate,
			@Param("messageTypes") final Collection<String> messageTypes );

	/**
	 * Look up by deduplicationKey
	 *
	 * @param deduplicationKey
	 * @return
	 * @implNote The deduplication_key column utilises a unique index.
	 */
	Optional<OutboxEntity> findByDeduplicationKey( final String deduplicationKey );

	/**
	 * Find all the entities that match the collection of keys.
	 *
	 * @param deduplicationKey
	 * @return
	 * @implNote The deduplication_key column utilises a unique index.
	 */
	Collection<OutboxEntity> findAllByDeduplicationKeyIn(
			final Collection<String> deduplicationKey );
}