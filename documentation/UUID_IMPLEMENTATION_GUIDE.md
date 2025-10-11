# UUID Implementation Guide for Paykesho

## Overview
This guide provides a comprehensive approach to implementing UUIDs in your application to enhance security by preventing IDOR (Insecure Direct Object Reference) vulnerabilities.

## Benefits
- **Security**: Prevents attackers from enumerating resources (/user/1, /user/2, etc.)
- **Privacy**: Hides the actual number of entities in your system
- **Scalability**: UUIDs can be generated independently in distributed systems
- **Best Practice**: Follows OWASP security recommendations

---

## Implementation Steps

### 1. Database Migration
Run the `add_uuid_columns.sql` script to add UUID columns to all tables and populate existing records.

### 2. Repository Layer

Add methods to find entities by UUID in your repositories.

**Example: MerchantRepository**
```java
package org.paykesho.backoffice.merchants.repository;

import org.commons.entity.merchant.Merchant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface MerchantRepository extends JpaRepository<Merchant, Integer> {
    
    // Existing methods...
    
    /**
     * Find merchant by UUID (for secure URL access)
     */
    Optional<Merchant> findByUuid(String uuid);
    
    /**
     * Check if merchant exists by UUID
     */
    @Query("SELECT COUNT(m) > 0 FROM Merchant m WHERE m.uuid = ?1")
    boolean existsByUuid(String uuid);
}
```

**Example: UserRepository**
```java
public interface UserRepository extends JpaRepository<User, Integer> {
    
    // Existing methods...
    
    Optional<User> findByUuid(String uuid);
    
    @Query("SELECT COUNT(u) > 0 FROM User u WHERE u.uuid = ?1")
    boolean existsByUuid(String uuid);
}
```

**Example: OrganizationRepository**
```java
public interface OrganizationRepository extends JpaRepository<Organization, Integer> {
    
    // Existing methods...
    
    Optional<Organization> findByUuid(String uuid);
    
    @Query("SELECT COUNT(o) > 0 FROM Organization o WHERE o.uuid = ?1")
    boolean existsByUuid(String uuid);
}
```

**Example: ClientRepository**
```java
public interface ClientRepository extends JpaRepository<Client, Integer> {
    
    // Existing methods...
    
    Optional<Client> findByUuid(String uuid);
    
    @Query("SELECT COUNT(c) > 0 FROM Client c WHERE c.uuid = ?1")
    boolean existsByUuid(String uuid);
}
```

---

### 3. Service Layer

Update service methods to support UUID-based lookups.

**Example: MerchantService**
```java
@Service
@Transactional
public class MerchantService {
    
    @Autowired
    private MerchantRepository merchantRepository;
    
    // Keep existing method for internal use
    public Merchant getMerchantById(Integer id) throws PaykeshoError {
        if (id == null) {
            throw new PaykeshoError("Null parameter id");
        }
        return merchantRepository.findById(id)
            .orElseThrow(() -> new PaykeshoError("Could not find merchant"));
    }
    
    // NEW: Add UUID-based method for public/URL access
    public Merchant getMerchantByUuid(String uuid) throws PaykeshoError {
        if (uuid == null || uuid.trim().isEmpty()) {
            throw new PaykeshoError("Invalid UUID parameter");
        }
        
        return merchantRepository.findByUuid(uuid)
            .orElseThrow(() -> new PaykeshoError("Could not find merchant with UUID: " + uuid));
    }
    
    // NEW: Update status by UUID
    public void updateMerchantStatusByUuid(String uuid, boolean enabled) throws PaykeshoError {
        Merchant merchant = getMerchantByUuid(uuid);
        merchantRepository.updateEnableStatus(merchant.getId(), enabled);
    }
}
```

**Example: UserService**
```java
@Service
@Transactional
public class UserService {
    
    @Autowired
    private UserRepository userRepository;
    
    // Keep existing method for internal use
    public User getUserById(Integer id) throws PaykeshoError {
        return userRepository.findById(id)
            .orElseThrow(() -> new PaykeshoError("Could not find user"));
    }
    
    // NEW: Add UUID-based method
    public User getUserByUuid(String uuid) throws PaykeshoError {
        if (uuid == null || uuid.trim().isEmpty()) {
            throw new PaykeshoError("Invalid UUID parameter");
        }
        
        return userRepository.findByUuid(uuid)
            .orElseThrow(() -> new PaykeshoError("Could not find user"));
    }
}
```

---

### 4. Controller Layer

Update controllers to use UUIDs in URLs instead of IDs.

**Example: MerchantController - BEFORE**
```java
@GetMapping("/edit/{id}")
public String editMerchant(@PathVariable(name = "id") Integer id, 
                          RedirectAttributes redirectAttributes, 
                          Model model) {
    try {
        Merchant merchant = merchantService.getMerchantById(id);
        model.addAttribute("merchant", merchant);
        return "merchants/merchant_form";
    } catch (PaykeshoError e) {
        redirectAttributes.addFlashAttribute("message", e.getMessage());
        return "redirect:/merchants";
    }
}
```

**Example: MerchantController - AFTER (Using UUID)**
```java
@GetMapping("/edit/{uuid}")
public String editMerchant(@PathVariable(name = "uuid") String uuid, 
                          RedirectAttributes redirectAttributes, 
                          Model model) {
    try {
        Merchant merchant = merchantService.getMerchantByUuid(uuid);
        model.addAttribute("merchant", merchant);
        return "merchants/merchant_form";
    } catch (PaykeshoError e) {
        redirectAttributes.addFlashAttribute("message", e.getMessage());
        return "redirect:/merchants";
    }
}

@GetMapping("/view/{uuid}")
public String viewMerchant(@PathVariable(name = "uuid") String uuid, 
                          RedirectAttributes redirectAttributes, 
                          Model model) {
    try {
        Merchant merchant = merchantService.getMerchantByUuid(uuid);
        model.addAttribute("merchant", merchant);
        return "merchants/merchant_detail";
    } catch (PaykeshoError e) {
        redirectAttributes.addFlashAttribute("message", e.getMessage());
        return "redirect:/merchants";
    }
}

@GetMapping("/{uuid}/enabled/{status}")
public String updateMerchantEnabledStatus(@PathVariable(name = "uuid") String uuid, 
                                         @PathVariable(name = "status") boolean enabled, 
                                         RedirectAttributes redirectAttributes) 
                                         throws PaykeshoError {
    merchantService.updateMerchantStatusByUuid(uuid, enabled);
    redirectAttributes.addFlashAttribute("message", "Merchant status updated successfully");
    return "redirect:/merchants";
}
```

**Example: UserController**
```java
@GetMapping("/edit/{uuid}")
public String editUser(@PathVariable(name = "uuid") String uuid, 
                      RedirectAttributes redirectAttributes, 
                      Model model) throws PaykeshoError {
    try {
        User user = userService.getUserByUuid(uuid);
        List<Role> roleList = userService.listRoles();
        
        model.addAttribute("user", user);
        model.addAttribute("roleList", roleList);
        model.addAttribute("pageTitle", "Edit User");
        
        return "users/user_form";
    } catch (PaykeshoError e) {
        redirectAttributes.addFlashAttribute("message", e.getMessage());
        return "redirect:/users";
    }
}

@GetMapping("/details/{uuid}")
public String viewUserDetails(@PathVariable("uuid") String uuid, 
                             Model model, 
                             RedirectAttributes redirectAttributes) 
                             throws PaykeshoError {
    try {
        User user = userService.getUserByUuid(uuid);
        model.addAttribute("user", user);
        return "users/user_detail";
    } catch (PaykeshoError e) {
        redirectAttributes.addFlashAttribute("message", e.getMessage());
        return "redirect:/users";
    }
}
```

**Example: OrganizationController**
```java
@GetMapping("/edit/{uuid}")
public String editOrganization(@PathVariable(name = "uuid") String uuid, 
                              RedirectAttributes redirectAttributes, 
                              Model model) {
    try {
        Organization organization = organizationService.getOrganizationByUuid(uuid);
        List<State> stateList = settingService.listStatesByCountry("Zambia");
        
        model.addAttribute("organization", organization);
        model.addAttribute("stateList", stateList);
        model.addAttribute("pageTitle", "Edit Organization");
        
        return "organizations/organization_form";
    } catch (PaykeshoError e) {
        redirectAttributes.addFlashAttribute("message", e.getMessage());
        return "redirect:/organizations";
    }
}
```

---

### 5. Frontend/Template Layer

Update Thymeleaf templates and JavaScript to use UUIDs instead of IDs.

**Thymeleaf Templates - BEFORE**
```html
<a th:href="@{'/merchants/edit/' + ${merchant.id}}" class="btn btn-primary">
    Edit
</a>
<a th:href="@{'/merchants/view/' + ${merchant.id}}" class="btn btn-info">
    View
</a>
```

**Thymeleaf Templates - AFTER**
```html
<a th:href="@{'/merchants/edit/' + ${merchant.uuid}}" class="btn btn-primary">
    Edit
</a>
<a th:href="@{'/merchants/view/' + ${merchant.uuid}}" class="btn btn-info">
    View
</a>
```

**JavaScript AJAX - BEFORE**
```javascript
function deleteMerchant(merchantId) {
    $.ajax({
        url: `/api/merchants/${merchantId}`,
        type: 'DELETE',
        success: function(result) {
            // handle success
        }
    });
}
```

**JavaScript AJAX - AFTER**
```javascript
function deleteMerchant(merchantUuid) {
    $.ajax({
        url: `/api/merchants/${merchantUuid}`,
        type: 'DELETE',
        success: function(result) {
            // handle success
        }
    });
}
```

---

## REST API Controllers

For REST APIs, also use UUIDs in the endpoints.

**Example: API Controller - BEFORE**
```java
@GetMapping("/api/clients/{id}/vouchers")
public ResponseEntity<?> getClientVouchers(@PathVariable("id") Integer clientId) {
    // ...
}
```

**Example: API Controller - AFTER**
```java
@GetMapping("/api/clients/{uuid}/vouchers")
public ResponseEntity<?> getClientVouchers(@PathVariable("uuid") String clientUuid) {
    try {
        Client client = clientService.getClientByUuid(clientUuid);
        List<Voucher> vouchers = voucherService.getVouchersByClient(client);
        return ResponseEntity.ok(vouchers);
    } catch (PaykeshoError e) {
        return ResponseEntity.notFound().build();
    }
}
```

---

## Migration Strategy

### Phase 1: Add UUID Support (Non-Breaking)
1. ✅ Add UUID column to `IdBasedEntity`
2. ✅ Run database migration script
3. Add `findByUuid()` methods to all repositories
4. Add UUID-based service methods alongside existing ID methods
5. Test thoroughly

### Phase 2: Update Controllers (Breaking Changes)
1. Update one controller at a time
2. Change URL patterns from `/{id}` to `/{uuid}`
3. Update corresponding service calls
4. Update frontend templates for that controller
5. Test each controller thoroughly before moving to next

### Phase 3: Update REST APIs
1. Version your API if needed (e.g., `/api/v2/...`)
2. Update API endpoints to use UUIDs
3. Update API documentation
4. Notify API consumers of changes

### Phase 4: Cleanup (Optional)
1. Keep ID-based methods for internal use and database relationships
2. Remove ID-based public endpoints
3. Update all frontend code

---

## Entities to Update

Based on your codebase analysis, these entities need UUID implementation:

### High Priority (User-Facing)
- ✅ **User** - Admin users
- ✅ **Merchant** - Merchant records
- ✅ **Organization** - Organization records
- ✅ **Client** - Client/customer records
- **Voucher** - Voucher records
- **Transaction** - Transaction records
- **Settlement** - Settlement records

### Medium Priority
- **Payment** - Payment records
- **EmployeeRecord** - Employee records
- **Notification** - Notification records
- **NotificationQueue** - Queued notifications

### Low Priority (Internal/Settings)
- **Announcement** - System announcements
- **State** - Geographic states
- **Country** - Countries
- **Event** - Events

---

## Testing Checklist

- [ ] All entities generate UUIDs on creation
- [ ] Existing records have UUIDs populated
- [ ] UUID-based repository methods work correctly
- [ ] UUID-based service methods work correctly
- [ ] Controller endpoints accept UUIDs
- [ ] Frontend links use UUIDs
- [ ] JavaScript/AJAX calls use UUIDs
- [ ] REST API endpoints use UUIDs
- [ ] Error handling for invalid UUIDs
- [ ] Performance testing (indexed UUID queries)

---

## Security Considerations

### ✅ What This Fixes
- Prevents ID enumeration attacks
- Makes resource guessing much harder
- Obscures system scale information

### ⚠️ Still Needed
- **Authorization checks**: Always verify user has permission to access resource
- **Input validation**: Validate UUID format before database query
- **Rate limiting**: Prevent brute-force UUID guessing
- **Logging**: Log suspicious access patterns

**Example Authorization Check:**
```java
@GetMapping("/edit/{uuid}")
public String editMerchant(@PathVariable String uuid, Authentication auth) {
    Merchant merchant = merchantService.getMerchantByUuid(uuid);
    
    // IMPORTANT: Check if current user has permission to edit this merchant
    if (!authorizationService.canEditMerchant(auth, merchant)) {
        throw new AccessDeniedException("You don't have permission to edit this merchant");
    }
    
    // ... continue with edit logic
}
```

---

## Performance Tips

1. **Indexing**: UUID columns are already indexed in the migration script
2. **Caching**: Consider caching UUID → Entity mappings for frequently accessed entities
3. **Database**: UUID queries with proper indexes are very fast
4. **String vs UUID type**: Using String (VARCHAR(36)) is fine and widely compatible

---

## Example Files to Update

Based on analysis, these files need updates:

### Controllers
- `MerchantController.java` - 4 endpoints
- `UserController.java` - 3 endpoints
- `OrganizationController.java` - 7 endpoints
- `ClientController.java` - 3 endpoints
- `VoucherController.java` - 3 endpoints
- `TransactionController.java` - 1 endpoint
- `SettlementController.java` - 1 endpoint
- `NotificationQueueController.java` - 4 endpoints

### Services
- `MerchantService.java`
- `UserService.java`
- `OrganizationService.java`
- `ClientService.java`
- `VoucherService.java`
- Others as needed

### Templates
- All Thymeleaf templates with edit/view/delete links
- All JavaScript files making API calls with IDs

---

## Common Pitfalls to Avoid

1. **❌ Don't expose IDs alongside UUIDs** - This defeats the purpose
2. **❌ Don't use UUIDs for foreign keys** - Keep using IDs for relationships
3. **❌ Don't forget authorization** - UUID ≠ security, still need proper auth
4. **❌ Don't skip database indexes** - UUID queries need indexes to be fast
5. **✅ Do validate UUID format** - Prevent injection attacks

**UUID Validation Example:**
```java
private boolean isValidUUID(String uuid) {
    if (uuid == null) return false;
    try {
        UUID.fromString(uuid);
        return true;
    } catch (IllegalArgumentException e) {
        return false;
    }
}
```

---

## Next Steps

1. Review this implementation guide
2. Run the database migration script
3. Start with one entity (e.g., Merchant) as a pilot
4. Update Repository → Service → Controller → Frontend
5. Test thoroughly
6. Repeat for other entities
7. Update documentation and API specs

---

## Questions to Consider

1. **Versioning**: Do you want to version your API (v1 with IDs, v2 with UUIDs)?
2. **Backward Compatibility**: Do you need to support old ID-based URLs temporarily?
3. **External Integrations**: Do external systems call your APIs with IDs?
4. **Rollout Strategy**: Big bang or gradual rollout?

---

## Support & Troubleshooting

### Issue: "Duplicate entry for UUID"
**Solution**: Ensure `@PrePersist` method is being called, or manually set UUID before saving.

### Issue: "Slow UUID queries"
**Solution**: Verify indexes are created on UUID columns.

### Issue: "Frontend still showing IDs"
**Solution**: Search codebase for `${entity.id}` and replace with `${entity.uuid}`.

### Issue: "Invalid UUID format errors"
**Solution**: Add UUID validation in controllers before calling services.

---

## Conclusion

Implementing UUIDs is a significant security improvement that will protect your users' data. While it requires systematic updates across multiple layers, the security benefits are substantial. Take it one entity at a time, test thoroughly, and you'll have a much more secure system.

Good luck with the implementation! 🔒

