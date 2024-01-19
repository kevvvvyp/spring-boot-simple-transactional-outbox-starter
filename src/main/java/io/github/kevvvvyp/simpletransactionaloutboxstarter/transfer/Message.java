package io.github.kevvvvyp.simpletransactionaloutboxstarter.transfer;

import java.time.Instant;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder(toBuilder = true)
public record Message(@NotBlank String type, String sender, String recipient,
					  @NotBlank String deduplicationKey, Instant scheduleAfter, //Nullable
					  String subject, //Nullable,
					  String body //Nullable
) {}
