package org.chaz.portal.config;

import org.chaz.portal.security.oauth.AssessorOAuth2User;
import org.chaz.portal.settings.currency.CurrencySettingBag;
import org.chaz.portal.settings.email.EmailSettingBag;
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

    public static String getEmailOfAuthenticatedAssessor(HttpServletRequest request) {
        Object principal = request.getUserPrincipal();
        if (principal == null) return null;

        String customerEmail = null;

        if (principal instanceof UsernamePasswordAuthenticationToken || principal instanceof RememberMeAuthenticationToken) {
            customerEmail = request.getUserPrincipal().getName();
        } else if (principal instanceof OAuth2AuthenticationToken) {
            OAuth2AuthenticationToken oAuth2AuthenticationToken = (OAuth2AuthenticationToken) principal;
            AssessorOAuth2User assessorOAuth2User = (AssessorOAuth2User) oAuth2AuthenticationToken.getPrincipal();
            customerEmail = assessorOAuth2User.getEmail();
        }

        return customerEmail;

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
