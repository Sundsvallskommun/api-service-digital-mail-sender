package se.sundsvall.digitalmail.api.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Builder(setterPrefix = "with")
@Setter
@Getter
public class DeliveryStatus {

	private boolean delivered;
	private String partyId;
	private String transactionId;
}
