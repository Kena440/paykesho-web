package org.paykesho.backoffice.paging;

import org.springframework.core.MethodParameter;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

public class PagingAndSortingArgumentResolver implements HandlerMethodArgumentResolver {
    @Override
    public boolean supportsParameter(@NonNull MethodParameter parameter) {
        return parameter.getParameterAnnotation(PagingAndSortingParam.class) != null;
    }

    @Override
    @Nullable
    public Object resolveArgument(@NonNull MethodParameter parameter, @Nullable ModelAndViewContainer mavContainer,
                                  @NonNull NativeWebRequest webRequest, @Nullable WebDataBinderFactory binderFactory) throws Exception {
        PagingAndSortingParam annotation = parameter.getParameterAnnotation(PagingAndSortingParam.class);

        if (annotation == null) {
            throw new IllegalArgumentException("PagingAndSortingParam annotation is required");
        }

        String sortDir = webRequest.getParameter("sortDir");
        String sortField = webRequest.getParameter("sortField");
        String keyword = webRequest.getParameter("keyword");

        // Provide default sortDir if null
        if (sortDir == null) {
            sortDir = "asc";
        }

        String reverseSortDir = sortDir.equals("asc") ? "desc" : "asc";

        if (mavContainer != null) {
            mavContainer.addAttribute("sortField", sortField);
            mavContainer.addAttribute("sortDir", sortDir);
            mavContainer.addAttribute("reverseSortDir", reverseSortDir);
            mavContainer.addAttribute("keyword", keyword);
            mavContainer.addAttribute("moduleURL", annotation.moduleURL());
        }

        return new PagingAndSortingHelper(mavContainer, annotation.listName(), sortField, sortDir, keyword);
    }
}
