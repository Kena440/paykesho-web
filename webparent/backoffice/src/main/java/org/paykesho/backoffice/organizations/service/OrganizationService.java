package org.paykesho.backoffice.organizations.service;

import lombok.extern.slf4j.Slf4j;
import org.commons.entity.client.Client;
import org.commons.entity.client.EmployeeRecord;
import org.commons.entity.organization.Organization;
import org.commons.enums.OrganizationType;
import org.commons.utils.PaykeshoError;
import org.paykesho.backoffice.client.repository.ClientRepository;
import org.paykesho.backoffice.files.dto.FileAccessResponse;
import org.paykesho.backoffice.files.service.FileUploadService;
import org.paykesho.backoffice.organizations.dto.GPSDataItem;
import org.paykesho.backoffice.organizations.repository.EmployeeRecordRepository;
import org.paykesho.backoffice.organizations.repository.OrganizationRepository;
import org.paykesho.backoffice.paging.PagingAndSortingHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
@Transactional
public class OrganizationService {

    public static final int MEMBERS_PER_PAGE = 20;
    protected DateFormat dateFormat;
    @Autowired
    private OrganizationRepository organizationRepository;
    @Autowired
    private EmployeeRecordRepository employeeRecordRepository;
    @Autowired
    private ClientRepository clientRepository;
    // @Autowired
    // private SettingService settingService; // Unused field
    @Autowired
    private FileUploadService fileUploadService;

    public List<Organization> listAll() {

        List<Organization> organizations = (List<Organization>) organizationRepository.findAll(Sort.by("name").ascending());

        for (Organization organization : organizations) {
            if (organization.getPhotoPath() != null && organization.getRemoteFileId() != null) {
                FileAccessResponse fileAccessResponse = fileUploadService.getFileAccessDetails(organization.getRemoteFileId());
                if (fileAccessResponse != null) {
                    organization.setPhotoPath(fileAccessResponse.getFileUrl());
                }
            }
        }

        return organizations;
    }

    public void listByPage(int pageNum, PagingAndSortingHelper helper) throws PaykeshoError {
        helper.listEntities(pageNum, MEMBERS_PER_PAGE, organizationRepository);
    }

    @Transactional(rollbackOn = {Exception.class})
    public Organization save(Organization organization) {
        if (organization == null) {
            return null;
        }

        return organizationRepository.save(organization);
    }

    public boolean isEmailUnique(Integer id, String email) {
        if (email == null) {
            return false;
        }
        Organization institution = organizationRepository.getOrganizationByEmail(email);

        if (institution == null) return true;
        boolean isCreatingNew = (id == null);

        if (isCreatingNew) {
            if (institution != null) return false;
        } else {
            if (institution.getId() != id) {
                return false;
            }
        }

        return true;
    }

    public boolean isNameUnique(Integer id, String email) {
        if (email == null) {
            return false;
        }
        Organization institution = organizationRepository.getOrganizationByName(email);

        if (institution == null) return true;
        boolean isCreatingNew = (id == null);

        if (isCreatingNew) {
            if (institution != null) return false;
        } else {
            if (institution.getId() != id) {
                return false;
            }
        }

        return true;
    }

    public Organization getOrganizationById(Integer id) throws PaykeshoError {
        if (id == null) {
            throw new PaykeshoError("Null parameter id");
        }

        try {
            Organization organization = organizationRepository.findById(id).get();
            
            // Process photoPath the same way as in listAll()
            if (organization.getPhotoPath() != null && organization.getRemoteFileId() != null) {
                FileAccessResponse fileAccessResponse = fileUploadService.getFileAccessDetails(organization.getRemoteFileId());
                if (fileAccessResponse != null) {
                    organization.setPhotoPath(fileAccessResponse.getFileUrl());
                }
            }
            
            return organization;
        } catch (NoSuchElementException exception) {
            throw new PaykeshoError("Could not find any member");
        }
    }

    @Transactional(rollbackOn = {Exception.class})
    public void delete(Integer id) throws PaykeshoError {
        if (id == null) {
            throw new PaykeshoError("Null parameter id");
        }

        Long countById = organizationRepository.countById(id);
        if (countById == null || countById == 0) {
            throw new PaykeshoError("Could not find any member");
        }

        Organization memberInstitution = getOrganizationById(id);
        if (memberInstitution.getId() == null) throw new PaykeshoError("Could not find any facility");
        memberInstitution.setDeleted(true);
        save(memberInstitution);
        // userRepository.deleteById(id);
    }

    @Transactional(rollbackOn = {Exception.class})
    public void updateOrganizationStatus(Integer id, boolean enabled) throws PaykeshoError {
        if (id == null) {
            throw new PaykeshoError("Null parameter id");
        }

        Long countById = organizationRepository.countById(id);
        if (countById == null || countById == 0) {
            throw new PaykeshoError("Could not find any member");
        }

        organizationRepository.updateEnableStatus(id, enabled);
    }

    // ============================================================================
    // UUID-based methods for secure URL access
    // ============================================================================

    /**
     * Get organization by UUID (for public/URL access)
     * 
     * @param uuid The organization's UUID
     * @return Organization entity
     * @throws PaykeshoError if UUID is invalid or organization not found
     */
    public Organization getOrganizationByUuid(String uuid) throws PaykeshoError {
        if (uuid == null || uuid.trim().isEmpty()) {
            log.error("getOrganizationByUuid called with null or empty UUID");
            throw new PaykeshoError("Invalid UUID parameter");
        }

        try {
            UUID.fromString(uuid);
        } catch (IllegalArgumentException e) {
            log.error("getOrganizationByUuid called with invalid UUID format: {}", uuid);
            throw new PaykeshoError("Invalid UUID format");
        }

        Organization organization = organizationRepository.findByUuid(uuid)
                .orElseThrow(() -> {
                    log.error("Organization not found with UUID: {}", uuid);
                    return new PaykeshoError("Could not find organization with UUID: " + uuid);
                });

        // Ensure UUID is set (in case it was null in database)
        if (organization.getUuid() == null || organization.getUuid().trim().isEmpty()) {
            log.warn("Organization {} has null or empty UUID, generating new one", organization.getId());
            organization.ensureUuid();
            organizationRepository.save(organization);
        }

        processPhotoPath(organization);
        log.debug("Successfully retrieved organization with UUID: {}", uuid);
        return organization;
    }

    /**
     * Update organization enabled status by UUID
     * 
     * @param uuid The organization's UUID
     * @param enabled The new enabled status
     * @throws PaykeshoError if UUID is invalid or organization not found
     */
    public void updateOrganizationStatusByUuid(String uuid, boolean enabled) throws PaykeshoError {
        Organization organization = getOrganizationByUuid(uuid);
        updateOrganizationStatus(organization.getId(), enabled);
        log.info("Updated organization status for UUID: {} to enabled: {}", uuid, enabled);
    }

    /**
     * Helper method to process organization photo path
     * 
     * @param organization The organization entity
     */
    private void processPhotoPath(Organization organization) {
        if (organization != null && organization.getPhotoPath() != null && organization.getRemoteFileId() != null) {
            FileAccessResponse fileAccessResponse = fileUploadService.getFileAccessDetails(organization.getRemoteFileId());
            if (fileAccessResponse != null) {
                organization.setPhotoPath(fileAccessResponse.getFileUrl());
            }
        }
    }

    public Organization getOrganizationByEmail(String email) throws PaykeshoError {
        if (email == null) {
            throw new PaykeshoError("Null parameter email");
        }

        try {
            return organizationRepository.getOrganizationByEmail(email);
        } catch (NoSuchElementException exception) {
            throw new PaykeshoError("Could not find any user");
        }
    }

    public boolean isOrganizationUnique(Integer id, String organizationName, String organizationType) {
        if (organizationName == null || organizationType == null) {
            return false;
        }

        Organization organization = organizationRepository.getOrganizationByIdAndNameAndOrganizationType(id, organizationName, OrganizationType.valueOf(organizationType));

        if (organization == null) return true;
        boolean isCreatingNew = (id == null);

        if (isCreatingNew) {
            return false;
        } else {
            return Objects.equals(organization.getId(), id);
        }
    }

    public boolean isOrganizationUniqueByUuid(String uuid, String organizationName, String organizationType) {
        if (organizationName == null || organizationType == null) {
            return false;
        }

        // If creating new (no UUID), just check if name exists
        if (uuid == null || uuid.trim().isEmpty()) {
            Organization existingOrg = organizationRepository.getOrganizationByName(organizationName);
            return existingOrg == null;
        }

        // If editing, get the current organization and exclude it from the check
        try {
            Organization currentOrg = organizationRepository.findByUuid(uuid)
                    .orElseThrow(() -> new RuntimeException("Organization not found"));
            return isOrganizationUnique(currentOrg.getId(), organizationName, organizationType);
        } catch (Exception e) {
            // If organization not found, treat as new
            Organization existingOrg = organizationRepository.getOrganizationByName(organizationName);
            return existingOrg == null;
        }
    }

/*    public List<DataItem> getOrganizationTypesDataEnabled(boolean enabled) {
        List<Organization> memberInstitutions = organizationRepository.findOrganizationTypesEnabled(enabled);

        List<DataItem> dataItemList = new ArrayList<>();

        for (Organization detail : memberInstitutions) {
            String identifier = organizationType.getName();

            DataItem dataItem = new DataItem(identifier);

            int itemIndex = dataItemList.indexOf(dataItem);
            if (itemIndex >= 0) {
                dataItem = dataItemList.get(itemIndex);
                dataItem.increaseItemCount(1);
            } else {
                dataItemList.add(new DataItem(identifier, 1));
            }
        }

        return dataItemList;
    }*/

    public Long countInstitutions() {
        return organizationRepository.count();
    }

    public List<GPSDataItem> getOrganizationGPSDataEnabled(boolean enabled) {
        List<Organization> memberInstitutions = organizationRepository.findOrganizationTypesEnabled(enabled);

        List<GPSDataItem> dataItemList = new ArrayList<>();

        for (Organization detail : memberInstitutions) {
            String title = detail.getName();
            float lng = detail.getLongitude();
            float lat = detail.getLatitude();

            dataItemList.add(new GPSDataItem(title, lng, lat));
        }

        return dataItemList;
    }

    public Page<Organization> listForOrganizationByPage(int pageNum, String sortField, String sortDir, String keyword) {
        Sort sort = Sort.by(sortField);
        sort = sortDir.equals("asc") ? sort.ascending() : sort.descending();

        Pageable pageable = PageRequest.of(pageNum - 1, 200, sort);

        if (keyword != null) {
            return organizationRepository.findAll(keyword, pageable);
        }
        return organizationRepository.findAll(pageable);
    }

    @Transactional(rollbackOn = {Exception.class})
    public EmployeeRecord saveEmployeeRecord(EmployeeRecord employeeRecord) {
        if (employeeRecord == null) {
            return null;
        }

        return employeeRecordRepository.save(employeeRecord);
    }

    public List<EmployeeRecord> listEmployeeRecordsAll(Integer id) {
        if (id == null) {
            return null;
        }

        return employeeRecordRepository.getEmployeeRecordsByOrganizationId(id);
    }

    public List<Client> listClientRecordsAll(Integer id) {
        if (id == null) {
            return null;
        }

        return clientRepository.listClientRecordsAll(id);
    }

    @Transactional(rollbackOn = {Exception.class})
    public EmployeeRecord getEmployeeRecordById(Integer id) {
        if (id == null) {
            return null;
        }
        return employeeRecordRepository.findById(id).orElse(null);
    }

    @Transactional(rollbackOn = {Exception.class})
    public void deleteEmployeeRecord(Integer id) throws PaykeshoError {
        if (id == null) {
            throw new PaykeshoError("Employee record ID is required");
        }

        EmployeeRecord employeeRecord = employeeRecordRepository.findById(id).orElse(null);
        if (employeeRecord == null) {
            throw new PaykeshoError("Employee record not found");
        }

        employeeRecordRepository.deleteById(id);
    }

    public void deleteEmployeeRecordByUuid(String uuid) throws PaykeshoError {
        if (uuid == null || uuid.isEmpty()) {
            throw new PaykeshoError("Employee record UUID is required");
        }

        EmployeeRecord employeeRecord = employeeRecordRepository.findByUuid(uuid).orElse(null);
        if (employeeRecord == null) {
            throw new PaykeshoError("Employee record not found");
        }

        employeeRecordRepository.deleteById(employeeRecord.getId());
    }

    @Transactional(rollbackOn = {Exception.class})
    public Client saveClientRecord(Client client) {
        if (client == null) {
            return null;
        }
        return clientRepository.save(client);
    }

    @Transactional(rollbackOn = {Exception.class})
    public Client getClientRecordById(Integer id) {
        if (id == null) {
            return null;
        }
        return clientRepository.findById(id).orElse(null);
    }

    @Transactional(rollbackOn = {Exception.class})
    public void deleteClientRecord(Integer id) throws PaykeshoError {
        if (id == null) {
            throw new PaykeshoError("Client record ID is required");
        }

        Client client = clientRepository.findById(id).orElse(null);
        if (client == null) {
            throw new PaykeshoError("Client record not found");
        }

        clientRepository.deleteById(id);
    }

    public void deleteClientRecordByUuid(String uuid) throws PaykeshoError {
        if (uuid == null || uuid.isEmpty()) {
            throw new PaykeshoError("Client UUID is required");
        }

        Client client = clientRepository.findByUuid(uuid).orElse(null);
        if (client == null) {
            throw new PaykeshoError("Client record not found");
        }

        clientRepository.deleteById(client.getId());
    }

/*    public List<GeneralStatsDto> getOrganizationsCountByLevelOfCare(Integer countryId, boolean enabled) {
        return organizationRepository.getOrganizationsCountByLevelOfCare(countryId, enabled);
    }*/

    /**
     * Save bulk employee records
     */
    @Transactional(rollbackOn = {Exception.class})
    public int saveBulkEmployeeRecords(List<EmployeeRecord> employeeRecords) {
        int savedCount = 0;
        for (EmployeeRecord employee : employeeRecords) {
            try {
                // Check if employee with same NRC already exists for this organization
                EmployeeRecord existing = employeeRecordRepository.findByNrcAndOrganization(
                        employee.getNrc(), employee.getOrganization());

                if (existing == null) {
                    employeeRecordRepository.save(employee);
                    savedCount++;
                    log.debug("Saved employee record: {}", employee.getNrc());
                } else {
                    log.warn("Employee record already exists: {}", employee.getNrc());
                }
            } catch (Exception e) {
                log.error("Error saving employee record {}: {}", employee.getNrc(), e.getMessage());
            }
        }
        return savedCount;
    }

    /**
     * Save bulk client records
     */
    @Transactional(rollbackOn = {Exception.class})
    public int saveBulkClientRecords(List<Client> clientRecords) {
        int savedCount = 0;
        for (Client client : clientRecords) {
            try {
                // Check if client with same email already exists for this organization
                Client existing = clientRepository.findByEmailAndOrganization(
                        client.getEmail(), client.getOrganization());

                if (existing == null) {
                    clientRepository.save(client);
                    savedCount++;
                    log.debug("Saved client record: {}", client.getEmail());
                } else {
                    log.warn("Client record already exists: {}", client.getEmail());
                }
            } catch (Exception e) {
                log.error("Error saving client record {}: {}", client.getEmail(), e.getMessage());
            }
        }
        return savedCount;
    }

}
