package org.paykesho.clientportal.organizations.service;

import lombok.extern.slf4j.Slf4j;
import org.commons.entity.organization.Organization;
import org.commons.utils.PaykeshoError;
import org.paykesho.clientportal.organizations.repository.OrganizationRepository;
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
    // @Autowired
    // private SettingService settingService; // Unused field

    public Organization getOrganizationById(Integer id) throws PaykeshoError {
        try {
            log.debug("Retrieving organization by ID: {}", id);
            if (id == null) {
                throw new PaykeshoError("Null parameter id");
            }

            try {
                Organization organization = organizationRepository.findById(id).get();
                log.debug("Found organization with ID: {}", id);
                return organization;
            } catch (NoSuchElementException exception) {
                log.warn("Organization not found with ID: {}", id);
                throw new PaykeshoError("Could not find any member");
            }
        } catch (PaykeshoError ex) {
            log.warn("PaykeshoError retrieving organization by ID {}: {}", id, ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            log.error("Error retrieving organization by ID {}: {}", id, ex.getMessage(), ex);
            throw new RuntimeException("Failed to retrieve organization by ID", ex);
        }
    }

    public Organization getOrganizationByName(String name) {
        return organizationRepository.getOrganizationByName(name);
    }

    public Long countOrganizations() {
        return organizationRepository.count();
    }

    public List<Organization> organizationListEnabled() {
        try {
            log.debug("Retrieving enabled organizations");
            List<Organization> organizations = organizationRepository.findOrganizationEnabled(true);
            log.debug("Successfully retrieved {} enabled organizations", organizations.size());
            return organizations;
        } catch (Exception ex) {
            log.error("Error retrieving enabled organizations: {}", ex.getMessage(), ex);
            throw new RuntimeException("Failed to retrieve enabled organizations", ex);
        }
    }
}
