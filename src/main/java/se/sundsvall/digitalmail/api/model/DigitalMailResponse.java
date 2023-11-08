package se.sundsvall.digitalmail.api.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(setterPrefix = "with")
@NoArgsConstructor
@AllArgsConstructor
public class DigitalMailResponse {
    
    private DeliveryStatus deliveryStatus;
}
