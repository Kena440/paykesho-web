package org.chaz.backoffice.events.controller;

import org.chaz.backoffice.events.enums.Constants;
import org.chaz.backoffice.events.service.EventCodeService;
import org.chaz.backoffice.events.service.EventsService;
import org.chaz.backoffice.paging.PagingAndSortingHelper;
import org.chaz.backoffice.paging.PagingAndSortingParam;
import org.chaz.backoffice.settings.service.SettingService;
import org.chaz.backoffice.utils.AmazonS3Util;
import org.commons.entity.event.Event;
import org.commons.utils.AssessmentToolError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;

@Controller
@RequestMapping("/events")
public class EventsController {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventsController.class);
    @Autowired
    private EventsService eventsService;
    @Autowired
    private EventCodeService eventCodeService;
    @Autowired
    private SettingService settingService;

    @GetMapping("")
    public String listFirstPage() {
        return Constants.REDIRECT_TO_Events_WITHOUT_KEYWORD;
    }

    @GetMapping("/page/{pageNum}")
    public String listAllEventsByPage(@PagingAndSortingParam(listName = "eventList", moduleURL = "/events") PagingAndSortingHelper helper, @PathVariable("pageNum") int pageNum) throws AssessmentToolError {
        eventsService.listByPage(pageNum, helper);

        return "events/events";
    }

    @GetMapping("/new")
    public String newEvent(Model model) {
        String pageTitle = "New Event";

        Event event = new Event();
        event.setEnabled(true);
        event.setRequiresResponse(false);
        event.setEventDate(new Date());

        model.addAttribute("event", event);
        model.addAttribute("pageTitle", pageTitle);
        return "events/event_form";
    }

    @PostMapping("/save")
    public String saveEvent(Event event, RedirectAttributes redirectAttributes, @RequestParam("image") MultipartFile multipartFile) throws IOException, AssessmentToolError {

        DateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss");
        try {
            event.setEventDate(dateFormatter.parse(event.getUpdatedTimeOnForm()));
        } catch (ParseException e) {
            e.printStackTrace();
        }

        if (!multipartFile.isEmpty()) {
            String fileName = StringUtils.cleanPath(Objects.requireNonNull(multipartFile.getOriginalFilename()));
            event.setPhotos(fileName);
            Event savedEvent = eventsService.save(event);
            String uploadDir = "event_photos/" + savedEvent.getId();

            if (!fileName.isEmpty()) {
                AmazonS3Util.removeFolder(uploadDir);
                AmazonS3Util.uploadFile(uploadDir, fileName, multipartFile.getInputStream());
            }
        } else {
            if (event.getPhotos().isEmpty()) {
                event.setPhotos(null);
            }
            eventsService.save(event);
        }

        //todo add logic for survey if response is required

        redirectAttributes.addFlashAttribute(Constants.MESSAGE, "event Operation Successful: " + event.getName());
        return getRedirectURLtoAffectedEvent(event);
    }

    @GetMapping("/edit/{id}")
    public String editEvent(@PathVariable(name = "id") Integer id, RedirectAttributes redirectAttributes, Model model) throws AssessmentToolError {
        if (id == null) {
            return Constants.REDIRECT_TO_Events;
        }
        String pageTitle = "Edit Event";
        Event event = eventsService.getEventById(id);

        model.addAttribute("event", event);
        model.addAttribute("pageTitle", pageTitle);

        return "events/event_form";
    }

    @GetMapping("delete/{id}")
    public String deleteEvent(@PathVariable(name = "id") Integer id, RedirectAttributes redirectAttributes) throws AssessmentToolError {
        if (id == null) {
            throw new AssessmentToolError("no parameter id");
        }

        eventsService.delete(id);

        String eventPhotosDir = "event_photos/" + id;
        AmazonS3Util.removeFolder(eventPhotosDir);
        redirectAttributes.addFlashAttribute(Constants.MESSAGE, "event deleted successfully");
        return Constants.REDIRECT_TO_Events;
    }

    @GetMapping("/{id}/enabled/{status}")
    public String updateEventEnabledStatus(@PathVariable(name = "id") Integer id, @PathVariable(name = "status") boolean enabled, RedirectAttributes redirectAttributes, Model model) throws AssessmentToolError {

        if (id == null) {
            throw new AssessmentToolError("no parameter id");
        }

        try {
            Event event = eventsService.getEventById(id);
            eventsService.updateEventStatus(id, enabled);
            String status = enabled ? "enabled" : "has been disabled";
            String operationMSG = event.getName() + " has been " + status;
            redirectAttributes.addFlashAttribute(Constants.MESSAGE, operationMSG);
        } catch (AssessmentToolError supremaError) {
            redirectAttributes.addFlashAttribute(Constants.MESSAGE, supremaError.getMessage());
        }
        return Constants.REDIRECT_TO_Events;

    }

    private String getRedirectURLtoAffectedEvent(Event event) throws AssessmentToolError {
        if (event == null) {
            throw new AssessmentToolError("no event provided");
        }
        String eventName = event.getName();
        return Constants.REDIRECT_TO_Events_KEYWORD + eventName;
    }

    @GetMapping("/view/{id}")
    public String viewEvent(@PathVariable("id") Integer id, Model model, RedirectAttributes redirectAttributes) {
        try {

            Event event = eventsService.getEventById(id);

            model.addAttribute("event", event);

            return "events/event_view";
        } catch (AssessmentToolError assessmentToolError) {
            redirectAttributes.addFlashAttribute("message", assessmentToolError.getMessage());
            return "redirect:/events";
        }
    }


    @GetMapping("/generateQRCode")
    public void generateQRCode(String qrContent, HttpServletResponse response) throws IOException {
        response.setContentType("image/png");
        byte[] qrCode = eventCodeService.generateQRCode(qrContent, 500, 500);
        OutputStream outputStream = response.getOutputStream();
        outputStream.write(qrCode);
    }


}
