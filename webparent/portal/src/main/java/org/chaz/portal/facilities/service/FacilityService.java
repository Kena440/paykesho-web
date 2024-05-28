package org.chaz.portal.facilities.service;

import org.chaz.portal.facilities.dto.DataItem;
import org.chaz.portal.facilities.dto.GPSDataItem;
import org.chaz.portal.facilities.repository.FacilityRepository;
import org.chaz.portal.settings.service.SettingService;
import org.commons.entity.facilities.Facility;
import org.commons.entity.facilities.LevelOfCare;
import org.commons.utils.AssessmentToolError;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@Transactional
public class FacilityService {

    public static final int MEMBERS_PER_PAGE = 10;
    protected DateFormat dateFormat;
    @Autowired
    private FacilityRepository facilityRepository;
    @Autowired
    private SettingService settingService;

    public Facility save(Facility facility) {
        if (facility == null) {
            return null;
        }

        return facilityRepository.save(facility);
    }

    public boolean isEmailUnique(Integer id, String email) {
        if (email == null) {
            return false;
        }
        Facility facility = facilityRepository.getFacilityByEmail(email);

        if (facility == null) return true;
        boolean isCreatingNew = (id == null);

        if (isCreatingNew) {
            if (facility != null) return false;
        } else {
            if (facility.getId() != id) {
                return false;
            }
        }

        return true;
    }

    public boolean isNameUnique(Integer id, String email) {
        if (email == null) {
            return false;
        }
        Facility facility = facilityRepository.getFacilityByName(email);

        if (facility == null) return true;
        boolean isCreatingNew = (id == null);

        if (isCreatingNew) {
            if (facility != null) return false;
        } else {
            if (facility.getId() != id) {
                return false;
            }
        }

        return true;
    }

    public Facility getFacilityById(Integer id) throws AssessmentToolError {
        if (id == null) {
            throw new AssessmentToolError("Null parameter id");
        }

        try {
            return facilityRepository.findById(id).get();
        } catch (NoSuchElementException exception) {
            throw new AssessmentToolError("Could not find any member");
        }
    }

    public void delete(Integer id) throws AssessmentToolError {
        if (id == null) {
            throw new AssessmentToolError("Null parameter id");
        }

        Long countById = facilityRepository.countById(id);
        if (countById == null || countById == 0) {
            throw new AssessmentToolError("Could not find any member");
        }

        Facility facility = getFacilityById(id);
        if (facility.getId() == null) throw new AssessmentToolError("Could not find any driver");
        facility.setDeleted(true);
        save(facility);
        // userRepository.deleteById(id);
    }

    public void updateFacilityStatus(Integer id, boolean enabled) throws AssessmentToolError {
        if (id == null) {
            throw new AssessmentToolError("Null parameter id");
        }

        Long countById = facilityRepository.countById(id);
        if (countById == null || countById == 0) {
            throw new AssessmentToolError("Could not find any member");
        }

        facilityRepository.updateEnableStatus(id, enabled);
    }

    public Facility getFacilityByEmail(String email) throws AssessmentToolError {
        if (email == null) {
            throw new AssessmentToolError("Null parameter email");
        }

        try {
            return facilityRepository.getFacilityByEmail(email);
        } catch (NoSuchElementException exception) {
            throw new AssessmentToolError("Could not find any user");
        }
    }

    public boolean isMemberUnique(Integer id, Integer provinceId, String memberName, String memberType, String email) {
        if (provinceId == null || memberName == null || memberType == null) {
            return false;
        }
        Facility facility = facilityRepository.getFacilityByIdAndProvinceAndNameAndMemberTypeAndEmail(id, provinceId, memberName, email);

        if (facility == null) return true;
        boolean isCreatingNew = (id == null);

        if (isCreatingNew) {
            if (facility != null) return false;
        } else {
            if (facility.getId() != id) {
                return false;
            }
        }

        return true;
    }

    public List<DataItem> getFacilityTypesDataEnabled(boolean enabled) {
        List<Facility> memberInstitutions = facilityRepository.findFacilityTypesEnabled(enabled);

        List<DataItem> dataItemList = new ArrayList<>();

        for (Facility detail : memberInstitutions) {
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
    }

    public Long countInstitutions() {
        return facilityRepository.count();
    }

    public List<GPSDataItem> getFacilityGPSDataEnabled(boolean enabled) {
        List<Facility> memberInstitutions = facilityRepository.findFacilityTypesEnabled(enabled);

        List<GPSDataItem> dataItemList = new ArrayList<>();

        for (Facility detail : memberInstitutions) {
            String title = detail.getName();
            float lng = detail.getLongitude();
            float lat = detail.getLatitude();

            dataItemList.add(new GPSDataItem(title, lng, lat));
        }

        return dataItemList;
    }

    public List<Facility> getAssessmentFacilities() {
        return facilityRepository.findFacilityEnabled(true);
    }
}
