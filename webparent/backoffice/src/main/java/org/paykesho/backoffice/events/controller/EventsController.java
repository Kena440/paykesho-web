package org.paykesho.backoffice.events.controller;

import org.commons.entity.announcement.Announcement;
import org.commons.utils.PaykeshoError;
import org.paykesho.backoffice.events.enums.Constants;
import org.paykesho.backoffice.events.service.EventCodeService;
import org.paykesho.backoffice.events.service.EventsService;
import org.paykesho.backoffice.paging.PagingAndSortingHelper;
import org.paykesho.backoffice.paging.PagingAndSortingParam;
import org.paykesho.backoffice.settings.service.SettingService;
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
    public String listAllEventsByPage(@PagingAndSortingParam(listName = "eventList", moduleURL = "/events") PagingAndSortingHelper helper, @PathVariable("pageNum") int pageNum) throws PaykeshoError {
        eventsService.listByPage(pageNum, helper);

        return "events/events";
    }

    @GetMapping("/new")
    public String newEvent(Model model) {
        String pageTitle = "New Event";

        Announcement announcement = new Announcement();
        announcement.setEnabled(true);
        announcement.setRequiresResponse(false);
        announcement.setEventDate(new Date());

        model.addAttribute("event", announcement);
        model.addAttribute("pageTitle", pageTitle);
        return "events/event_form";
    }

    @PostMapping("/save")
    public String saveEvent(Announcement announcement, RedirectAttributes redirectAttributes, @RequestParam("image") MultipartFile multipartFile) throws IOException, PaykeshoError {

        DateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss");
        try {
            announcement.setEventDate(dateFormatter.parse(announcement.getUpdatedTimeOnForm()));
        } catch (ParseException e) {
            e.printStackTrace();
        }

        redirectAttributes.addFlashAttribute(Constants.MESSAGE, "event Operation Successful: " + announcement.getName());
        return getRedirectURLtoAffectedEvent(announcement);
    }

    @GetMapping("/edit/{id}")
    public String editEvent(@PathVariable(name = "id") Integer id, RedirectAttributes redirectAttributes, Model model) throws PaykeshoError {
        if (id == null) {
            return Constants.REDIRECT_TO_Events;
        }
        String pageTitle = "Edit Event";
        Announcement announcement = eventsService.getEventById(id);

        model.addAttribute("event", announcement);
        model.addAttribute("pageTitle", pageTitle);

        return "events/event_form";
    }

    @GetMapping("/{id}/enabled/{status}")
    public String updateEventEnabledStatus(@PathVariable(name = "id") Integer id, @PathVariable(name = "status") boolean enabled, RedirectAttributes redirectAttributes, Model model) throws PaykeshoError {

        if (id == null) {
            throw new PaykeshoError("no parameter id");
        }

        try {
            Announcement announcement = eventsService.getEventById(id);
            eventsService.updateEventStatus(id, enabled);
            String status = enabled ? "enabled" : "has been disabled";
            String operationMSG = announcement.getName() + " has been " + status;
            redirectAttributes.addFlashAttribute(Constants.MESSAGE, operationMSG);
        } catch (PaykeshoError supremaError) {
            redirectAttributes.addFlashAttribute(Constants.MESSAGE, supremaError.getMessage());
        }
        return Constants.REDIRECT_TO_Events;

    }

    private String getRedirectURLtoAffectedEvent(Announcement announcement) throws PaykeshoError {
        if (announcement == null) {
            throw new PaykeshoError("no event provided");
        }
        String eventName = announcement.getName();
        return Constants.REDIRECT_TO_Events_KEYWORD + eventName;
    }

    @GetMapping("/view/{id}")
    public String viewEvent(@PathVariable("id") Integer id, Model model, RedirectAttributes redirectAttributes) {
        try {

            Announcement announcement = eventsService.getEventById(id);

            model.addAttribute("event", announcement);

            return "events/event_view";
        } catch (PaykeshoError paykeshoError) {
            redirectAttributes.addFlashAttribute("message", paykeshoError.getMessage());
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
