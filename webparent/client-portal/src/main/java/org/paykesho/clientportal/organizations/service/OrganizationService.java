package org.paykesho.clientportal.organizations.service;

import lombok.extern.slf4j.Slf4j;
import org.commons.entity.organization.Organization;
import org.commons.utils.PaykeshoError;
import org.paykesho.clientportal.organizations.repository.OrganizationRepository;
import org.paykesho.clientportal.settings.service.SettingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.text.DateFormat;
import java.util.List;
import java.util.NoSuchElementException;

@Slf4j
@Service
@Transactional
public class OrganizationService {

    public static final int MEMBERS_PER_PAGE = 10;
    protected DateFormat dateFormat;
    @Autowired
    private OrganizationRepository organizationRepository;
    @Autowired
    private SettingService settingService;

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

    public Organization getOrganizationByName(String name) {
        return organizationRepository.getOrganizationByName(name);
    }

    public Long countOrganizations() {
        return organizationRepository.count();
    }

    public List<Organization> organizationListEnabled() {
        return organizationRepository.findOrganizationEnabled(true);
    }
}
