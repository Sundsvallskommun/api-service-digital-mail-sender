package se.sundsvall.digitalmail.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PACKAGE)
@Builder(setterPrefix = "with")
@Schema(description = "Response model for a mailbox")
public class Mailbox {

	@Schema(description = "partyId for the legal Id the mailbox belongs to")
	private String partyId;

	@Schema(description = "Name of the mailbox, e.g. Kivra")
	private String supplier;

	@Schema(description = "If it's possible to send messages to this mailbox")
	private boolean reachable;
}
