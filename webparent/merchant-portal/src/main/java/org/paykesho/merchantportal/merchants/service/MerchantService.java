package org.paykesho.merchantportal.merchants.service;

import lombok.extern.slf4j.Slf4j;
import org.commons.entity.merchant.Merchant;
import org.commons.entity.merchant.MerchantAdmin;
import org.commons.utils.PaykeshoError;
import org.paykesho.merchantportal.merchants.dto.GPSDataItem;
import org.paykesho.merchantportal.merchants.repository.MerchantRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

@Slf4j
@Service
@Transactional
public class MerchantService {

    public static final int MEMBERS_PER_PAGE = 10;
    protected DateFormat dateFormat;
    @Autowired
    private MerchantRepository merchantRepository;


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
            return merchantRepository.findById(id).get();
        } catch (NoSuchElementException exception) {
            throw new PaykeshoError("Could not find any member");
        }
    }

    /*public void delete(Integer id) throws PaykeshoError {
        if (id == null) {
            throw new PaykeshoError("Null parameter id");
        }

        Long countById = merchantRepository.countById(id);
        if (countById == null || countById == 0) {
            throw new PaykeshoError("Could not find any member");
        }

        Merchant merchant = getMerchantById(id);
        if (merchant.getId() == null) throw new PaykeshoError("Could not find any driver");
        merchant.setDeleted(true);
        save(merchant);
        // userRepository.deleteById(id);
    }
*/
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

    public boolean isMemberUnique(Integer id, String memberName, String memberType, String email) {
        if (memberName == null || memberType == null) {
            return false;
        }
        Merchant merchant = merchantRepository.getMerchantByIdAndNameAndMemberTypeAndEmail(id, memberName, email);

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

/*    public List<DataItem> getMerchantTypesDataEnabled(boolean enabled) {
        List<Merchant> memberInstitutions = merchantRepository.findMerchantTypesEnabled(enabled);

        List<DataItem> dataItemList = new ArrayList<>();

        for (Merchant detail : memberInstitutions) {
            LevelOfCare organizationType = settingService.findLevelOfCareById(detail.getLevelOfCare().getId());
            String identifier = organizationType.getName();

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
        return merchantRepository.count();
    }

    public List<GPSDataItem> getMerchantGPSDataEnabled(boolean enabled) {
        List<Merchant> memberInstitutions = merchantRepository.findMerchantTypesEnabled(enabled);

        List<GPSDataItem> dataItemList = new ArrayList<>();

        for (Merchant detail : memberInstitutions) {
            String title = detail.getName();
            float lng = detail.getLongitude();
            float lat = detail.getLatitude();

            dataItemList.add(new GPSDataItem(title, lng, lat));
        }

        return dataItemList;
    }

    public List<Merchant> getAssessmentFacilities() {
        return merchantRepository.findMerchantEnabled(true);
    }

    public Merchant getMerchantByContactPerson(MerchantAdmin merchantAdmin) {
        return merchantRepository.getMerchantByContactPersonId(merchantAdmin.getId());
    }
}
