package org.paykesho.merchantportal.utils;

import org.commons.utils.PaykeshoError;

import javax.servlet.http.HttpServletRequest;

public class RequestParamValidator {

    public static String validateParam(HttpServletRequest request, String paramName) throws PaykeshoError {
        String paramValue = request.getParameter(paramName);
        if (paramValue == null || paramValue.isEmpty()) {
            throw new PaykeshoError("Missing or empty parameter: " + paramName);
        }
        return paramValue;
    }
}