package io.github.kevvvvyp.simpletransactionaloutboxstarter.transfer;

import java.time.Instant;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

/**
 * Message DTO that is used by senders to add data to the outbox and  also recipients.
 *
 * @param type             The type of message, this must marry up to an OutboxDeliveryStrategy. [Mandatory]
 * @param sender           Metadata
 * @param recipient        Metadata
 * @param deduplicationKey Key used to dedup messages if they are delayed within the outbox.
 *                         If you are not using this functionality then it would be recommended to
 *                         use a random UUID [Mandatory].
 * @param scheduleAfter    A date to send the message after
 * @param subject          Metadata
 * @param body             Body of message.
 */
@Builder(toBuilder = true)
public record Message(@NotBlank String type, String sender, String recipient,
					  @NotBlank String deduplicationKey, Instant scheduleAfter, //Nullable
					  String subject, //Nullable,
					  String body //Nullable
) {}
