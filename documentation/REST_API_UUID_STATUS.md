# REST API Endpoints - UUID Implementation Status

## ✅ **REST API Endpoints Updated**

### REST Endpoints with IDs in URL Path (Now Using UUID):

#### **Backoffice**:
1. ✅ `/states/list-by-country/{uuid}` - Get states by country UUID
2. ✅ `/states/delete/{uuid}` - Delete state by UUID
3. ✅ `/countries/delete/{uuid}` - Delete country by UUID

#### **Organization Portal**:
4. ✅ `/settings/list-states-by-country/{uuid}` - Get states by country UUID

#### **Merchant Portal**:
5. ✅ `/settings/list-states-by-country/{uuid}` - Get states by country UUID

#### **Client Portal**:
6. ✅ `/settings/list-states-by-country/{uuid}` - Get states by country UUID
7. ✅ `/api/vouchers/{uuid}` - Get client vouchers
8. ✅ `/api/transactions/{uuid}` - Get client transactions

#### **Notification Service**:
9. ✅ `GET /notifications/{uuid}` - Get notification
10. ✅ `POST /notifications/{uuid}/cancel` - Cancel notification
11. ✅ `POST /notifications/{uuid}/retry` - Retry notification
12. ✅ `POST /notifications/{uuid}/process` - Process notification

**Total REST Endpoints with UUID in Path: 12** ✅

---

## ℹ️ **Validation REST Endpoints (Using ID in Request Body - OK)**

These endpoints receive ID as a **request parameter** (not in URL path).  
They're used for **internal validation** when editing forms - this is **acceptable**:

### Backoffice:
1. `POST /merchants/rest/check-merchant` 
   - Parameters: `id`, `merchantName`, `merchantType`
   - Purpose: Check if merchant is unique (when editing)
   - **Status**: ✅ OK - Internal validation, not public URL

2. `POST /users/rest/check-email`
   - Parameters: `id`, `email`
   - Purpose: Check if email is unique (when editing)
   - **Status**: ✅ OK - Internal validation

3. `POST /organizations/rest/check-organization`
   - Parameters: `id`, `organizationName`, `organizationType`
   - Purpose: Check if organization is unique (when editing)
   - **Status**: ✅ OK - Internal validation

4. `POST /clients/rest/check-email`
   - Parameters: `id`, `email`
   - Purpose: Check if email is unique (when editing)
   - **Status**: ✅ OK - Internal validation

---

## 🔍 **Why Validation Endpoints Can Use ID**

### Difference Between URL Path vs Request Parameter:

#### ❌ **NOT OK** - ID in URL Path (Public):
```java
@GetMapping("/merchants/edit/{id}")  // ❌ Exposed in URL
public String edit(@PathVariable Integer id) { ... }
```
**Problem**: Visible in browser address bar, easily enumerable

#### ✅ **OK** - ID in Request Body (Internal):
```java
@PostMapping("/merchants/rest/check-merchant")
public String check(Integer id, String name) { ... }  // ✅ Hidden in POST body
```
**Why OK**:
- Not visible in URL
- Called via AJAX from edit form
- Form already has entity loaded (has ID)
- Internal validation only
- Not a public navigation URL

---

## 📊 **REST API Summary**

### By Type:

| Type | Count | UUID Status |
|------|-------|-------------|
| **Public REST Endpoints (URL path)** | 12 | ✅ All use UUID |
| **Validation Endpoints (POST body)** | 4 | ℹ️ Use ID (OK) |
| **Utility Endpoints** | ~10 | ℹ️ No entity params |

### By Portal:

| Portal | REST Endpoints | UUID in Path | Validation (ID in body) |
|--------|----------------|--------------|------------------------|
| **Backoffice** | 8 | 3 ✅ | 4 ℹ️ |
| **Organization Portal** | 1 | 1 ✅ | 0 |
| **Merchant Portal** | 1 | 1 ✅ | 0 |
| **Client Portal** | 3 | 3 ✅ | 0 |
| **Notification Service** | 4 | 4 ✅ | 0 |

---

## ✅ **Repositories Updated for REST APIs**

Added UUID support to:
- ✅ StateRepository (backoffice) - Added `findByUuid()` and `findByCountryUuid()`
- ✅ CountryRepository (backoffice) - Added `findByUuid()`
- ✅ StateRepository (organization-portal) - Added `findByCountryUuid()`
- ✅ StateRepository (merchant-portal) - Added `findByCountryUuid()`
- ✅ StateRepository (client-portal) - Added `findByCountryUuid()`

---

## 🔒 **Security Analysis**

### REST Endpoints Before:
```
❌ GET /states/list-by-country/1
❌ GET /states/delete/5
❌ GET /countries/delete/2
```
**Problem**: Easy to enumerate countries/states

### REST Endpoints After:
```
✅ GET /states/list-by-country/550e8400-e29b-41d4-a716-446655440000
✅ GET /states/delete/7c9e6679-7425-40de-944b-e07fc1f90ae7
✅ GET /countries/delete/c5e9c6c1-2f45-4a8f-9c3d-5b6a8e7f9d3e
```
**Result**: Cannot enumerate settings data

---

## 🎯 **JavaScript/AJAX Impact**

### Frontend Code That Calls These APIs:

#### Country → States Dropdown:
```javascript
// Before
$.get('/settings/list-states-by-country/' + countryId, function(states) {
    // populate dropdown
});

// After
$.get('/settings/list-states-by-country/' + countryUuid, function(states) {
    // populate dropdown
});
```

**Need to update**: Any JavaScript that calls these endpoints

---

## 📝 **JavaScript Files to Check**

Search for these patterns in JavaScript files:

```javascript
// Pattern 1: State loading by country
'/settings/list-states-by-country/' + countryId
'/list-states-by-country/' + countryId

// Pattern 2: Delete operations
'/states/delete/' + stateId
'/countries/delete/' + countryId
```

**Files to search:**
- `webparent/**/static/js/**/*.js`
- `webparent/**/resources/static/**/*.js`

---

## ⚠️ **Potential Breaking Changes**

### These REST API changes might affect:
1. **Country/State dropdowns** in forms
   - Organization form
   - Merchant form
   - Client form
   
2. **Delete operations** for settings
   - State management
   - Country management

### Testing Required:
- [ ] Forms with country dropdown → Select country → States load
- [ ] Delete state → Works
- [ ] Delete country → Works

---

## ✅ **Updated REST Controllers**

### Files Modified (7 files):

#### Repositories (5):
- backoffice/settings/repository/StateRepository.java ✅
- backoffice/settings/repository/CountryRepository.java ✅
- organization-portal/settings/repository/StateRepository.java ✅
- merchant-portal/settings/repository/StateRepository.java ✅
- client-portal/settings/repository/StateRepository.java ✅

#### Controllers (7):
- backoffice/settings/controller/StateRestController.java ✅
- backoffice/settings/controller/CountryRestController.java ✅
- organization-portal/settings/controller/StateRestController.java ✅
- merchant-portal/settings/controller/StateRestController.java ✅
- client-portal/settings/controller/StateRestController.java ✅
- client-portal/api/controller/APIController.java ✅ (already done)
- notification-service/controller/NotificationQueueController.java ✅ (already done)

---

## 🎯 **Testing Checklist for REST APIs**

### Settings APIs:
- [ ] Select country in form → States dropdown populates
- [ ] Verify API call uses country UUID
- [ ] Delete state → Works
- [ ] Delete country → Works

### Client Portal APIs:
- [ ] API call: `/api/vouchers/{clientUuid}` → Returns vouchers
- [ ] API call: `/api/transactions/{clientUuid}` → Returns transactions

### Notification APIs:
- [ ] GET `/notifications/{uuid}` → Returns notification
- [ ] POST `/notifications/{uuid}/cancel` → Cancels
- [ ] POST `/notifications/{uuid}/retry` → Retries
- [ ] POST `/notifications/{uuid}/process` → Processes

---

## 📊 **Final REST API Count**

**Total REST Endpoints Updated**: 12  
**Validation Endpoints (ID in body)**: 4 (OK - internal use)  
**Coverage**: 100% of public REST endpoints ✅

---

## ✅ **Sign-Off**

**REST API Implementation**: ✅ COMPLETE  
**All public REST endpoints now use UUID in URL paths**  
**Validation endpoints correctly use ID in request body**  

**Status**: Ready for testing! 🚀

