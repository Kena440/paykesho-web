package org.paykesho.backoffice.merchants.service;

import org.commons.entity.State;
import org.commons.entity.merchant.Merchant;
import org.commons.enums.MerchantType;
import org.commons.utils.PaykeshoError;
import org.paykesho.backoffice.files.dto.FileAccessResponse;
import org.paykesho.backoffice.files.service.FileUploadService;
import org.paykesho.backoffice.merchants.repository.MerchantRepository;
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
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;

@Service
@Transactional
public class MerchantService {

    public static final int MEMBERS_PER_PAGE = 20;
    protected DateFormat dateFormat;
    @Autowired
    private MerchantRepository merchantRepository;
    @Autowired
    private SettingService settingService;
    @Autowired
    private FileUploadService fileUploadService;

    public List<Merchant> listAll() {
        List<Merchant> merchants = (List<Merchant>) merchantRepository.findAll(Sort.by("name").ascending());

        for (Merchant merchant : merchants) {
            if (merchant.getPhotoPath() != null) {
                FileAccessResponse fileAccessResponse = fileUploadService.getFileAccessDetails(merchant.getRemoteFileId());
                if (fileAccessResponse != null) {
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

        State state = settingService.getProvinceByName("LUSAKA");
        merchant.setProvince(state);
        merchant.setDistrict("LUSAKA");

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
            return merchantRepository.findById(id).get();
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


}
