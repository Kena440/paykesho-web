package org.paykesho.backoffice.settings.email;


import org.commons.entity.settings.Setting;
import org.commons.entity.settings.SettingBag;

import java.util.List;

public class EmailSettingBag extends SettingBag {
    public EmailSettingBag(List<Setting> settingList) {
        super(settingList);
    }

    public String getHost() {
        return super.getValue("MAIL_HOST");
    }

    public int getPort() {
        return Integer.parseInt(super.getValue("MAIL_PORT"));
    }

    public String getUsername() {
        return super.getValue("MAIL_USERNAME");
    }

    public String getPassword() {
        return super.getValue("MAIL_PASSWORD");
    }

    public String getSMTPAuth() {
        return super.getValue("SMTP_AUTH");
    }

    public String getSMTPSecured() {
        return super.getValue("SMTP_SECURED");
    }

    public String getFromAddress() {
        return super.getValue("MAIL_FROM");
    }

    public String getSenderName() {
        return super.getValue("MAIL_SENDER_NAME");
    }

    public String getOrganizationAdminCreationSubject() {
        return super.getValue("ORGANIZATION_ADMIN_CREATION_SUBJECT");
    }

    public String getOrganizationAdminCreationContent() {
        return super.getValue("ORGANIZATION_ADMIN_CREATION_CONTENT");
    }

    public String getMerchantAdminCreationSubject() {
        return super.getValue("MERCHANT_ADMIN_CREATION_SUBJECT");
    }

    public String getMerchantAdminCreationContent() {
        return super.getValue("MERCHANT_ADMIN_CREATION_CONTENT");
    }

    public String getFleetManager() {
        return super.getValue("FLEET_MANAGER");
    }

}
