# UUID Migration Checklist

## Overview
This checklist tracks the implementation of UUID-based URLs across the Paykesho platform for enhanced security.

---

## ✅ Phase 1: Foundation (COMPLETED)

- [x] Add UUID field to `IdBasedEntity.java`
- [x] Create database migration script (`add_uuid_columns.sql`)
- [x] Create implementation guide
- [ ] **TODO**: Run database migration script on all environments
  - [ ] Development
  - [ ] Staging
  - [ ] Production

---

## Phase 2: Repository Layer Updates

### Backoffice Module

#### MerchantRepository
- [ ] Add `Optional<Merchant> findByUuid(String uuid)`
- [ ] Add `boolean existsByUuid(String uuid)`
- **File**: `webparent/backoffice/src/main/java/org/paykesho/backoffice/merchants/repository/MerchantRepository.java`

#### UserRepository
- [ ] Add `Optional<User> findByUuid(String uuid)`
- [ ] Add `boolean existsByUuid(String uuid)`
- **File**: `webparent/backoffice/src/main/java/org/paykesho/backoffice/user/repository/UserRepository.java`

#### OrganizationRepository
- [ ] Add `Optional<Organization> findByUuid(String uuid)`
- [ ] Add `boolean existsByUuid(String uuid)`
- **File**: `webparent/backoffice/src/main/java/org/paykesho/backoffice/organizations/repository/OrganizationRepository.java`

#### ClientRepository
- [ ] Add `Optional<Client> findByUuid(String uuid)`
- [ ] Add `boolean existsByUuid(String uuid)`
- **File**: `webparent/backoffice/src/main/java/org/paykesho/backoffice/client/repository/ClientRepository.java`

#### VoucherRepository
- [ ] Add `Optional<Voucher> findByUuid(String uuid)`
- [ ] Add `boolean existsByUuid(String uuid)`
- **File**: Location to be determined

#### TransactionRepository
- [ ] Add `Optional<Transaction> findByUuid(String uuid)`
- [ ] Add `boolean existsByUuid(String uuid)`
- **File**: Location to be determined

#### SettlementRepository
- [ ] Add `Optional<Settlement> findByUuid(String uuid)`
- [ ] Add `boolean existsByUuid(String uuid)`
- **File**: Location to be determined

#### NotificationQueueRepository
- [ ] Add `Optional<NotificationQueue> findByUuid(String uuid)`
- [ ] Add `boolean existsByUuid(String uuid)`
- **File**: `webparent/notification-service/src/main/java/org/paykesho/notificationservice/repository/...`

### Other Modules

#### Organization Portal - ClientRepository
- [ ] Add UUID methods
- **File**: `webparent/organization-portal/src/main/java/.../repository/ClientRepository.java`

#### Merchant Portal Repositories
- [ ] Review and add UUID methods as needed
- **Location**: `webparent/merchant-portal/src/main/java/.../repository/`

#### Client Portal Repositories
- [ ] Review and add UUID methods as needed
- **Location**: `webparent/client-portal/src/main/java/.../repository/`

---

## Phase 3: Service Layer Updates

### MerchantService
- [ ] Add `getMerchantByUuid(String uuid)` method
- [ ] Add `updateMerchantStatusByUuid(String uuid, boolean enabled)` method
- **Files**:
  - `webparent/backoffice/src/main/java/org/paykesho/backoffice/merchants/service/MerchantService.java`
  - `webparent/merchant-portal/src/main/java/org/paykesho/merchantportal/merchants/service/MerchantService.java`
  - `webparent/client-portal/src/main/java/org/paykesho/clientportal/merchant/service/MerchantService.java`

### UserService
- [ ] Add `getUserByUuid(String uuid)` method
- [ ] Add `updateUserStatusByUuid(String uuid, boolean enabled)` method
- **File**: `webparent/backoffice/src/main/java/org/paykesho/backoffice/user/service/UserService.java`

### OrganizationService
- [ ] Add `getOrganizationByUuid(String uuid)` method
- [ ] Add `updateOrganizationStatusByUuid(String uuid, boolean enabled)` method
- [ ] Add `getEmployeeRecordsByUuid(String uuid)` method
- [ ] Add `getClientRecordsByUuid(String uuid)` method
- **Files**:
  - `webparent/backoffice/src/main/java/org/paykesho/backoffice/organizations/service/OrganizationService.java`
  - `webparent/organization-portal/src/main/java/org/paykesho/organizationportal/.../service/OrganizationService.java`

### ClientService
- [ ] Add `getClientByUuid(String uuid)` method
- [ ] Add `updateClientStatusByUuid(String uuid, boolean enabled)` method
- **Files**:
  - `webparent/backoffice/src/main/java/org/paykesho/backoffice/client/service/ClientService.java`
  - `webparent/organization-portal/src/main/java/org/paykesho/organizationportal/client/service/ClientService.java`
  - `webparent/client-portal/src/main/java/org/paykesho/clientportal/.../service/ClientService.java`

### VoucherService
- [ ] Add `getVoucherByUuid(String uuid)` method
- [ ] Add `approveVoucherByUuid(String uuid)` method
- **File**: `webparent/backoffice/src/main/java/org/paykesho/backoffice/voucher/service/VoucherService.java`

### TransactionService
- [ ] Add `getTransactionsByMerchantUuid(String uuid)` method
- **File**: `webparent/backoffice/src/main/java/org/paykesho/backoffice/transaction/service/TransactionService.java`

### SettlementService
- [ ] Add `getSettlementsByMerchantUuid(String uuid)` method
- **File**: `webparent/backoffice/src/main/java/org/paykesho/backoffice/settlement/service/SettlementService.java`

### NotificationQueueService
- [ ] Add `getNotificationByUuid(String uuid)` method
- [ ] Add `cancelNotificationByUuid(String uuid)` method
- [ ] Add `retryNotificationByUuid(String uuid)` method
- [ ] Add `processNotificationByUuid(String uuid)` method
- **File**: `webparent/notification-service/src/main/java/org/paykesho/notificationservice/service/...`

### EventService (Multiple Portals)
- [ ] Add `getEventByUuid(String uuid)` method
- **Files**:
  - `webparent/backoffice/.../events/service/EventService.java`
  - `webparent/organization-portal/.../events/service/EventService.java`
  - `webparent/merchant-portal/.../events/service/EventService.java`
  - `webparent/client-portal/.../events/service/EventService.java`

---

## Phase 4: Controller Layer Updates

### Backoffice Controllers

#### MerchantController.java
**File**: `webparent/backoffice/src/main/java/org/paykesho/backoffice/merchants/controller/MerchantController.java`

- [ ] Line 71-93: Update `/edit/{id}` to `/edit/{uuid}`
  - Change `@PathVariable(name = "id") Integer id` to `@PathVariable(name = "uuid") String uuid`
  - Change `merchantService.getMerchantById(id)` to `merchantService.getMerchantByUuid(uuid)`

- [ ] Line 181-212: Update `/view/{id}` to `/view/{uuid}`
  - Change path variable to use UUID
  - Update service call

- [ ] Line 213-233: Update `/{id}/enabled/{status}` to `/{uuid}/enabled/{status}`
  - Change path variable to use UUID
  - Update service call

- [ ] Line 233-260: Update `/details/{id}` to `/details/{uuid}`
  - Change path variable to use UUID
  - Update service call

#### UserController.java
**File**: `webparent/backoffice/src/main/java/org/paykesho/backoffice/user/controller/UserController.java`

- [ ] Line 137-158: Update `/edit/{id}` to `/edit/{uuid}`
- [ ] Line 160-180: Update `/{id}/enabled/{status}` to `/{uuid}/enabled/{status}`
- [ ] Line 180-200: Update `/details/{id}` to `/details/{uuid}`

#### OrganizationController.java
**File**: `webparent/backoffice/src/main/java/org/paykesho/backoffice/organizations/controller/OrganizationController.java`

- [ ] Line 118-167: Update `/view/{id}` to `/view/{uuid}`
- [ ] Line 168-191: Update `/edit/{id}` to `/edit/{uuid}`
- [ ] Line 192-206: Update `/delete/{id}` to `/delete/{uuid}`
- [ ] Line 207-227: Update `/{id}/enabled/{status}` to `/{uuid}/enabled/{status}`
- [ ] Line 227-250: Update `/details/{id}` to `/details/{uuid}`
- [ ] Line 306-320: Update `/{id}/employee-records` to `/{uuid}/employee-records`
- [ ] Line 427-440: Update `/{id}/client-records` to `/{uuid}/client-records`

#### ClientController.java
**File**: `webparent/backoffice/src/main/java/org/paykesho/backoffice/client/controller/ClientController.java`

- [ ] Line 41-55: Update `/{id}/enabled/{status}` to `/{uuid}/enabled/{status}`
- [ ] Line 56-69: Update `/details/{id}` to `/details/{uuid}`
- [ ] Line 70-90: Update `/edit/{id}` to `/edit/{uuid}`

#### VoucherController.java
**File**: `webparent/backoffice/src/main/java/org/paykesho/backoffice/voucher/controller/VoucherController.java`

- [ ] Line 67-71: Update `/voucher/{id}` to `/voucher/{uuid}`
- [ ] Line 72-78: Update `/view/{id}` to `/view/{uuid}`
- [ ] Line 79-100: Update `/approve/{id}` to `/approve/{uuid}`

#### TransactionController.java
**File**: `webparent/backoffice/src/main/java/org/paykesho/backoffice/transaction/controller/TransactionController.java`

- [ ] Line 37-42: Update `/merchant/{id}` to `/merchant/{uuid}`

#### SettlementController.java
**File**: `webparent/backoffice/src/main/java/org/paykesho/backoffice/settlement/controller/SettlementController.java`

- [ ] Line 43-48: Update `/merchant/{id}` to `/merchant/{uuid}`

#### StateRestController.java
**File**: `webparent/backoffice/src/main/java/org/paykesho/backoffice/settings/controller/StateRestController.java`

- [ ] Line 20-22: Update `/list-by-country/{id}` to `/list-by-country/{uuid}` (if Country has UUID)
- [ ] Line 38-40: Update `/delete/{id}` to `/delete/{uuid}`

#### CountryRestController.java
**File**: `webparent/backoffice/src/main/java/org/paykesho/backoffice/settings/controller/CountryRestController.java`

- [ ] Line 28-30: Update `/delete/{id}` to `/delete/{uuid}`

### Organization Portal Controllers

#### ClientController.java
**File**: `webparent/organization-portal/src/main/java/org/paykesho/organizationportal/client/controller/ClientController.java`

- [ ] Line 59-70: Update `/{id}/enabled/{status}` to `/{uuid}/enabled/{status}`
- [ ] Line 84-95: Update `/details/{id}` to `/details/{uuid}`
- [ ] Line 96-120: Update `/edit/{id}` to `/edit/{uuid}`

#### EventsController.java
**File**: `webparent/organization-portal/src/main/java/org/paykesho/organizationportal/events/controller/EventsController.java`

- [ ] Line 45-60: Update `/view/{id}` to `/view/{uuid}`

#### StateRestController.java
**File**: `webparent/organization-portal/src/main/java/org/paykesho/organizationportal/settings/controller/StateRestController.java`

- [ ] Line 23-25: Update `/list-states-by-country/{id}` to `/list-states-by-country/{uuid}`

### Merchant Portal Controllers

#### EventsController.java
**File**: `webparent/merchant-portal/src/main/java/org/paykesho/merchantportal/events/controller/EventsController.java`

- [ ] Line 45-60: Update `/view/{id}` to `/view/{uuid}`

#### StateRestController.java
**File**: `webparent/merchant-portal/src/main/java/org/paykesho/merchantportal/settings/controller/StateRestController.java`

- [ ] Line 23-25: Update `/list-states-by-country/{id}` to `/list-states-by-country/{uuid}`

### Client Portal Controllers

#### APIController.java
**File**: `webparent/client-portal/src/main/java/org/paykesho/clientportal/api/controller/APIController.java`

- [ ] Line 107-120: Update `/vouchers/{id}` to `/vouchers/{uuid}`
- [ ] Line 179-195: Update `/transactions/{id}` to `/transactions/{uuid}`

#### EventsController.java
**File**: `webparent/client-portal/src/main/java/org/paykesho/clientportal/events/controller/EventsController.java`

- [ ] Line 45-60: Update `/view/{id}` to `/view/{uuid}`

#### StateRestController.java
**File**: `webparent/client-portal/src/main/java/org/paykesho/clientportal/settings/controller/StateRestController.java`

- [ ] Line 23-25: Update `/list-states-by-country/{id}` to `/list-states-by-country/{uuid}`

### Notification Service Controllers

#### NotificationQueueController.java
**File**: `webparent/notification-service/src/main/java/org/paykesho/notificationservice/controller/NotificationQueueController.java`

- [ ] Line 159-175: Update `/{id}` GET endpoint to `/{uuid}`
- [ ] Line 177-190: Update `/{id}/cancel` POST endpoint to `/{uuid}/cancel`
- [ ] Line 191-210: Update `/{id}/retry` POST endpoint to `/{uuid}/retry`
- [ ] Line 233-250: Update `/{id}/process` POST endpoint to `/{uuid}/process`

---

## Phase 5: Frontend/Template Updates

### Thymeleaf Templates - Backoffice

#### Merchant Templates
**Directory**: `webparent/backoffice/src/main/resources/templates/merchants/`

- [ ] `merchants.html` - Update edit/view/delete links to use `${merchant.uuid}`
- [ ] `merchant_form.html` - Update form submission if applicable
- [ ] `merchant_detail.html` - Update any links

#### User Templates
**Directory**: `webparent/backoffice/src/main/resources/templates/users/`

- [ ] `users.html` - Update edit/view links to use `${user.uuid}`
- [ ] `user_form.html` - Update form submission if applicable
- [ ] `user_detail.html` - Update any links

#### Organization Templates
**Directory**: `webparent/backoffice/src/main/resources/templates/organizations/`

- [ ] `organizations.html` - Update edit/view/delete links to use `${organization.uuid}`
- [ ] `organization_form.html` - Update form submission if applicable
- [ ] `organization_detail.html` - Update any links

#### Client Templates
**Directory**: `webparent/backoffice/src/main/resources/templates/clients/`

- [ ] All templates - Update to use `${client.uuid}`

#### Voucher Templates
**Directory**: `webparent/backoffice/src/main/resources/templates/vouchers/`

- [ ] All templates - Update to use `${voucher.uuid}`

#### Transaction Templates
**Directory**: `webparent/backoffice/src/main/resources/templates/transactions/`

- [ ] All templates - Update to use `${transaction.uuid}`

#### Settlement Templates
**Directory**: `webparent/backoffice/src/main/resources/templates/settlements/`

- [ ] All templates - Update to use `${settlement.uuid}`

#### Event Templates
**Directory**: `webparent/backoffice/src/main/resources/templates/events/`

- [ ] `view_tabs/vendors.html` (Line 125) - Update vendor edit link
- [ ] `view_tabs/attendees.html` (Line 169) - Update attendee edit link

### Thymeleaf Templates - Organization Portal

**Directory**: `webparent/organization-portal/src/main/resources/templates/`

- [ ] `clients/` - All client-related templates
- [ ] `events/` - All event-related templates
- [ ] Any other templates with entity links

### Thymeleaf Templates - Merchant Portal

**Directory**: `webparent/merchant-portal/src/main/resources/templates/`

- [ ] `events/` - All event-related templates
- [ ] Any other templates with entity links

### Thymeleaf Templates - Client Portal

**Directory**: `webparent/client-portal/src/main/resources/templates/`

- [ ] `events/` - All event-related templates
- [ ] `vouchers/` - All voucher-related templates
- [ ] `transactions/` - All transaction-related templates
- [ ] Any other templates with entity links

### JavaScript Files

**Search Pattern**: Look for any JavaScript files that construct URLs with IDs

- [ ] Search all `.js` files in `webparent/backoffice/src/main/resources/static/js/`
- [ ] Search all `.js` files in `webparent/organization-portal/src/main/resources/static/js/`
- [ ] Search all `.js` files in `webparent/merchant-portal/src/main/resources/static/js/`
- [ ] Search all `.js` files in `webparent/client-portal/src/main/resources/static/js/`
- [ ] Search all `.js` files in `webparent/notification-service/src/main/resources/static/js/`

**Look for patterns like:**
- `/edit/${id}`
- `/view/${id}`
- `/delete/${id}`
- `+ id +`
- `.id` in AJAX calls

---

## Phase 6: Testing

### Unit Tests
- [ ] Test UUID generation in `IdBasedEntity`
- [ ] Test repository `findByUuid()` methods
- [ ] Test service UUID methods
- [ ] Test invalid UUID handling

### Integration Tests
- [ ] Test controller endpoints with valid UUIDs
- [ ] Test controller endpoints with invalid UUIDs
- [ ] Test controller endpoints with non-existent UUIDs
- [ ] Test CRUD operations end-to-end

### Frontend Tests
- [ ] Verify all edit links work with UUIDs
- [ ] Verify all view links work with UUIDs
- [ ] Verify all delete operations work with UUIDs
- [ ] Verify JavaScript AJAX calls work with UUIDs

### Security Tests
- [ ] Verify IDs are not exposed in URLs
- [ ] Verify authorization checks still work
- [ ] Test UUID enumeration resistance
- [ ] Test invalid UUID format handling

### Performance Tests
- [ ] Benchmark UUID-based queries
- [ ] Verify indexes are being used
- [ ] Test with large datasets

---

## Phase 7: Documentation Updates

- [ ] Update API documentation with UUID endpoints
- [ ] Update developer documentation
- [ ] Update deployment guides
- [ ] Create migration guide for external API consumers
- [ ] Update Postman/API testing collections

---

## Phase 8: Deployment

### Development Environment
- [ ] Run database migration
- [ ] Deploy updated code
- [ ] Smoke test all major features
- [ ] Verify logs for errors

### Staging Environment
- [ ] Run database migration
- [ ] Deploy updated code
- [ ] Full regression testing
- [ ] Performance testing
- [ ] Security testing

### Production Environment
- [ ] Create database backup
- [ ] Schedule maintenance window
- [ ] Run database migration
- [ ] Deploy updated code
- [ ] Monitor for errors
- [ ] Verify critical paths
- [ ] Monitor performance metrics

---

## Rollback Plan

In case of issues:

1. **Database Rollback** (if needed)
   ```sql
   -- Only if you need to remove UUID columns
   ALTER TABLE users DROP COLUMN uuid;
   ALTER TABLE merchants DROP COLUMN uuid;
   -- etc...
   ```

2. **Code Rollback**
   - Keep previous version tagged in Git
   - Redeploy previous version
   - UUID columns can remain (not used by old code)

3. **Testing After Rollback**
   - Verify system functionality
   - Check for any data inconsistencies

---

## Estimated Effort

### By Phase
- **Phase 1** (Foundation): ✅ Complete - 2 hours
- **Phase 2** (Repositories): ~4 hours (8 repositories × 30 min)
- **Phase 3** (Services): ~8 hours (10 services × 45 min)
- **Phase 4** (Controllers): ~12 hours (40 endpoints × 15 min)
- **Phase 5** (Frontend): ~8 hours (variable based on complexity)
- **Phase 6** (Testing): ~16 hours (thorough testing)
- **Phase 7** (Documentation): ~4 hours
- **Phase 8** (Deployment): ~6 hours

**Total Estimated Effort**: ~60 hours (1.5 weeks for one developer)

### Recommended Approach
- **Week 1**: Phases 1-3 (Foundation, Repositories, Services)
- **Week 2**: Phase 4-5 (Controllers, Frontend)
- **Week 3**: Phase 6-8 (Testing, Documentation, Deployment)

---

## Priority Order

### High Priority (Security Critical)
1. User management
2. Client management
3. Merchant management
4. Organization management

### Medium Priority
5. Vouchers
6. Transactions
7. Settlements
8. Payments

### Low Priority
9. Notifications
10. Events
11. Settings (Countries, States)

---

## Success Metrics

- [ ] Zero sequential IDs exposed in public URLs
- [ ] All entity endpoints use UUIDs
- [ ] No security vulnerabilities from ID enumeration
- [ ] Performance remains acceptable (<100ms per query)
- [ ] Zero data loss during migration
- [ ] All tests passing

---

## Notes

- Keep internal references (foreign keys) using integer IDs
- UUIDs are only for external/URL access
- Always perform authorization checks regardless of UUID usage
- Monitor logs for invalid UUID access attempts
- Consider rate limiting on UUID endpoints

---

## Questions/Decisions Needed

1. **API Versioning**: Should we version the API (v1 vs v2)?
   - [ ] Decision: ___________

2. **Backward Compatibility**: Support old ID URLs temporarily?
   - [ ] Decision: ___________

3. **External Integrations**: How many external systems use our APIs?
   - [ ] Count: ___________
   - [ ] Migration plan: ___________

4. **Rollout Strategy**: Big bang or gradual?
   - [ ] Decision: ___________

---

## Sign-off

- [ ] Development Lead Approval
- [ ] Security Team Review
- [ ] QA Team Approval
- [ ] DevOps Ready
- [ ] Product Owner Approval

---

**Last Updated**: [Date]
**Version**: 1.0
**Owner**: Development Team

