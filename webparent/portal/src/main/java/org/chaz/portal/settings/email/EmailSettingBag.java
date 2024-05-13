package org.chaz.membershipsite.settings.email;

import org.membershipcommons.entity.settings.Setting;
import org.membershipcommons.entity.settings.SettingBag;

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

    public String getCustomerVerificationSubject() {
        return super.getValue("CUSTOMER_VERIFY_SUBJECT");
    }

    public String getCustomerVerificationContent() {
        return super.getValue("CUSTOMER_VERIFY_CONTENT");
    }
}
