package io.github.kevvvvyp.simpletransactionaloutboxstarter.domain.repository.rw;

import static org.hibernate.cfg.AvailableSettings.JAKARTA_LOCK_TIMEOUT;

import java.time.Instant;
import java.util.Collection;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import io.github.kevvvvyp.simpletransactionaloutboxstarter.domain.model.OutboxEntity;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;

@Repository
public interface OutboxRwRepository extends JpaRepository<OutboxEntity, UUID> {

	/**
	 * Indicates that we should utilise the databases `SKIP LOCKED` feature if possible
	 */
	String SKIP_LOCKED = "-2";

	/**
	 * Find available messages that are ready to be sent. Here we "lock" them to then be updated.
	 *
	 * @param now
	 * @param reclaimDate
	 * @param pageable
	 * @param messageTypes
	 * @return
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@QueryHints({ @QueryHint(name = JAKARTA_LOCK_TIMEOUT, value = SKIP_LOCKED) })
	@Query(value = """
			SELECT o FROM OutboxEntity o
			WHERE (
			    (o.scheduledAfter < :now AND o.lockedBy IS NULL AND o.type IN (:messageTypes))
			    OR
			    (o.scheduledAfter < :now AND o.updatedAt < :reclaimDate AND o.lockedBy IS NOT NULL AND o.type IN (:messageTypes))
			)
			ORDER BY o.scheduledAfter
			""")
	Page<OutboxEntity> findAvailable( @Param("now") final Instant now,
			@Param("reclaimDate") final Instant reclaimDate,
			@Param("pageable") final Pageable pageable,
			@Param("messageTypes") final Collection<String> messageTypes );

	/**
	 * Lock messages for a period of time.
	 *
	 * @param claimerId
	 * @param ids
	 * @param now
	 * @return
	 * @implNote The Application Configuration when messages can be reclaimed.
	 */
	@Modifying
	@Query(value = """
			UPDATE OutboxEntity AS outbox
			SET outbox.lockedBy = :claimerId, outbox.updatedAt = :now
			WHERE outbox.id IN (:ids)
			""")
	int lock( @Param("claimerId") final UUID claimerId, @Param("ids") final Collection<UUID> ids,
			@Param("now") final Instant now );
}