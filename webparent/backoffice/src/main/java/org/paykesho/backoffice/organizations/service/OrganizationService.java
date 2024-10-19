package org.paykesho.backoffice.organizations.service;

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
import org.paykesho.backoffice.settings.service.SettingService;
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
    @Autowired
    private SettingService settingService;
    @Autowired
    private FileUploadService fileUploadService;

    public List<Organization> listAll() {

        List<Organization> organizations = (List<Organization>) organizationRepository.findAll(Sort.by("name").ascending());

        for (Organization organization : organizations) {
            if (organization.getPhotoPath() != null) {
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
            return organizationRepository.findById(id).get();
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

/*    public List<GeneralStatsDto> getOrganizationsCountByLevelOfCare(Integer countryId, boolean enabled) {
        return organizationRepository.getOrganizationsCountByLevelOfCare(countryId, enabled);
    }*/

}
