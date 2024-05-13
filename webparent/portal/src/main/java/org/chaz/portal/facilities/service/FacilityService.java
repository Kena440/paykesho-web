package org.chaz.portal.facilities.service;

import org.chaz.membershipsite.memberinstitution.repository.MemberInstitutionRepository;
import org.membershipcommons.entity.members.MemberAdmin;
import org.membershipcommons.entity.members.MemberInstitution;
import org.membershipcommons.enums.MemberType;
import org.membershipcommons.utils.MembershipMgtError;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.text.DateFormat;
import java.util.NoSuchElementException;

@Service
@Transactional
public class MemberInstitutionService {

    public static final int MEMBERS_PER_PAGE = 10;
    protected DateFormat dateFormat;
    @Autowired
    private MemberInstitutionRepository memberInstitutionRepository;

    public MemberInstitution save(MemberInstitution memberInstitution) {
        if (memberInstitution == null) {
            return null;
        }

        return memberInstitutionRepository.save(memberInstitution);
    }

    public boolean isEmailUnique(Integer id, String email) {
        if (email == null) {
            return false;
        }
        MemberInstitution institution = memberInstitutionRepository.getMemberInstitutionByEmail(email);

        if (institution == null) return true;
        boolean isCreatingNew = (id == null);

        if (isCreatingNew) {
            if (institution != null) return false;
        } else {
            if (institution.getId() != id) {
                return false;
            }
        }

        return true;
    }

    public boolean isNameUnique(Integer id, String email) {
        if (email == null) {
            return false;
        }
        MemberInstitution institution = memberInstitutionRepository.getMemberInstitutionByName(email);

        if (institution == null) return true;
        boolean isCreatingNew = (id == null);

        if (isCreatingNew) {
            if (institution != null) return false;
        } else {
            if (institution.getId() != id) {
                return false;
            }
        }

        return true;
    }

    public MemberInstitution getMemberInstitutionById(Integer id) throws MembershipMgtError {
        if (id == null) {
            throw new MembershipMgtError("Null parameter id");
        }

        try {
            return memberInstitutionRepository.findById(id).get();
        } catch (NoSuchElementException exception) {
            throw new MembershipMgtError("Could not find any member");
        }
    }

    public void delete(Integer id) throws MembershipMgtError {
        if (id == null) {
            throw new MembershipMgtError("Null parameter id");
        }

        Long countById = memberInstitutionRepository.countById(id);
        if (countById == null || countById == 0) {
            throw new MembershipMgtError("Could not find any member");
        }

        MemberInstitution memberInstitution = getMemberInstitutionById(id);
        if (memberInstitution.getId() == null) throw new MembershipMgtError("Could not find any driver");
        memberInstitution.setDeleted(true);
        save(memberInstitution);
        // userRepository.deleteById(id);
    }

    public void updateMemberInstitutionStatus(Integer id, boolean enabled) throws MembershipMgtError {
        if (id == null) {
            throw new MembershipMgtError("Null parameter id");
        }

        Long countById = memberInstitutionRepository.countById(id);
        if (countById == null || countById == 0) {
            throw new MembershipMgtError("Could not find any member");
        }

        memberInstitutionRepository.updateEnableStatus(id, enabled);
    }

    public MemberInstitution getMemberInstitutionByEmail(String email) throws MembershipMgtError {
        if (email == null) {
            throw new MembershipMgtError("Null parameter email");
        }

        try {
            return memberInstitutionRepository.getMemberInstitutionByEmail(email);
        } catch (NoSuchElementException exception) {
            throw new MembershipMgtError("Could not find any user");
        }
    }

    public boolean isMemberUnique(Integer id, Integer provinceId, String memberName, String memberType, String email) {
        if (provinceId == null || memberName == null || memberType == null) {
            return false;
        }
        MemberInstitution institution = memberInstitutionRepository.getMemberInstitutionByIdAndProvinceAndNameAndMemberTypeAndEmail(id, provinceId, memberName, MemberType.valueOf(memberType), email);

        if (institution == null) return true;
        boolean isCreatingNew = (id == null);

        if (isCreatingNew) {
            if (institution != null) return false;
        } else {
            if (institution.getId() != id) {
                return false;
            }
        }

        return true;
    }

    public Long countInstitutions() {
        return memberInstitutionRepository.count();
    }

    public Long getTotalCatchmentMembers(boolean enabled) {
        return memberInstitutionRepository.getTotalCatchmentMembers(enabled);
    }

    public MemberInstitution getMemberInstitutionByMemberAdmin(MemberAdmin memberAdmin) {
        return memberInstitutionRepository.getMemberInstitutionByContactPerson(memberAdmin.getId());
    }
}
