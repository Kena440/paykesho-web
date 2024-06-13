package org.chaz.backoffice.assessments.service;

import org.chaz.backoffice.assessments.repository.*;
import org.chaz.backoffice.paging.PagingAndSortingHelper;
import org.commons.entity.assessment.*;
import org.commons.utils.AssessmentToolError;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class AssessmentsService {

    private static final int EVENTS_PER_PAGE = 20;
    @Autowired
    private AssessmentsRepository assessmentsRepository;
    @Autowired
    private AssessmentAssessorsRepository assessmentAssessorsRepository;
    @Autowired
    private AssessmentFeedbackRepository assessmentFeedbackRepository;
    @Autowired
    private AssessmentQuestionsRepository assessmentQuestionsRepository;
    @Autowired
    private AssessmentFormsRepository assessmentFormsRepository;
    @Autowired
    private AssessmentQuestionOptionRepository assessmentQuestionOptionRepository;
    @Autowired
    private AssessmentInterviewsRepository assessmentInterviewsRepository;
    @Autowired
    private AssessmentQuestionResponseRepository assessmentQuestionResponseRepository;
    @Autowired
    private AssessmentQuestionVerificationResponseRepository assessmentQuestionVerificationResponseRepository;

    /**
     * +
     * This method saves assessment submitted from the assessment
     *
     * @param assessment
     */
    public Assessment save(Assessment assessment) {
        if (assessment == null) {
            return null;
        }

        return assessmentsRepository.save(assessment);
    }

    public AssessmentAssessor saveAssessor(AssessmentAssessor assessmentAssessor) throws AssessmentToolError {
        if (assessmentAssessor == null) {
            throw new AssessmentToolError("Null parameter Assessor");
        }

        return assessmentAssessorsRepository.save(assessmentAssessor);
    }

    public AssessmentQuestion saveQuestion(AssessmentQuestion assessmentQuestion) throws AssessmentToolError {
        if (assessmentQuestion == null) {
            throw new AssessmentToolError("Null parameter Question");
        }

        return assessmentQuestionsRepository.save(assessmentQuestion);
    }

    public AssessmentQuestionOption saveQuestionOption(AssessmentQuestionOption assessmentQuestionOption) throws AssessmentToolError {
        if (assessmentQuestionOption == null) {
            throw new AssessmentToolError("Null parameter Question");
        }

        return assessmentQuestionOptionRepository.save(assessmentQuestionOption);
    }


    public AssessmentForm saveForm(AssessmentForm assessmentForm) throws AssessmentToolError {
        if (assessmentForm == null) {
            throw new AssessmentToolError("Null parameter Form");
        }

        return assessmentFormsRepository.save(assessmentForm);
    }

    /**
     * +
     * This method checks if the code submitted is unique
     *
     * @param id
     * @param name
     * @return
     */
    public boolean isAssessmentUnique(Integer id, String name) {
        if (name == null) {
            return false;
        }
        Assessment assessment = assessmentsRepository.getAssessmentByName(name);

        if (assessment == null) return true;
        boolean isCreatingNew = (id == null);

        if (isCreatingNew) {
            if (assessment != null) return false;
        } else {
            if (assessment.getId() != id) {
                return false;
            }
        }

        return true;
    }

    public void listByPage(int pageNum, PagingAndSortingHelper helper) throws AssessmentToolError {
        helper.listEntities(pageNum, EVENTS_PER_PAGE, assessmentsRepository);
    }

    public Assessment getAssessmentById(Integer id) throws AssessmentToolError {
        if (id == null) {
            throw new AssessmentToolError("Null parameter id");
        }

        try {
            return assessmentsRepository.findById(id).get();
        } catch (NoSuchElementException exception) {
            throw new AssessmentToolError("Could not find any Assessment");
        }
    }

    public AssessmentForm getAssessmentFormById(Integer id) throws AssessmentToolError {
        if (id == null) {
            throw new AssessmentToolError("Null parameter id");
        }

        try {
            return assessmentFormsRepository.findById(id).get();
        } catch (NoSuchElementException exception) {
            throw new AssessmentToolError("Could not find any Assessment");
        }
    }

    public AssessmentInterview getAssessmentInterviewById(Integer id) throws AssessmentToolError {
        if (id == null) {
            throw new AssessmentToolError("Null parameter id");
        }

        try {
            return assessmentInterviewsRepository.findById(id).get();
        } catch (NoSuchElementException exception) {
            throw new AssessmentToolError("Could not find any Assessment");
        }
    }

    public void delete(Integer id) throws AssessmentToolError {
        if (id == null) {
            throw new AssessmentToolError("Null parameter id");
        }

        Long countById = assessmentsRepository.countById(id);
        if (countById == null || countById == 0) {
            throw new AssessmentToolError("Could not find any Assessment");
        }

        assessmentsRepository.deleteById(id);
    }

    public void updateAssessmentStatus(Integer id, boolean enabled) throws AssessmentToolError {
        if (id == null) {
            throw new AssessmentToolError("Null parameter id");
        }

        Long countById = assessmentsRepository.countById(id);
        if (countById == null || countById == 0) {
            throw new AssessmentToolError("Could not find any Assessment");
        }

        assessmentsRepository.updateEnableStatus(id, enabled);
    }

    public List<AssessmentQuestion> getVerifiedAssessmentQuestions(AssessmentForm assessmentForm) {
        List<AssessmentQuestion> allQuestions = assessmentQuestionsRepository.findByAssessmentForm(assessmentForm.getId());

        return allQuestions.stream()
                .filter(AssessmentQuestion::isNeedsVerification)
                .collect(Collectors.toList());
    }

    public List<Assessment> getUpcomingAssessments(int i) {
        if (i > 0) {
            return assessmentsRepository.findUpcomingAssessments(i);
        } else {
            return assessmentsRepository.findUpcomingAssessments(30);
        }
    }

    public List<AssessmentFeedback> getAssessmentFeedback(Assessment assessment) throws AssessmentToolError {
        if (assessment == null) {
            throw new AssessmentToolError("Null parameter assessment");
        }

        return assessmentFeedbackRepository.findByAssessment(assessment.getId());
    }

    public List<AssessmentInterview> getAssessmentInterviews(Assessment assessment) throws AssessmentToolError {
        if (assessment == null) {
            throw new AssessmentToolError("Null parameter assessment");
        }
        return assessmentInterviewsRepository.findByAssessment(assessment.getId());
    }

    public List<AssessmentInterview> findRecentInterviews(Integer assessmentId) {
        // Calculate two days ago
        LocalDateTime twoDaysAgo = LocalDateTime.now().minusDays(2);
        Date date = Date.from(twoDaysAgo.atZone(ZoneId.systemDefault()).toInstant());

        List<AssessmentInterview> assessmentInterviews = assessmentInterviewsRepository.findRecentByAssessment(assessmentId, date);

        for (AssessmentInterview assessmentInterview : assessmentInterviews) {
            int size = getVerifiedAssessmentQuestions(assessmentInterview.getAssessmentForm()).size();
            if (size > 0) {
                assessmentInterview.setNeedsVerification(true);
            }
        }
        return assessmentInterviews;
    }

    public Page<AssessmentInterview> listAssessmentInterviewsForByPage(int pageNum, String sortField, String sortDir, String keyword) {
        Sort sort = Sort.by(sortField);
        sort = sortDir.equals("asc") ? sort.ascending() : sort.descending();

        Pageable pageable = PageRequest.of(pageNum - 1, 200, sort);

        if (keyword != null) {
            return assessmentInterviewsRepository.findAll(keyword, pageable);
        }
        return assessmentInterviewsRepository.findAll(pageable);
    }

    public List<AssessmentAssessor> getAssessmentAssessors(Assessment assessment) throws AssessmentToolError {
        if (assessment == null) {
            throw new AssessmentToolError("Null parameter assessment");
        }

        return assessmentAssessorsRepository.findByAssessment(assessment.getId());
    }

    public List<AssessmentQuestion> getAssessmentQuestions(Assessment assessment) throws AssessmentToolError {
        if (assessment == null) {
            throw new AssessmentToolError("Null parameter assessment");
        }

        return assessmentQuestionsRepository.findByAssessment(assessment.getId());
    }

    public List<AssessmentQuestion> getAssessmentQuestionsByForm(AssessmentForm assessmentForm) throws AssessmentToolError {
        if (assessmentForm == null) {
            throw new AssessmentToolError("Null parameter assessmentForm");
        }

        return assessmentQuestionsRepository.findByAssessmentForm(assessmentForm.getId());
    }

    public List<AssessmentForm> getAssessmentForms(Assessment assessment) throws AssessmentToolError {
        if (assessment == null) {
            throw new AssessmentToolError("Null parameter assessment");
        }
        List<AssessmentForm> assessmentForms = assessmentFormsRepository.findByAssessment(assessment.getId());
     /*   for (AssessmentForm form : assessmentForms) {
            List<AssessmentQuestion> questions = assessmentQuestionsRepository.findByAssessmentForm(form.getId());
            form.setQuestions(questions);
        }*/
        return assessmentForms;

    }

    public List<AssessmentQuestionResponse> getAssessmentQuestionResponseByInterviewId(AssessmentInterview assessmentInterview) throws AssessmentToolError {
        if (assessmentInterview == null) {
            throw new AssessmentToolError("Null parameter assessment");
        }

        return assessmentQuestionResponseRepository.findByAssessmentInterview(assessmentInterview.getId());
    }

    public List<AssessmentQuestionVerificationResponse> getAssessmentQuestionVerificationResponseByInterviewId(AssessmentInterview assessmentInterview) throws AssessmentToolError {
        if (assessmentInterview == null) {
            throw new AssessmentToolError("Null parameter assessment");
        }

        return assessmentQuestionVerificationResponseRepository.findByAssessmentInterview(assessmentInterview.getId());
    }


    public int getRespondingFacilities(Integer assessmentId) {
        List<AssessmentInterview> interviews = assessmentInterviewsRepository.findByAssessment(assessmentId);

        // Ensure that facilities and their IDs are not null before processing
        Set<Integer> uniqueFacilityIds = interviews.stream()
                .filter(interview -> interview.getFacility() != null) // Filter out null facilities
                .filter(interview -> interview.getFacility().getId() != null) // Filter out facilities with null IDs
                .map(interview -> interview.getFacility().getId())
                .collect(Collectors.toSet());

        return uniqueFacilityIds.size();
    }

    public Map<String, Long> getInterviewsByAssessor(Integer assessmentId) {
        // Check for null assessmentId
        if (assessmentId == null) {
            throw new IllegalArgumentException("Assessment ID cannot be null");
        }

        List<AssessmentInterview> interviews = assessmentInterviewsRepository.findAssessmentInterviewsByAssessmentFormId(assessmentId);

        // Filter out null values before grouping by assessor name
        return interviews.stream()
                .filter(interview -> interview != null &&
                        interview.getAssessor() != null &&
                        interview.getAssessor().getName() != null)
                .collect(Collectors.groupingBy(interview -> interview.getAssessor().getName(), Collectors.counting()));
    }

    public Map<String, Long> getCompletionRates(Integer assessmentId) {
        // Check for null assessmentId
        if (assessmentId == null) {
            throw new IllegalArgumentException("Assessment ID cannot be null");
        }

        // Initialize completion rates map
        Map<String, Long> completionRates = new HashMap<>();

        try {
            long totalInterviews = assessmentInterviewsRepository.countByAssessmentFormId(assessmentId);
            long completedInterviews = assessmentInterviewsRepository.countByAssessmentFormIdAndCompleted(assessmentId, true);

            // Populate the map with completion rates
            completionRates.put("totalInterviews", totalInterviews);
            completionRates.put("completedInterviews", completedInterviews);
        } catch (Exception e) {
            // Handle potential repository exceptions
            throw new RuntimeException("Error fetching completion rates", e);
        }

        return completionRates;
    }


    public Map<String, String> getAverageCompletionTime(Integer assessmentId) {
        // Check for null assessmentId
        if (assessmentId == null) {
            throw new IllegalArgumentException("Assessment ID cannot be null");
        }

        // Retrieve interviews for the given assessmentId
        List<AssessmentInterview> interviews = assessmentInterviewsRepository.findAssessmentInterviewsByAssessmentFormId(assessmentId);

        // Filter out interviews with null dates and incomplete interviews
        List<AssessmentInterview> validInterviews = interviews.stream()
                .filter(interview -> interview.getCreatedDate() != null && interview.getCompletedDate() != null)
                .collect(Collectors.toList());

        // Calculate statistics only if there are valid interviews
        DoubleSummaryStatistics stats = validInterviews.stream()
                .mapToDouble(interview -> {
                    long durationMillis = interview.getCompletedDate().getTime() - interview.getCreatedDate().getTime();
                    return durationMillis / (1000.0 * 60); // Convert milliseconds to minutes
                })
                .summaryStatistics();

        // Initialize map with default values
        Map<String, String> completionTimes = new HashMap<>();
        completionTimes.put("averageTime", formatDuration(stats.getCount() > 0 ? stats.getAverage() : 0));
        completionTimes.put("minTime", formatDuration(stats.getCount() > 0 ? stats.getMin() : 0));
        completionTimes.put("maxTime", formatDuration(stats.getCount() > 0 ? stats.getMax() : 0));

        return completionTimes;
    }

    // Utility method to format duration in hours and minutes
    private String formatDuration(double durationMinutes) {
        long hours = (long) durationMinutes / 60;
        long minutes = (long) durationMinutes % 60;

        return String.format("%d hours %d minutes", hours, minutes);
    }

    public List<AssessmentInterview> getAssessmentInterviewsByForm(AssessmentForm assessmentForm) throws AssessmentToolError {
        if (assessmentForm == null) {
            throw new AssessmentToolError("Null parameter assessmentForm");
        }

        return assessmentInterviewsRepository.findAssessmentInterviewsByAssessmentFormId(assessmentForm.getId());
    }

    public List<AssessmentQuestionResponse> getAssessmentQuestionResponsesByInterviews(List<AssessmentInterview> interviews) {
        List<Integer> interviewIds = interviews.stream().map(AssessmentInterview::getId).collect(Collectors.toList());
        return assessmentQuestionResponseRepository.findByInterviewIds(interviewIds);
    }

    public long countAssessmentInterviews(Integer id) {
        return assessmentInterviewsRepository.countAssessmentInterviewsByAssessmentFormAssessmentId(id);
    }
}
