package org.paykesho.backoffice.merchants.service;

import lombok.extern.slf4j.Slf4j;
import org.commons.entity.merchant.Merchant;
import org.commons.enums.MerchantType;
import org.commons.utils.PaykeshoError;
import org.paykesho.backoffice.files.dto.FileAccessResponse;
import org.paykesho.backoffice.files.service.FileUploadService;
import org.paykesho.backoffice.merchants.repository.MerchantRepository;
import org.paykesho.backoffice.paging.PagingAndSortingHelper;
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
import java.util.UUID;

@Slf4j
@Service
@Transactional
public class MerchantService {

    public static final int MEMBERS_PER_PAGE = 20;
    protected DateFormat dateFormat;
    @Autowired
    private MerchantRepository merchantRepository;
    // @Autowired
    // private SettingService settingService; // Unused field
    @Autowired
    private FileUploadService fileUploadService;

    public List<Merchant> listAll() {
        List<Merchant> merchants = (List<Merchant>) merchantRepository.findAll(Sort.by("name").ascending());

        for (Merchant merchant : merchants) {
            if (merchant.getPhotoPath() != null && merchant.getRemoteFileId() != null) {
                FileAccessResponse fileAccessResponse = fileUploadService.getFileAccessDetails(merchant.getRemoteFileId());
                if (fileAccessResponse != null && fileAccessResponse.getFileUrl() != null) {
                    merchant.setPhotoPath(fileAccessResponse.getFileUrl());
                }
            }
        }

        return merchants;
    }

    public void listByPage(int pageNum, PagingAndSortingHelper helper) throws PaykeshoError {
        helper.listEntities(pageNum, MEMBERS_PER_PAGE, merchantRepository);
    }

    @Transactional(rollbackOn = {Exception.class})
    public Merchant save(Merchant merchant) {
        if (merchant == null) {
            return null;
        }

        return merchantRepository.save(merchant);
    }

    public boolean isEmailUnique(Integer id, String email) {
        if (email == null) {
            return false;
        }
        Merchant merchant = merchantRepository.getMerchantByEmail(email);

        if (merchant == null) return true;
        boolean isCreatingNew = (id == null);

        if (isCreatingNew) {
            if (merchant != null) return false;
        } else {
            if (merchant.getId() != id) {
                return false;
            }
        }

        return true;
    }

    public boolean isNameUnique(Integer id, String email) {
        if (email == null) {
            return false;
        }
        Merchant merchant = merchantRepository.getMerchantByName(email);

        if (merchant == null) return true;
        boolean isCreatingNew = (id == null);

        if (isCreatingNew) {
            if (merchant != null) return false;
        } else {
            if (merchant.getId() != id) {
                return false;
            }
        }

        return true;
    }

    public Merchant getMerchantById(Integer id) throws PaykeshoError {
        if (id == null) {
            throw new PaykeshoError("Null parameter id");
        }

        try {
            Merchant merchant = merchantRepository.findById(id).get();
            
            // Process photoPath the same way as in listAll()
            if (merchant.getPhotoPath() != null && merchant.getRemoteFileId() != null) {
                FileAccessResponse fileAccessResponse = fileUploadService.getFileAccessDetails(merchant.getRemoteFileId());
                if (fileAccessResponse != null && fileAccessResponse.getFileUrl() != null) {
                    merchant.setPhotoPath(fileAccessResponse.getFileUrl());
                }
            }
            
            return merchant;
        } catch (NoSuchElementException exception) {
            throw new PaykeshoError("Could not find any member");
        }
    }

    @Transactional(rollbackOn = {Exception.class})
    public void updateMerchantStatus(Integer id, boolean enabled) throws PaykeshoError {
        if (id == null) {
            throw new PaykeshoError("Null parameter id");
        }

        Long countById = merchantRepository.countById(id);
        if (countById == null || countById == 0) {
            throw new PaykeshoError("Could not find any member");
        }

        merchantRepository.updateEnableStatus(id, enabled);
    }

    public Merchant getMerchantByEmail(String email) throws PaykeshoError {
        if (email == null) {
            throw new PaykeshoError("Null parameter email");
        }

        try {
            return merchantRepository.getMerchantByEmail(email);
        } catch (NoSuchElementException exception) {
            throw new PaykeshoError("Could not find any user");
        }
    }

    public boolean isMerchantUnique(Integer id, String merchantName, String merchantType) {
        if (merchantName == null || merchantType == null) {
            return false;
        }

        Merchant merchant = merchantRepository.getMerchantByIdAndNameAndMerchantType(id, merchantName, MerchantType.valueOf(merchantType));

        if (merchant == null) return true;
        boolean isCreatingNew = (id == null);

        if (isCreatingNew) {
            return false;
        } else {
            return Objects.equals(merchant.getId(), id);
        }
    }

    public boolean isMerchantUniqueByUuid(String uuid, String merchantName, String merchantType) {
        if (merchantName == null || merchantType == null) {
            return false;
        }

        // If creating new (no UUID), just check if name exists
        if (uuid == null || uuid.trim().isEmpty()) {
            Merchant existingMerchant = merchantRepository.getMerchantByName(merchantName);
            return existingMerchant == null;
        }

        // If editing, get the current merchant and exclude it from the check
        try {
            Merchant currentMerchant = merchantRepository.findByUuid(uuid)
                    .orElseThrow(() -> new RuntimeException("Merchant not found"));
            return isMerchantUnique(currentMerchant.getId(), merchantName, merchantType);
        } catch (Exception e) {
            // If merchant not found, treat as new
            Merchant existingMerchant = merchantRepository.getMerchantByName(merchantName);
            return existingMerchant == null;
        }
    }

    public Long countInstitutions() {
        return merchantRepository.count();
    }

    public Page<Merchant> listForMerchantByPage(int pageNum, String sortField, String sortDir, String keyword) {
        Sort sort = Sort.by(sortField);
        sort = sortDir.equals("asc") ? sort.ascending() : sort.descending();

        Pageable pageable = PageRequest.of(pageNum - 1, 200, sort);

        if (keyword != null) {
            return merchantRepository.findAll(keyword, pageable);
        }
        return merchantRepository.findAll(pageable);
    }

    // ============================================================================
    // UUID-based methods for secure URL access
    // ============================================================================

    /**
     * Get merchant by UUID (for public/URL access)
     * This method should be used for all public-facing endpoints
     * 
     * @param uuid The merchant's UUID
     * @return Merchant entity
     * @throws PaykeshoError if UUID is invalid or merchant not found
     */
    public Merchant getMerchantByUuid(String uuid) throws PaykeshoError {
        // Validate UUID parameter
        if (uuid == null || uuid.trim().isEmpty()) {
            log.error("getMerchantByUuid called with null or empty UUID");
            throw new PaykeshoError("Invalid UUID parameter");
        }

        // Validate UUID format to prevent injection attacks
        try {
            UUID.fromString(uuid);
        } catch (IllegalArgumentException e) {
            log.error("getMerchantByUuid called with invalid UUID format: {}", uuid);
            throw new PaykeshoError("Invalid UUID format");
        }

        // Fetch merchant by UUID
        Merchant merchant = merchantRepository.findByUuid(uuid)
                .orElseThrow(() -> {
                    log.error("Merchant not found with UUID: {}", uuid);
                    return new PaykeshoError("Could not find merchant with UUID: " + uuid);
                });

        // Process photo path
        processPhotoPath(merchant);

        log.debug("Successfully retrieved merchant with UUID: {}", uuid);
        return merchant;
    }

    /**
     * Update merchant enabled status by UUID
     * 
     * @param uuid The merchant's UUID
     * @param enabled The new enabled status
     * @throws PaykeshoError if UUID is invalid or merchant not found
     */
    @Transactional(rollbackOn = {Exception.class})
    public void updateMerchantStatusByUuid(String uuid, boolean enabled) throws PaykeshoError {
        // Get merchant by UUID (includes validation)
        Merchant merchant = getMerchantByUuid(uuid);
        
        // Update status using internal ID
        updateMerchantStatus(merchant.getId(), enabled);
        
        log.info("Updated merchant status for UUID: {} to enabled: {}", uuid, enabled);
    }

    /**
     * Helper method to process merchant photo path
     * Extracted to avoid code duplication
     * 
     * @param merchant The merchant entity
     */
    private void processPhotoPath(Merchant merchant) {
        if (merchant != null && merchant.getPhotoPath() != null && merchant.getRemoteFileId() != null) {
            FileAccessResponse fileAccessResponse = fileUploadService.getFileAccessDetails(merchant.getRemoteFileId());
            if (fileAccessResponse != null && fileAccessResponse.getFileUrl() != null) {
                merchant.setPhotoPath(fileAccessResponse.getFileUrl());
            }
        }
    }


}
