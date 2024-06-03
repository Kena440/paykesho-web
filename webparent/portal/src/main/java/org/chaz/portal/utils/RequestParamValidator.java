package org.chaz.portal.utils;

import org.commons.utils.AssessmentToolError;

import javax.servlet.http.HttpServletRequest;

public class RequestParamValidator {

    public static String validateParam(HttpServletRequest request, String paramName) throws AssessmentToolError {
        String paramValue = request.getParameter(paramName);
        if (paramValue == null || paramValue.isEmpty()) {
            throw new AssessmentToolError("Missing or empty parameter: " + paramName);
        }
        return paramValue;
    }
}