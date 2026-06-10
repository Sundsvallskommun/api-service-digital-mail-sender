package se.sundsvall.digitalmail.api.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(setterPrefix = "with")
public class DeliveryStatus {

	private boolean delivered;
	private String partyId;
	private String transactionId;
}
