package org.chaz.backoffice.csos.service;

import org.chaz.backoffice.csos.repository.CSORepository;
import org.chaz.backoffice.paging.PagingAndSortingHelper;
import org.chaz.backoffice.settings.service.SettingService;
import org.commons.entity.cso.CivilSocietyOrganization;
import org.commons.utils.AssessmentToolError;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.text.DateFormat;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;

@Service
@Transactional
public class CSOService {

    public static final int MEMBERS_PER_PAGE = 20;
    protected DateFormat dateFormat;
    @Autowired
    private CSORepository csoRepository;
    @Autowired
    private SettingService settingService;

    public List<CivilSocietyOrganization> listAll() {
        return (List<CivilSocietyOrganization>) csoRepository.findAll(Sort.by("name").ascending());
    }

    public void listByPage(int pageNum, PagingAndSortingHelper helper) throws AssessmentToolError {
        helper.listEntities(pageNum, MEMBERS_PER_PAGE, csoRepository);
    }

    @Transactional(rollbackOn = {Exception.class})
    public CivilSocietyOrganization save(CivilSocietyOrganization civilSocietyOrganization) {
        if (civilSocietyOrganization == null) {
            return null;
        }

        return csoRepository.save(civilSocietyOrganization);
    }

    public boolean isEmailUnique(Integer id, String email) {
        if (email == null) {
            return false;
        }
        CivilSocietyOrganization civilSocietyOrganization = csoRepository.getCivilSocietyOrganizationByEmail(email);

        if (civilSocietyOrganization == null) return true;
        boolean isCreatingNew = (id == null);

        if (isCreatingNew) {
            if (civilSocietyOrganization != null) return false;
        } else {
            if (civilSocietyOrganization.getId() != id) {
                return false;
            }
        }

        return true;
    }

    public boolean isNameUnique(Integer id, String email) {
        if (email == null) {
            return false;
        }
        CivilSocietyOrganization civilSocietyOrganization = csoRepository.getCivilSocietyOrganizationByName(email);

        if (civilSocietyOrganization == null) return true;
        boolean isCreatingNew = (id == null);

        if (isCreatingNew) {
            if (civilSocietyOrganization != null) return false;
        } else {
            if (civilSocietyOrganization.getId() != id) {
                return false;
            }
        }

        return true;
    }

    public CivilSocietyOrganization getCivilSocietyOrganizationById(Integer id) throws AssessmentToolError {
        if (id == null) {
            throw new AssessmentToolError("Null parameter id");
        }

        try {
            return csoRepository.findById(id).get();
        } catch (NoSuchElementException exception) {
            throw new AssessmentToolError("Could not find any member");
        }
    }

    @Transactional(rollbackOn = {Exception.class})
    public void delete(Integer id) throws AssessmentToolError {
        if (id == null) {
            throw new AssessmentToolError("Null parameter id");
        }

        Long countById = csoRepository.countById(id);
        if (countById == null || countById == 0) {
            throw new AssessmentToolError("Could not find any member");
        }

        CivilSocietyOrganization civilSocietyOrganization = getCivilSocietyOrganizationById(id);
        if (civilSocietyOrganization.getId() == null)
            throw new AssessmentToolError("Could not find any civilSocietyOrganization");
        civilSocietyOrganization.setDeleted(true);
        save(civilSocietyOrganization);
        // userRepository.deleteById(id);
    }

    @Transactional(rollbackOn = {Exception.class})
    public void updateCivilSocietyOrganizationStatus(Integer id, boolean enabled) throws AssessmentToolError {
        if (id == null) {
            throw new AssessmentToolError("Null parameter id");
        }

        Long countById = csoRepository.countById(id);
        if (countById == null || countById == 0) {
            throw new AssessmentToolError("Could not find any member");
        }

        csoRepository.updateEnableStatus(id, enabled);
    }

    public CivilSocietyOrganization getCivilSocietyOrganizationByEmail(String email) throws AssessmentToolError {
        if (email == null) {
            throw new AssessmentToolError("Null parameter email");
        }

        try {
            return csoRepository.getCivilSocietyOrganizationByEmail(email);
        } catch (NoSuchElementException exception) {
            throw new AssessmentToolError("Could not find any user");
        }
    }

    public boolean isCivilSocietyOrganizationUnique(Integer id, Integer provinceId, String civilSocietyOrganizationName, String district) {
        if (provinceId == null || civilSocietyOrganizationName == null || district == null) {
            return false;
        }
        CivilSocietyOrganization civilSocietyOrganization = csoRepository.getCivilSocietyOrganizationByIdAndProvinceAndNameAndDistrict(id, provinceId, civilSocietyOrganizationName, district);

        if (civilSocietyOrganization == null) return true;
        boolean isCreatingNew = (id == null);

        if (isCreatingNew) {
            return false;
        } else {
            return Objects.equals(civilSocietyOrganization.getId(), id);
        }
    }

    public Long countInstitutions() {
        return csoRepository.count();
    }

    public Page<CivilSocietyOrganization> listForCivilSocietyOrganizationByPage(int pageNum, String sortField, String sortDir, String keyword) {
        Sort sort = Sort.by(sortField);
        sort = sortDir.equals("asc") ? sort.ascending() : sort.descending();

        Pageable pageable = PageRequest.of(pageNum - 1, 200, sort);

        if (keyword != null) {
            return csoRepository.findAll(keyword, pageable);
        }
        return csoRepository.findAll(pageable);
    }


}
