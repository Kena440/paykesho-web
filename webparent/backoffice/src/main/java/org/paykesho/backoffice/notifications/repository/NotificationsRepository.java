package org.paykesho.backoffice.notifications.repository;

import org.commons.dto.GeneralStatsDto;
import org.commons.entity.Notification;
import org.commons.entity.merchant.Merchant;
import org.commons.enums.MerchantType;
import org.paykesho.backoffice.paging.SearchRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationsRepository extends JpaRepository<Notification, Long> {
    // Additional query methods can be defined here
}
