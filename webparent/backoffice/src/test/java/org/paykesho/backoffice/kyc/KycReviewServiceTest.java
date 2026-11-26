package org.paykesho.backoffice.kyc;

import org.commons.entity.client.Client;
import org.commons.entity.client.EmployeeRecord;
import org.commons.entity.organization.Organization;
import org.commons.entity.kyc.ClientKycApplication;
import org.commons.entity.kyc.KycDocument;
import org.commons.enums.KycDocumentType;
import org.commons.enums.KycStatus;
import org.commons.utils.PaykeshoError;
import org.paykesho.backoffice.client.repository.ClientRepository;
import org.paykesho.backoffice.notification.general.NotificationClient;
import org.paykesho.backoffice.organizations.repository.EmployeeRecordRepository;
import org.paykesho.backoffice.utils.PaykeshoReferenceService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KycReviewServiceTest {

    @Mock KycApplicationRepository applications;
    @Mock KycDocumentRepository documents;
    @Mock ClientRepository clients;
    @Mock EmployeeRecordRepository employeeRecords;
    @Mock PaykeshoReferenceService references;
    @Mock NotificationClient notificationClient;

    @InjectMocks KycReviewService service;

    private ClientKycApplication application() {
        Organization employer = new Organization();
        Client client = new Client();
        client.setId(7);
        client.setName("Mary Chanda Banda");
        client.setNrc("222222/22/2");
        client.setTpin("2000000002");
        client.setManNumber("1002");
        client.setOrganization(employer);
        ClientKycApplication app = new ClientKycApplication();
        app.setId(42L);
        app.setClient(client);
        app.setFirstName("Mary");
        app.setMiddleName("Chanda");
        app.setLastName("Banda");
        app.setStatus(KycStatus.SUBMITTED);
        return app;
    }

    private List<KycDocument> requiredDocuments() {
        return List.of(document(KycDocumentType.NRC_FRONT), document(KycDocumentType.NRC_BACK), document(KycDocumentType.PAYSLIP));
    }

    private KycDocument document(KycDocumentType type) {
        KycDocument d = new KycDocument();
        d.setDocumentType(type);
        d.setFileUploadId(100L);
        return d;
    }

    private EmployeeRecord matchingStaff() {
        EmployeeRecord staff = new EmployeeRecord();
        staff.setName("Mary Banda");
        staff.setNrc("222222/22/2");
        staff.setTpin("2000000002");
        staff.setManNumber("1002");
        return staff;
    }

    @Test
    void approvalIsBlockedWhenRequiredDocumentsAreMissing() {
        ClientKycApplication app = application();
        when(applications.findWithClientForUpdate(42L)).thenReturn(Optional.of(app));
        when(documents.findByKycApplicationIdOrderByIdAsc(42L)).thenReturn(List.of(document(KycDocumentType.NRC_FRONT)));

        assertThrows(PaykeshoError.class, () -> service.approve(42L, 9, null));
        verify(applications, never()).save(any());
        verify(clients, never()).save(any());
    }

    @Test
    void approvalIsBlockedWhenEmployerStaffRecordDoesNotMatch() {
        ClientKycApplication app = application();
        when(applications.findWithClientForUpdate(42L)).thenReturn(Optional.of(app));
        when(documents.findByKycApplicationIdOrderByIdAsc(42L)).thenReturn(requiredDocuments());
        EmployeeRecord staff = matchingStaff();
        staff.setTpin("WRONG");
        when(employeeRecords.findByNrcAndOrganization(app.getClient().getNrc(), app.getClient().getOrganization())).thenReturn(staff);

        assertThrows(PaykeshoError.class, () -> service.approve(42L, 9, null));
        verify(applications, never()).save(any());
        verify(clients, never()).save(any());
    }

    @Test
    void approvalActivatesOnlyWhenDocumentsAndStaffRecordMatch() throws Exception {
        ClientKycApplication app = application();
        when(applications.findWithClientForUpdate(42L)).thenReturn(Optional.of(app));
        when(documents.findByKycApplicationIdOrderByIdAsc(42L)).thenReturn(requiredDocuments());
        when(employeeRecords.findByNrcAndOrganization(app.getClient().getNrc(), app.getClient().getOrganization())).thenReturn(matchingStaff());
        when(references.generateCode("ITEM_REQ_CLT")).thenReturn("CLT-0007");

        ClientKycApplication approved = service.approve(42L, 9, "Checked");

        assertEquals(KycStatus.APPROVED, approved.getStatus());
        assertTrue(app.getClient().isEnabled());
        assertEquals("CLT-0007", app.getClient().getCode());
        verify(applications).save(app);
        verify(clients).save(app.getClient());
    }
}
