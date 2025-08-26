package se.sundsvall.digitalmail.integration.skatteverket;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(setterPrefix = "with")
public class MailboxDto {

	private String recipientId;
	private String serviceAddress;
	private String serviceName;
	private boolean validMailbox;
}
