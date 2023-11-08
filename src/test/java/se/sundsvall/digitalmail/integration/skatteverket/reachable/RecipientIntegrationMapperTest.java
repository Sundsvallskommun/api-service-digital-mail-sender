package se.sundsvall.digitalmail.integration.skatteverket.reachable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static se.sundsvall.digitalmail.integration.skatteverket.reachable.RecipientIntegrationMapper.SENDER_ORG_NR;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import se.sundsvall.digitalmail.integration.skatteverket.SkatteverketProperties;

import se.gov.minameddelanden.schema.recipient.AccountStatus;
import se.gov.minameddelanden.schema.recipient.ReachabilityStatus;
import se.gov.minameddelanden.schema.recipient.ServiceSupplier;
import se.gov.minameddelanden.schema.recipient.v3.IsReachableResponse;

@ExtendWith(MockitoExtension.class)
class RecipientIntegrationMapperTest {

    @Mock
    private SkatteverketProperties mockSkatteverketProperties;

    @InjectMocks
    private RecipientIntegrationMapper mapper;

    @Test
    void testCreateIsRegistered() {
        var personalNumber = "197001011234";

        var isReachableRequest = mapper.createIsReachableRequest(List.of(personalNumber));

        assertThat(isReachableRequest.getSenderOrgNr()).isEqualTo(SENDER_ORG_NR);
        assertThat(isReachableRequest.getRecipientId().get(0)).isEqualTo(personalNumber);
    }
    
    @Test
    void testGetMailboxSettings_shouldReturnRecipientId_WhenPresent() {
        when(mockSkatteverketProperties.supportedSuppliers()).thenReturn(List.of("Kivra"));

        var response = createIsReachableResponse(false, true, true);

        var mailboxSettings = mapper.getMailboxSettings(response);

        assertThat(mailboxSettings).hasSize(1);
        assertThat(mailboxSettings.get(0).serviceAddress()).isEqualTo("http://somewhere.com");
        assertThat(mailboxSettings.get(0).recipientId()).isEqualTo("recipientId");
    }
    
    @Test
    void testGetMailboxSettings_shouldReturnEmpty_WhenPending() {
        var response = createIsReachableResponse(true, true, false);

        var mailboxSettings = mapper.getMailboxSettings(response);

        assertThat(mailboxSettings).isEmpty();
    }
    
    @Test
    void testGetMailboxSettings_shouldReturnEmpty_WhenNoServiceSupplier() {
        var response = createIsReachableResponse(false, false, false);

        var mailboxSettings = mapper.getMailboxSettings(response);

        assertThat(mailboxSettings).isEmpty();
    }
    
    @Test
    void testGetMailboxSettings_shouldReturnEmpty_whenSenderNotAccepted() {
        var response = createIsReachableResponse(false, true, false);

        var mailboxSettings = mapper.getMailboxSettings(response);

        assertThat(mailboxSettings).isEmpty();
    }
    
    @Test
    void testFindMatchingSupplier() {
        when(mockSkatteverketProperties.supportedSuppliers()).thenReturn(List.of("supplier1", "supplier2", "supplier3"));

        assertThat(mapper.isSupportedSupplier("supplier1")).isTrue();
        assertThat(mapper.isSupportedSupplier("supplier2")).isTrue();
        assertThat(mapper.isSupportedSupplier("supplier3")).isTrue();
        assertThat(mapper.isSupportedSupplier("unknownSupplier")).isFalse();
    }
    
    @Test
    void testFindShortSupplierName_shouldMapAndReturnShortName() {
        when(mockSkatteverketProperties.supportedSuppliers()).thenReturn(List.of("Supplier1", "SuPPliEr2"));

        assertThat(mapper.getShortSupplierName("Supplier1")).isEqualTo("supplier1");
        assertThat(mapper.getShortSupplierName("SuPPliEr2")).isEqualTo("supplier2");
    }
    
    /**
     *
     * @param pending if pending, the mailbox has not yet been created and should be interpreted as the recipient not having a digital mailbox.
     * @param shouldHaveServiceSupplier No serviceSupplier indicates that the recipient doesn't have a digital mailbox.
     * @param isAccepted is the sender accepted by the recipient
     * @return
     */
    private IsReachableResponse createIsReachableResponse(final boolean pending,
            final boolean shouldHaveServiceSupplier, final boolean isAccepted) {

        final var accountStatus = new AccountStatus();
        accountStatus.setRecipientId("recipientId");
        accountStatus.setPending(pending);

        if (shouldHaveServiceSupplier) {
            var serviceSupplier = new ServiceSupplier();
            serviceSupplier.setId("165568402266");
            serviceSupplier.setName("Kivra");
            serviceSupplier.setServiceAdress("http://somewhere.com");

            accountStatus.setServiceSupplier(serviceSupplier);
        }

        final var reachabilityStatus = new ReachabilityStatus();
        reachabilityStatus.setSenderAccepted(isAccepted);
        reachabilityStatus.setAccountStatus(accountStatus);

        final var response = new IsReachableResponse();
        response.getReturn().add(reachabilityStatus);

        return response;
    }
}
