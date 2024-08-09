package org.paykesho.clientportal.organizations.service;

import org.commons.entity.organization.Organization;
import org.commons.utils.PaykeshoError;
import org.paykesho.clientportal.organizations.dto.GPSDataItem;
import org.paykesho.clientportal.organizations.repository.OrganizationRepository;
import org.paykesho.clientportal.settings.service.SettingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@Transactional
public class OrganizationService {

    public static final int MEMBERS_PER_PAGE = 10;
    protected DateFormat dateFormat;
    @Autowired
    private OrganizationRepository organizationRepository;
    @Autowired
    private SettingService settingService;

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
        Organization organization = organizationRepository.getOrganizationByEmail(email);

        if (organization == null) return true;
        boolean isCreatingNew = (id == null);

        if (isCreatingNew) {
            if (organization != null) return false;
        } else {
            if (organization.getId() != id) {
                return false;
            }
        }

        return true;
    }

    public boolean isNameUnique(Integer id, String email) {
        if (email == null) {
            return false;
        }
        Organization organization = organizationRepository.getOrganizationByName(email);

        if (organization == null) return true;
        boolean isCreatingNew = (id == null);

        if (isCreatingNew) {
            if (organization != null) return false;
        } else {
            if (organization.getId() != id) {
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

    public void delete(Integer id) throws PaykeshoError {
        if (id == null) {
            throw new PaykeshoError("Null parameter id");
        }

        Long countById = organizationRepository.countById(id);
        if (countById == null || countById == 0) {
            throw new PaykeshoError("Could not find any member");
        }

        Organization organization = getOrganizationById(id);
        if (organization.getId() == null) throw new PaykeshoError("Could not find any driver");
        organization.setDeleted(true);
        save(organization);
        // userRepository.deleteById(id);
    }

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

    public boolean isMemberUnique(Integer id, Integer provinceId, String memberName, String memberType, String email) {
        if (provinceId == null || memberName == null || memberType == null) {
            return false;
        }
        Organization organization = organizationRepository.getOrganizationByIdAndProvinceAndNameAndMemberTypeAndEmail(id, provinceId, memberName, email);

        if (organization == null) return true;
        boolean isCreatingNew = (id == null);

        if (isCreatingNew) {
            if (organization != null) return false;
        } else {
            if (organization.getId() != id) {
                return false;
            }
        }

        return true;
    }

/*    public List<DataItem> getOrganizationTypesDataEnabled(boolean enabled) {
        List<Organization> memberInstitutions = organizationRepository.findOrganizationTypesEnabled(enabled);

        List<DataItem> dataItemList = new ArrayList<>();

        for (Organization detail : memberInstitutions) {
            LevelOfCare levelOfCare = settingService.findLevelOfCareById(detail.getLevelOfCare().getId());
            String identifier = levelOfCare.getName();

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

    public List<Organization> getAssessmentFacilities() {
        return organizationRepository.findOrganizationEnabled(true);
    }
}
