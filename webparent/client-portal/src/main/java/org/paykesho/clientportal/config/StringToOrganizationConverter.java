package org.paykesho.clientportal.config;

import lombok.extern.slf4j.Slf4j;
import org.commons.entity.organization.Organization;
import org.commons.utils.PaykeshoError;
import org.paykesho.clientportal.organizations.service.OrganizationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

/**
 * Converter to convert UUID strings to Organization entities.
 * This is needed for form binding when the form submits organization UUIDs.
 */
@Slf4j
@Component
public class StringToOrganizationConverter implements Converter<String, Organization> {

    @Autowired
    private OrganizationService organizationService;

    @Override
    public Organization convert(String source) {
        if (source == null || source.trim().isEmpty()) {
            return null;
        }

        try {
            log.debug("Converting UUID string to Organization: {}", source);
            return organizationService.getOrganizationByUuid(source.trim());
        } catch (PaykeshoError e) {
            log.warn("Failed to convert UUID to Organization: {}", e.getMessage());
            // Return null to allow validation to handle the error
            return null;
        } catch (Exception e) {
            log.error("Unexpected error converting UUID to Organization: {}", e.getMessage(), e);
            return null;
        }
    }
}

