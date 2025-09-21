package org.paykesho.organizationportal.config;

import org.paykesho.organizationportal.security.oauth.OrganizationAdminOAuth2User;
import org.paykesho.organizationportal.settings.currency.CurrencySettingBag;
import org.paykesho.organizationportal.settings.email.EmailSettingBag;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.security.authentication.RememberMeAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;

import javax.servlet.http.HttpServletRequest;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Properties;

public class Utility {
    public static String getSiteUrl(HttpServletRequest request) {
        String siteUrl = request.getRequestURL().toString();
        return siteUrl.replace(request.getServletPath(), "");
    }

    public static JavaMailSenderImpl prepareMailSender(EmailSettingBag emailSettingBag) {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(emailSettingBag.getHost());
        mailSender.setPort(emailSettingBag.getPort());
        mailSender.setUsername(emailSettingBag.getUsername());
        mailSender.setPassword(emailSettingBag.getPassword());

        Properties mailProperties = new Properties();
        mailProperties.setProperty("mail.smtp.auth", emailSettingBag.getSMTPAuth());
        mailProperties.setProperty("mail.smtp.starttls.enable", emailSettingBag.getSMTPSecured());

        mailSender.setJavaMailProperties(mailProperties);

        return mailSender;
    }

    public static String getEKeshofAuthenticatedOrganizationAdmin(HttpServletRequest request) {
        try {
            if (request == null) {
                return null;
            }

            Object principal = request.getUserPrincipal();
            if (principal == null) {
                return null;
            }

            String customerEmail = null;

            if (principal instanceof UsernamePasswordAuthenticationToken || principal instanceof RememberMeAuthenticationToken) {
                try {
                    customerEmail = request.getUserPrincipal().getName();
                } catch (Exception e) {
                    System.err.println("Error getting username from authentication token: " + e.getMessage());
                    return null;
                }
            } else if (principal instanceof OAuth2AuthenticationToken) {
                try {
                    OAuth2AuthenticationToken oAuth2AuthenticationToken = (OAuth2AuthenticationToken) principal;
                    Object principalObj = oAuth2AuthenticationToken.getPrincipal();

                    if (principalObj instanceof OrganizationAdminOAuth2User) {
                        OrganizationAdminOAuth2User organizationAdminOAuth2User = (OrganizationAdminOAuth2User) principalObj;
                        customerEmail = organizationAdminOAuth2User.getEmail();
                    } else {
                        System.err.println("Unexpected OAuth2 principal type: " + principalObj.getClass().getName());
                        return null;
                    }
                } catch (Exception e) {
                    System.err.println("Error getting email from OAuth2 token: " + e.getMessage());
                    return null;
                }
            }

            return customerEmail;

        } catch (Exception e) {
            System.err.println("Unexpected error in getEKeshofAuthenticatedOrganizationAdmin: " + e.getMessage());
            return null;
        }
    }

    public static String formatCurrency(float amount, CurrencySettingBag currencySettingBag) {
        String symbol = currencySettingBag.getCURRENCY_SYMBOL();
        String symbolPostion = currencySettingBag.getCURRENCY_SYMBOL_POSITION();
        String decimalPointType = currencySettingBag.getDECIMAL_POINT_TYPE();
        String thousandPointType = currencySettingBag.getTHOUSAND_POINT_TYPE();
        int decimalDigits = currencySettingBag.getDECIMAL_DIGITS();

        String pattern = symbolPostion.equals("before price") ? symbol : "";
        pattern += "###,###";

        if (decimalDigits > 0) {
            pattern += ".";
            for (int count = 1; count < decimalDigits; count++) pattern += "#";
        }

        pattern += symbolPostion.equals("after price") ? symbol : "";

        char thousandSepator = thousandPointType.equals("POINT") ? '.' : ',';
        char decimalSepator = decimalPointType.equals("POINT") ? '.' : ',';

        DecimalFormatSymbols decimalFormatSymbols = DecimalFormatSymbols.getInstance();
        decimalFormatSymbols.setDecimalSeparator(decimalSepator);
        decimalFormatSymbols.setGroupingSeparator(thousandSepator);
        DecimalFormat decimalFormat = new DecimalFormat(pattern, decimalFormatSymbols);

        return decimalFormat.format(amount);
    }

}
