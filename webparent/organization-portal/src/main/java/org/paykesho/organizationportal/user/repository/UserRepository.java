package org.paykesho.organizationportal.user.repository;

import org.commons.entity.User;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface UserRepository extends CrudRepository<User, Integer> {
    
    /**
     * Find all enabled users that are not deleted
     * Used for sending admin notifications
     */
    @Query("SELECT u FROM User u WHERE u.enabled = true AND u.deleted = false")
    List<User> findAllEnabledUsers();
}

