package org.chaz.csoportal.paging;

import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

public class PagingAndSortingArgumentResolver implements HandlerMethodArgumentResolver {
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.getParameterAnnotation(PagingAndSortingParam.class) != null;
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer, NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
        PagingAndSortingParam annotation = parameter.getParameterAnnotation(PagingAndSortingParam.class);

        String sortDir = webRequest.getParameter("sortDir");
        String sortField = webRequest.getParameter("sortField");
        String keyword = webRequest.getParameter("keyword");

        String reverseSortDir = sortDir.equals("asc") ? "desc" : "asc";

        mavContainer.addAttribute("sortField", sortField);
        mavContainer.addAttribute("sortDir", sortDir);
        mavContainer.addAttribute("reverseSortDir", reverseSortDir);
        mavContainer.addAttribute("keyword", keyword);
        mavContainer.addAttribute("moduleURL", annotation.moduleURL());

        return new PagingAndSortingHelper(mavContainer, annotation.listName(), sortField, sortDir, keyword);
    }
}
