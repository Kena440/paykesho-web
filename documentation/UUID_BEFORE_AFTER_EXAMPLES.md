# UUID Implementation - Before & After Examples

Quick reference guide showing concrete code changes needed for UUID implementation.

---

## 1. Entity Layer

### ✅ ALREADY DONE
`IdBasedEntity.java` has been updated with UUID support.

```java
@Data
@MappedSuperclass
public abstract class IdBasedEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    protected Integer id;
    
    @Column(name = "uuid", unique = true, nullable = false, updatable = false, length = 36)
    private String uuid;
    
    @PrePersist
    public void generateUuid() {
        if (this.uuid == null) {
            this.uuid = UUID.randomUUID().toString();
        }
    }
}
```

---

## 2. Repository Layer

### BEFORE
```java
public interface MerchantRepository extends JpaRepository<Merchant, Integer> {
    Merchant getMerchantByEmail(String email);
    Merchant getMerchantByName(String name);
    Long countById(Integer id);
}
```

### AFTER
```java
public interface MerchantRepository extends JpaRepository<Merchant, Integer> {
    // Existing methods...
    Merchant getMerchantByEmail(String email);
    Merchant getMerchantByName(String name);
    Long countById(Integer id);
    
    // NEW: UUID-based methods for public access
    Optional<Merchant> findByUuid(String uuid);
    
    @Query("SELECT COUNT(m) > 0 FROM Merchant m WHERE m.uuid = ?1")
    boolean existsByUuid(String uuid);
}
```

---

## 3. Service Layer

### BEFORE
```java
@Service
@Transactional
public class MerchantService {
    
    @Autowired
    private MerchantRepository merchantRepository;
    
    public Merchant getMerchantById(Integer id) throws PaykeshoError {
        if (id == null) {
            throw new PaykeshoError("Null parameter id");
        }
        
        try {
            return merchantRepository.findById(id).get();
        } catch (NoSuchElementException exception) {
            throw new PaykeshoError("Could not find any merchant");
        }
    }
    
    public void updateMerchantStatus(Integer id, boolean enabled) throws PaykeshoError {
        if (id == null) {
            throw new PaykeshoError("Null parameter id");
        }
        
        Long countById = merchantRepository.countById(id);
        if (countById == null || countById == 0) {
            throw new PaykeshoError("Could not find any merchant");
        }
        
        merchantRepository.updateEnableStatus(id, enabled);
    }
}
```

### AFTER
```java
@Service
@Transactional
public class MerchantService {
    
    @Autowired
    private MerchantRepository merchantRepository;
    
    // Keep this for internal use (foreign keys, etc.)
    public Merchant getMerchantById(Integer id) throws PaykeshoError {
        if (id == null) {
            throw new PaykeshoError("Null parameter id");
        }
        
        try {
            Merchant merchant = merchantRepository.findById(id).get();
            
            // Process photoPath if needed
            if (merchant.getPhotoPath() != null && merchant.getRemoteFileId() != null) {
                FileAccessResponse fileAccessResponse = fileUploadService.getFileAccessDetails(merchant.getRemoteFileId());
                if (fileAccessResponse != null && fileAccessResponse.getFileUrl() != null) {
                    merchant.setPhotoPath(fileAccessResponse.getFileUrl());
                }
            }
            
            return merchant;
        } catch (NoSuchElementException exception) {
            throw new PaykeshoError("Could not find any merchant");
        }
    }
    
    // NEW: Use this for public/URL access
    public Merchant getMerchantByUuid(String uuid) throws PaykeshoError {
        if (uuid == null || uuid.trim().isEmpty()) {
            throw new PaykeshoError("Invalid UUID parameter");
        }
        
        // Validate UUID format to prevent injection
        try {
            UUID.fromString(uuid);
        } catch (IllegalArgumentException e) {
            throw new PaykeshoError("Invalid UUID format");
        }
        
        return merchantRepository.findByUuid(uuid)
            .map(merchant -> {
                // Process photoPath the same way
                if (merchant.getPhotoPath() != null && merchant.getRemoteFileId() != null) {
                    FileAccessResponse fileAccessResponse = fileUploadService.getFileAccessDetails(merchant.getRemoteFileId());
                    if (fileAccessResponse != null && fileAccessResponse.getFileUrl() != null) {
                        merchant.setPhotoPath(fileAccessResponse.getFileUrl());
                    }
                }
                return merchant;
            })
            .orElseThrow(() -> new PaykeshoError("Could not find merchant"));
    }
    
    // Keep this for internal use
    public void updateMerchantStatus(Integer id, boolean enabled) throws PaykeshoError {
        if (id == null) {
            throw new PaykeshoError("Null parameter id");
        }
        
        Long countById = merchantRepository.countById(id);
        if (countById == null || countById == 0) {
            throw new PaykeshoError("Could not find any merchant");
        }
        
        merchantRepository.updateEnableStatus(id, enabled);
    }
    
    // NEW: Use this for public/URL access
    public void updateMerchantStatusByUuid(String uuid, boolean enabled) throws PaykeshoError {
        Merchant merchant = getMerchantByUuid(uuid);
        updateMerchantStatus(merchant.getId(), enabled);
    }
}
```

---

## 4. Controller Layer

### BEFORE
```java
@Controller
@RequestMapping("/merchants")
public class MerchantController {
    
    @Autowired
    private MerchantService merchantService;
    
    @GetMapping("/edit/{id}")
    public String editMerchant(@PathVariable(name = "id") Integer id, 
                              RedirectAttributes redirectAttributes, 
                              Model model) {
        if (id == null) {
            return "redirect:/merchants";
        }
        
        String pageTitle = "Edit Merchant";
        
        try {
            Merchant merchant = merchantService.getMerchantById(id);
            List<State> stateList = settingService.listStatesByCountry("Zambia");
            
            model.addAttribute("merchant", merchant);
            model.addAttribute("stateList", stateList);
            model.addAttribute("pageTitle", pageTitle);
            
            return "merchants/merchant_form";
        } catch (PaykeshoError paykeshoError) {
            redirectAttributes.addFlashAttribute("message", paykeshoError.getMessage());
            return "redirect:/merchants";
        }
    }
    
    @GetMapping("/view/{id}")
    public String viewMerchant(@PathVariable(name = "id") Integer id, 
                              RedirectAttributes redirectAttributes, 
                              Model model, 
                              HttpServletRequest request) {
        try {
            Merchant merchant = merchantService.getMerchantById(id);
            model.addAttribute("merchant", merchant);
            return "merchants/merchant_detail";
        } catch (PaykeshoError paykeshoError) {
            redirectAttributes.addFlashAttribute("message", paykeshoError.getMessage());
            return "redirect:/merchants";
        }
    }
    
    @GetMapping("/{id}/enabled/{status}")
    public String updateMerchantEnabledStatus(@PathVariable(name = "id") Integer id, 
                                             @PathVariable(name = "status") boolean enabled, 
                                             RedirectAttributes redirectAttributes, 
                                             Model model) throws PaykeshoError {
        merchantService.updateMerchantStatus(id, enabled);
        redirectAttributes.addFlashAttribute("message", "Merchant status updated successfully");
        return "redirect:/merchants";
    }
}
```

### AFTER
```java
@Controller
@RequestMapping("/merchants")
public class MerchantController {
    
    @Autowired
    private MerchantService merchantService;
    
    @GetMapping("/edit/{uuid}")  // Changed from {id} to {uuid}
    public String editMerchant(@PathVariable(name = "uuid") String uuid,  // Changed type
                              RedirectAttributes redirectAttributes, 
                              Model model) {
        if (uuid == null || uuid.trim().isEmpty()) {  // Updated validation
            return "redirect:/merchants";
        }
        
        String pageTitle = "Edit Merchant";
        
        try {
            Merchant merchant = merchantService.getMerchantByUuid(uuid);  // Changed method
            List<State> stateList = settingService.listStatesByCountry("Zambia");
            
            model.addAttribute("merchant", merchant);
            model.addAttribute("stateList", stateList);
            model.addAttribute("pageTitle", pageTitle);
            
            return "merchants/merchant_form";
        } catch (PaykeshoError paykeshoError) {
            redirectAttributes.addFlashAttribute("message", paykeshoError.getMessage());
            return "redirect:/merchants";
        }
    }
    
    @GetMapping("/view/{uuid}")  // Changed from {id} to {uuid}
    public String viewMerchant(@PathVariable(name = "uuid") String uuid,  // Changed type
                              RedirectAttributes redirectAttributes, 
                              Model model, 
                              HttpServletRequest request) {
        try {
            Merchant merchant = merchantService.getMerchantByUuid(uuid);  // Changed method
            model.addAttribute("merchant", merchant);
            return "merchants/merchant_detail";
        } catch (PaykeshoError paykeshoError) {
            redirectAttributes.addFlashAttribute("message", paykeshoError.getMessage());
            return "redirect:/merchants";
        }
    }
    
    @GetMapping("/{uuid}/enabled/{status}")  // Changed from {id} to {uuid}
    public String updateMerchantEnabledStatus(@PathVariable(name = "uuid") String uuid,  // Changed type
                                             @PathVariable(name = "status") boolean enabled, 
                                             RedirectAttributes redirectAttributes, 
                                             Model model) throws PaykeshoError {
        merchantService.updateMerchantStatusByUuid(uuid, enabled);  // Changed method
        redirectAttributes.addFlashAttribute("message", "Merchant status updated successfully");
        return "redirect:/merchants";
    }
}
```

---

## 5. Thymeleaf Templates

### BEFORE
```html
<!-- merchants.html -->
<table class="table">
    <thead>
        <tr>
            <th>ID</th>
            <th>Name</th>
            <th>Type</th>
            <th>Actions</th>
        </tr>
    </thead>
    <tbody>
        <tr th:each="merchant : ${listMerchants}">
            <td th:text="${merchant.id}">1</td>
            <td th:text="${merchant.name}">Merchant Name</td>
            <td th:text="${merchant.merchantType}">Type</td>
            <td>
                <a th:href="@{'/merchants/edit/' + ${merchant.id}}" 
                   class="btn btn-primary btn-sm">
                    <i class="fas fa-edit"></i> Edit
                </a>
                <a th:href="@{'/merchants/view/' + ${merchant.id}}" 
                   class="btn btn-info btn-sm">
                    <i class="fas fa-eye"></i> View
                </a>
                <a th:href="@{'/merchants/' + ${merchant.id} + '/enabled/' + ${!merchant.enabled}}" 
                   class="btn btn-warning btn-sm">
                    <i class="fas fa-toggle-on"></i>
                    <span th:if="${merchant.enabled}">Disable</span>
                    <span th:unless="${merchant.enabled}">Enable</span>
                </a>
            </td>
        </tr>
    </tbody>
</table>
```

### AFTER
```html
<!-- merchants.html -->
<table class="table">
    <thead>
        <tr>
            <!-- Remove or hide ID column for security -->
            <!-- <th>ID</th> -->
            <th>Name</th>
            <th>Type</th>
            <th>Actions</th>
        </tr>
    </thead>
    <tbody>
        <tr th:each="merchant : ${listMerchants}">
            <!-- <td th:text="${merchant.id}">1</td> -->
            <td th:text="${merchant.name}">Merchant Name</td>
            <td th:text="${merchant.merchantType}">Type</td>
            <td>
                <a th:href="@{'/merchants/edit/' + ${merchant.uuid}}"  <!-- Changed to uuid -->
                   class="btn btn-primary btn-sm">
                    <i class="fas fa-edit"></i> Edit
                </a>
                <a th:href="@{'/merchants/view/' + ${merchant.uuid}}"  <!-- Changed to uuid -->
                   class="btn btn-info btn-sm">
                    <i class="fas fa-eye"></i> View
                </a>
                <a th:href="@{'/merchants/' + ${merchant.uuid} + '/enabled/' + ${!merchant.enabled}}"  <!-- Changed to uuid -->
                   class="btn btn-warning btn-sm">
                    <i class="fas fa-toggle-on"></i>
                    <span th:if="${merchant.enabled}">Disable</span>
                    <span th:unless="${merchant.enabled}">Enable</span>
                </a>
            </td>
        </tr>
    </tbody>
</table>
```

---

## 6. JavaScript/AJAX Calls

### BEFORE
```javascript
// merchants.js
function deleteMerchant(merchantId) {
    if (confirm('Are you sure you want to delete this merchant?')) {
        $.ajax({
            url: `/api/merchants/${merchantId}`,
            type: 'DELETE',
            success: function(result) {
                showSuccessMessage('Merchant deleted successfully');
                location.reload();
            },
            error: function(error) {
                showErrorMessage('Failed to delete merchant');
            }
        });
    }
}

function editMerchant(merchantId) {
    window.location.href = `/merchants/edit/${merchantId}`;
}

// Event handler
$('.edit-merchant').on('click', function() {
    const merchantId = $(this).data('merchant-id');
    editMerchant(merchantId);
});
```

### AFTER
```javascript
// merchants.js
function deleteMerchant(merchantUuid) {  // Changed parameter name
    if (confirm('Are you sure you want to delete this merchant?')) {
        $.ajax({
            url: `/api/merchants/${merchantUuid}`,  // Changed to use UUID
            type: 'DELETE',
            success: function(result) {
                showSuccessMessage('Merchant deleted successfully');
                location.reload();
            },
            error: function(error) {
                showErrorMessage('Failed to delete merchant');
            }
        });
    }
}

function editMerchant(merchantUuid) {  // Changed parameter name
    window.location.href = `/merchants/edit/${merchantUuid}`;  // Changed to use UUID
}

// Event handler
$('.edit-merchant').on('click', function() {
    const merchantUuid = $(this).data('merchant-uuid');  // Changed data attribute
    editMerchant(merchantUuid);
});
```

### Corresponding HTML Change
```html
<!-- BEFORE -->
<button class="btn btn-primary edit-merchant" 
        data-merchant-id="${merchant.id}">
    Edit
</button>

<!-- AFTER -->
<button class="btn btn-primary edit-merchant" 
        data-merchant-uuid="${merchant.uuid}">
    Edit
</button>
```

---

## 7. REST API Controller

### BEFORE
```java
@RestController
@RequestMapping("/api/clients")
public class ClientAPIController {
    
    @Autowired
    private ClientService clientService;
    
    @Autowired
    private VoucherService voucherService;
    
    @GetMapping("/{id}/vouchers")
    public ResponseEntity<?> getClientVouchers(@PathVariable("id") Integer clientId) {
        try {
            Client client = clientService.get(clientId);
            List<Voucher> vouchers = voucherService.getVouchersByClient(client);
            return ResponseEntity.ok(vouchers);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body("Client not found");
        }
    }
    
    @GetMapping("/{id}/transactions")
    public ResponseEntity<?> getClientTransactions(@PathVariable("id") Integer clientId) {
        try {
            Client client = clientService.get(clientId);
            List<Transaction> transactions = transactionService.getTransactionsByClient(client);
            return ResponseEntity.ok(transactions);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body("Client not found");
        }
    }
}
```

### AFTER
```java
@RestController
@RequestMapping("/api/clients")
public class ClientAPIController {
    
    @Autowired
    private ClientService clientService;
    
    @Autowired
    private VoucherService voucherService;
    
    @GetMapping("/{uuid}/vouchers")  // Changed from {id} to {uuid}
    public ResponseEntity<?> getClientVouchers(@PathVariable("uuid") String clientUuid) {  // Changed type
        try {
            Client client = clientService.getByUuid(clientUuid);  // Changed method
            List<Voucher> vouchers = voucherService.getVouchersByClient(client);
            return ResponseEntity.ok(vouchers);
        } catch (PaykeshoError e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "Client not found"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "Invalid UUID format"));
        }
    }
    
    @GetMapping("/{uuid}/transactions")  // Changed from {id} to {uuid}
    public ResponseEntity<?> getClientTransactions(@PathVariable("uuid") String clientUuid) {  // Changed type
        try {
            Client client = clientService.getByUuid(clientUuid);  // Changed method
            List<Transaction> transactions = transactionService.getTransactionsByClient(client);
            return ResponseEntity.ok(transactions);
        } catch (PaykeshoError e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "Client not found"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "Invalid UUID format"));
        }
    }
}
```

---

## 8. Database Migration

### Run This Once
```sql
-- Add UUID column to merchants table
ALTER TABLE merchants ADD COLUMN uuid VARCHAR(36) UNIQUE;

-- Generate UUIDs for existing records
UPDATE merchants SET uuid = UUID() WHERE uuid IS NULL;

-- Make UUID column NOT NULL
ALTER TABLE merchants MODIFY COLUMN uuid VARCHAR(36) NOT NULL;

-- Add index for performance
CREATE INDEX idx_merchants_uuid ON merchants(uuid);
```

---

## 9. Testing Examples

### Unit Test - Service Layer

#### BEFORE
```java
@Test
public void testGetMerchantById() throws PaykeshoError {
    // Arrange
    Integer merchantId = 1;
    Merchant mockMerchant = new Merchant();
    mockMerchant.setId(merchantId);
    mockMerchant.setName("Test Merchant");
    
    when(merchantRepository.findById(merchantId))
        .thenReturn(Optional.of(mockMerchant));
    
    // Act
    Merchant result = merchantService.getMerchantById(merchantId);
    
    // Assert
    assertNotNull(result);
    assertEquals(merchantId, result.getId());
    assertEquals("Test Merchant", result.getName());
}
```

#### AFTER (Add New Test)
```java
@Test
public void testGetMerchantByUuid() throws PaykeshoError {
    // Arrange
    String merchantUuid = "550e8400-e29b-41d4-a716-446655440000";
    Merchant mockMerchant = new Merchant();
    mockMerchant.setId(1);
    mockMerchant.setUuid(merchantUuid);
    mockMerchant.setName("Test Merchant");
    
    when(merchantRepository.findByUuid(merchantUuid))
        .thenReturn(Optional.of(mockMerchant));
    
    // Act
    Merchant result = merchantService.getMerchantByUuid(merchantUuid);
    
    // Assert
    assertNotNull(result);
    assertEquals(merchantUuid, result.getUuid());
    assertEquals("Test Merchant", result.getName());
}

@Test
public void testGetMerchantByUuid_InvalidFormat() {
    // Arrange
    String invalidUuid = "not-a-valid-uuid";
    
    // Act & Assert
    assertThrows(PaykeshoError.class, () -> {
        merchantService.getMerchantByUuid(invalidUuid);
    });
}

@Test
public void testGetMerchantByUuid_NotFound() {
    // Arrange
    String merchantUuid = "550e8400-e29b-41d4-a716-446655440000";
    
    when(merchantRepository.findByUuid(merchantUuid))
        .thenReturn(Optional.empty());
    
    // Act & Assert
    assertThrows(PaykeshoError.class, () -> {
        merchantService.getMerchantByUuid(merchantUuid);
    });
}
```

### Integration Test - Controller

#### BEFORE
```java
@Test
public void testEditMerchantEndpoint() throws Exception {
    // Arrange
    Integer merchantId = 1;
    
    // Act & Assert
    mockMvc.perform(get("/merchants/edit/" + merchantId))
        .andExpect(status().isOk())
        .andExpect(view().name("merchants/merchant_form"))
        .andExpect(model().attributeExists("merchant"));
}
```

#### AFTER (Add New Test)
```java
@Test
public void testEditMerchantEndpoint_WithUuid() throws Exception {
    // Arrange
    String merchantUuid = "550e8400-e29b-41d4-a716-446655440000";
    
    // Act & Assert
    mockMvc.perform(get("/merchants/edit/" + merchantUuid))
        .andExpect(status().isOk())
        .andExpect(view().name("merchants/merchant_form"))
        .andExpect(model().attributeExists("merchant"));
}

@Test
public void testEditMerchantEndpoint_InvalidUuid() throws Exception {
    // Arrange
    String invalidUuid = "invalid-uuid";
    
    // Act & Assert
    mockMvc.perform(get("/merchants/edit/" + invalidUuid))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/merchants"));
}
```

---

## 10. Authorization Example (IMPORTANT!)

### Don't Forget Authorization Checks!

#### BEFORE (Potentially Vulnerable)
```java
@GetMapping("/edit/{id}")
public String editMerchant(@PathVariable Integer id, Model model) {
    Merchant merchant = merchantService.getMerchantById(id);
    model.addAttribute("merchant", merchant);
    return "merchants/merchant_form";
}
```

#### AFTER (Secure with Authorization)
```java
@GetMapping("/edit/{uuid}")
public String editMerchant(@PathVariable String uuid, 
                          Model model, 
                          Authentication authentication) throws PaykeshoError {
    // Get the merchant by UUID
    Merchant merchant = merchantService.getMerchantByUuid(uuid);
    
    // IMPORTANT: Check authorization
    if (!authorizationService.canEditMerchant(authentication, merchant)) {
        throw new AccessDeniedException("You don't have permission to edit this merchant");
    }
    
    model.addAttribute("merchant", merchant);
    return "merchants/merchant_form";
}
```

**Remember**: UUID obscurity is NOT a security control. Always implement proper authorization!

---

## Summary of Changes

| Layer | File Type | Change Type | Example |
|-------|-----------|-------------|---------|
| **Entity** | `.java` | ✅ Done | Added `uuid` field to `IdBasedEntity` |
| **Repository** | `.java` | Add methods | `findByUuid(String uuid)` |
| **Service** | `.java` | Add methods | `getMerchantByUuid(String uuid)` |
| **Controller** | `.java` | Change path & type | `/{id}` → `/{uuid}`, `Integer` → `String` |
| **Templates** | `.html` | Change references | `${entity.id}` → `${entity.uuid}` |
| **JavaScript** | `.js` | Change variables | `entityId` → `entityUuid` |
| **Database** | `.sql` | Add column | `ALTER TABLE ... ADD COLUMN uuid` |

---

## Quick Search & Replace Patterns

Use these patterns to find areas needing updates:

### In Java Controllers:
- Search: `@PathVariable.*Integer.*id`
- Check if it should be: `@PathVariable.*String.*uuid`

### In Thymeleaf:
- Search: `\$\{.*\.id\}`
- Check if it should be: `${*.uuid}`

### In JavaScript:
- Search: `Id` variable names
- Check if it should be: `Uuid`

### In URLs:
- Search: `/edit/{id}`
- Change to: `/edit/{uuid}`

---

**Remember**: Test thoroughly at each step! Start with one entity, verify it works end-to-end, then proceed to others.

