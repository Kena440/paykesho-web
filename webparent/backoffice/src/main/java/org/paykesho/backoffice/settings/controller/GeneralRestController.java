package org.paykesho.backoffice.settings.controller;

import org.commons.entity.merchant.Merchant;
import org.commons.entity.organization.Organization;
import org.paykesho.backoffice.files.dto.FileAccessResponse;
import org.paykesho.backoffice.files.service.FileUploadService;
import org.paykesho.backoffice.merchants.repository.MerchantRepository;
import org.paykesho.backoffice.organizations.repository.OrganizationRepository;
import org.paykesho.backoffice.settings.dto.MapDataDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/general/rest")
public class GeneralRestController {
    @Autowired
    private MerchantRepository merchantRepository;
    @Autowired
    private OrganizationRepository organizationRepository;
    @Autowired
    private FileUploadService fileUploadService;

    @GetMapping("/map-data")
    public List<MapDataDto> mapDataList() {
        List<MapDataDto> items = new ArrayList<>();

        List<Merchant> merchants = merchantRepository.findMerchantTypesEnabled(true);

        for (Merchant merchant : merchants) {
            FileAccessResponse fileAccessResponse = fileUploadService.getFileAccessDetails(merchant.getRemoteFileId());
            if (fileAccessResponse != null) {
                merchant.setPhotoPath(fileAccessResponse.getFileUrl());
            }
            items.add(new MapDataDto(merchant.getName(), merchant.getMerchantType().name(), merchant.getPhotoPath(), merchant.getLatitude(), merchant.getLongitude()));
        }

        List<Organization> organizations = organizationRepository.findOrganizationTypesEnabled(true);

        for (Organization organization : organizations) {
            FileAccessResponse fileAccessResponse = fileUploadService.getFileAccessDetails(organization.getRemoteFileId());
            if (fileAccessResponse != null) {
                organization.setPhotoPath(fileAccessResponse.getFileUrl());
            }
            items.add(new MapDataDto(organization.getName(), organization.getOrganizationType().name(), organization.getPhotoPath(), organization.getLatitude(), organization.getLongitude()));
        }

        return items;
    }

}
