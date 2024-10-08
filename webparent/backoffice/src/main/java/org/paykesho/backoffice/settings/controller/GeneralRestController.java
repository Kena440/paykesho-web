package org.paykesho.backoffice.settings.controller;

import org.commons.entity.merchant.Merchant;
import org.commons.entity.organization.Organization;
import org.commons.entity.settings.Country;
import org.paykesho.backoffice.merchants.repository.MerchantRepository;
import org.paykesho.backoffice.organizations.repository.OrganizationRepository;
import org.paykesho.backoffice.settings.dto.MapDataDto;
import org.paykesho.backoffice.settings.repository.CountryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/general/rest")
public class GeneralRestController {
    @Autowired
    private MerchantRepository merchantRepository;
    @Autowired
    private OrganizationRepository organizationRepository;

    @GetMapping("/map-data")
    public List<MapDataDto> mapDataList() {
        List<MapDataDto> items = new ArrayList<>();

        List<Merchant> merchants = merchantRepository.findMerchantTypesEnabled(true);

        for (Merchant merchant: merchants){
            items.add(new MapDataDto(merchant.getName(), merchant.getMerchantType().name(), merchant.getLatitude(), merchant.getLongitude()));
        }

        List<Organization> organizations = organizationRepository.findOrganizationTypesEnabled(true);

        for (Organization organization: organizations){
            items.add(new MapDataDto(organization.getName(), organization.getOrganizationType().name(), organization.getLatitude(), organization.getLongitude()));
        }

        return items;
    }

}
