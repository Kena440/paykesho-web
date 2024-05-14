package org.chaz.backoffice.paging;

import lombok.Data;
import org.commons.utils.AssessmentToolError;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.List;

@Data
public class PagingAndSortingHelper {

    private ModelAndViewContainer model;
    private String listName;
    private String sortField;
    private String sortDir;
    private String keyword;

    public PagingAndSortingHelper(ModelAndViewContainer model, String listName, String sortField, String sortDir, String keyword) {
        this.model = model;
        this.listName = listName;
        this.sortField = sortField;
        this.sortDir = sortDir;
        this.keyword = keyword;
    }

    public void updateModelAttributes(int pageNum, Page<?> page) {

        List<?> itemsList = page.getContent();
        int pageSize = page.getSize();

        long startCount = (long) (pageNum - 1) * pageSize + 1;
        long endCount = startCount + pageSize - 1;

        if (endCount > page.getTotalElements()) {
            endCount = page.getTotalElements();
        }

        int previousPage = pageNum - 1;
        int nextPage = pageNum + 1;
        if (previousPage == 0) {
            previousPage = 1;
        }
        if (nextPage > page.getTotalPages()) {
            nextPage = page.getTotalPages();
        }


        model.addAttribute("currentPage", pageNum);
        model.addAttribute("previousPage", previousPage);
        model.addAttribute("nextPage", nextPage);
        model.addAttribute("startCount", startCount);
        model.addAttribute("endCount", endCount);
        model.addAttribute("totalItems", page.getTotalElements());
        model.addAttribute("totalPages", page.getTotalPages());
        model.addAttribute(listName, itemsList);

    }

    public void listEntities(int pageNum, int pageSize, SearchRepository<?, Integer> repository) throws AssessmentToolError {
        if (pageNum == 0) {
            throw new AssessmentToolError("invalid page");
        }
        Sort sort = Sort.by(sortField);
        sort = sortDir.equals("desc") ? sort.descending() : sort.ascending();
        Pageable pageable = PageRequest.of(pageNum - 1, pageSize, sort);
        Page<?> page = null;

        if (keyword != null) {
            page = repository.findAll(keyword, pageable);
        } else {
            page = repository.findAll(pageable);
        }

        updateModelAttributes(pageNum, page);
    }

    public Pageable createPageable(int pageSize, int pageNum) {
        Sort sort = Sort.by(sortField);
        sort = sortDir.equals("asc") ? sort.ascending() : sort.descending();
        return PageRequest.of(pageNum - 1, pageSize, sort);
    }


}
