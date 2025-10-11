# JAR Size Optimization Summary

## Problem Identified
Your JAR files were extremely large:
- **backoffice**: 167 MB
- **client-portal**: 148 MB  
- **payment-service**: 130 MB

## Root Causes Found

### 1. ❌ OpenCV (72.47 MB) - **NOT USED**
- Included in parent POM (`webparent/pom.xml`)
- Pulled into ALL modules unnecessarily
- **No actual usage found** in any Java code
- **Action**: ✅ Removed from parent POM

### 2. ❌ AWS S3 SDK (2.5 MB per JAR) - **NOT USED**
- Declared in 4 modules but never used
- Only commented-out references in `APIController.java`
- **Action**: ✅ Removed from all modules

### 3. ⚠️ Apache POI (14 MB) - **Limited Use**
- Only used in `backoffice/UserExcelExporter.java`
- But included in ALL modules via parent POM
- **Action**: ✅ Remains in parent (backoffice needs it)

### 4. ⚠️ OpenPDF (4 MB) - **Limited Use**
- Only used in `backoffice/UserPDFExporter.java`
- Only declared in backoffice module
- **Action**: ✅ Correct placement, no change needed

### 5. ⚠️ spring-boot-devtools
- Included in production JARs
- **Action**: ✅ Marked as `<optional>true</optional>` and `<scope>runtime</scope>`

## Changes Made

### 1. Parent POM (`webparent/pom.xml`)
```xml
<!-- REMOVED: OpenCV dependency (72 MB saved per JAR) -->

<!-- UPDATED: devtools to be optional -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-devtools</artifactId>
    <scope>runtime</scope>
    <optional>true</optional>
</dependency>
```

### 2. All Portal Modules
- **backoffice**
- **client-portal**
- **merchant-portal**
- **organization-portal**

**Removed:**
- AWS S3 SDK dependencies
- AWS BOM dependency management

**Updated:**
- devtools marked as optional

## Expected Size Reduction

### Per JAR Savings:
- **OpenCV removal**: -72 MB
- **AWS S3 SDK removal**: -2.5 MB
- **devtools optimization**: -2 MB (when optional)
- **Total Expected Savings**: ~75-80 MB per JAR

### New Expected Sizes:
- **backoffice**: ~87 MB (from 167 MB) - **48% reduction**
- **client-portal**: ~68 MB (from 148 MB) - **54% reduction**
- **payment-service**: ~50 MB (from 130 MB) - **62% reduction**

## How to Apply

Run a clean build:
```bash
mvn clean install
```

Then verify the new sizes:
```bash
dir deploy\backoffice\backoffice-1.0-SNAPSHOT.jar
dir deploy\client-portal\client-portal-1.0-SNAPSHOT.jar
dir deploy\payment-service\payment-service-1.0-SNAPSHOT.jar
```

## Further Optimization Opportunities

### Consider for Future:
1. **Move Apache POI** to backoffice-only if no other modules need it
2. **Evaluate WebJars usage** - jQuery UI (1.65 MB) might be replaceable with CDN
3. **Review Hibernate** - if only using simple queries, consider lighter alternatives
4. **Scope test dependencies** - ensure test libraries aren't in production JARs
5. **Exclude transitive dependencies** you don't use

### Check Static Resources:
Large JARs can also contain:
- Duplicate JavaScript libraries
- Large images/fonts
- Unused static assets

## Verification Commands

After rebuild, analyze the new largest dependencies:
```powershell
Add-Type -Assembly 'System.IO.Compression.FileSystem'
$jar = [System.IO.Compression.ZipFile]::OpenRead('deploy\backoffice\backoffice-1.0-SNAPSHOT.jar')
$entries = $jar.Entries | Where-Object {$_.FullName -like 'BOOT-INF/lib/*'} | Sort-Object Length -Descending | Select-Object -First 15
foreach ($entry in $entries) {
    $sizeMB = [math]::Round($entry.Length/1MB,2)
    Write-Host "$sizeMB MB - $($entry.Name)"
}
$jar.Dispose()
```

## Best Practices Moving Forward

1. ✅ **Add dependencies to specific modules**, not parent POM (unless truly shared by all)
2. ✅ **Audit dependencies before adding** - check actual size and necessity
3. ✅ **Use `<optional>true</optional>`** for development-only dependencies
4. ✅ **Exclude unused transitive dependencies** explicitly
5. ✅ **Regular dependency cleanup** - remove unused libraries

## Summary
By removing **unused OpenCV (72 MB)** and **AWS SDK (2.5 MB)** from your JARs, you should see a **50-60% size reduction** across all modules. Your deployment and startup times will improve significantly!

