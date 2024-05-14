package org.chaz.backoffice.assessments.controller;

import org.chaz.backoffice.assessments.service.AssessmentsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/assessments/rest")
public class AssessmentsRestController {

    @Autowired
    private AssessmentsService assessmentsService;

    @PostMapping("/check-assessment")
    public String checkDuplicateCode(Integer id, String name) {
        return assessmentsService.isAssessmentUnique(id, name) ? "OK" : "Duplicated";
    }
}
