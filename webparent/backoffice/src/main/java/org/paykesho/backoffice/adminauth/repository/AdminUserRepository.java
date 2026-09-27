package org.paykesho.backoffice.adminauth.repository;

import org.paykesho.backoffice.adminauth.entity.AdminUser;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface AdminUserRepository extends JpaRepository<AdminUser,Integer> {
    Optional<AdminUser> findByEmail(String email);
}
