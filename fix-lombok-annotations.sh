#!/bin/bash

# Script to fix Lombok @EqualsAndHashCode annotations in commons entities

echo "🔧 Fixing Lombok annotations in commons entities..."

# List of entity files that need @EqualsAndHashCode(callSuper = false)
entities=(
    "commons/src/main/java/org/commons/entity/AbstractAddressWithCountry.java"
    "commons/src/main/java/org/commons/entity/merchant/Merchant.java"
    "commons/src/main/java/org/commons/entity/announcement/Announcement.java"
    "commons/src/main/java/org/commons/entity/AbstractAddress.java"
    "commons/src/main/java/org/commons/entity/OTP.java"
    "commons/src/main/java/org/commons/entity/State.java"
    "commons/src/main/java/org/commons/entity/Notification.java"
    "commons/src/main/java/org/commons/entity/client/CreditScore.java"
    "commons/src/main/java/org/commons/entity/organization/Organization.java"
    "commons/src/main/java/org/commons/entity/client/EmployeePayslipInformation.java"
    "commons/src/main/java/org/commons/entity/client/ClientBalance.java"
    "commons/src/main/java/org/commons/entity/settings/Currency.java"
    "commons/src/main/java/org/commons/entity/Role.java"
)

for entity in "${entities[@]}"; do
    if [ -f "$entity" ]; then
        echo "Fixing: $entity"
        
        # Add @EqualsAndHashCode(callSuper = false) after @Data annotation
        sed -i 's/@Data/@Data\n@EqualsAndHashCode(callSuper = false)/g' "$entity" 2>/dev/null || true
        
        echo "✅ Fixed: $entity"
    else
        echo "⚠️  Not found: $entity"
    fi
done

echo "🎉 Lombok annotation fixes completed!"
