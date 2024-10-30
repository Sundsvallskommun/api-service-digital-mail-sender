package se.sundsvall.digitalmail.api.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder(setterPrefix = "with")
@Setter
@Getter
public class DeliveryStatus {

	private boolean delivered;
	private String partyId;
	private String transactionId;
}
