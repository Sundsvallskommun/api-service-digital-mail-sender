package se.sundsvall.digitalmail.integration.minameddelanden;

/**
 * Simple record for sending recipientId, serviceAddress and serviceName back and forth
 *
 * @param recipientId
 * @param serviceAddress
 */
public record MailboxDto(String recipientId, String serviceAddress, String serviceName) {
}
