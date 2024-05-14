package org.chaz.portal.events.controller;

import org.chaz.portal.events.enums.Constants;
import org.chaz.portal.events.service.EventCodeService;
import org.chaz.portal.events.service.EventsService;
import org.chaz.portal.paging.PagingAndSortingHelper;
import org.chaz.portal.paging.PagingAndSortingParam;
import org.commons.entity.event.Event;
import org.commons.utils.AssessmentToolError;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;

@Controller
@RequestMapping("/events")
public class EventsController {

    @Autowired
    private EventsService eventsService;
    @Autowired
    private EventCodeService eventCodeService;


    @GetMapping("")
    public String listFirstPage() {
        return Constants.REDIRECT_TO_Events_WITHOUT_KEYWORD;
    }

    @GetMapping("/page/{pageNum}")
    public String listAllEventsByPage(@PagingAndSortingParam(listName = "eventList", moduleURL = "/events") PagingAndSortingHelper helper, @PathVariable("pageNum") int pageNum) throws AssessmentToolError {
        eventsService.listByPage(pageNum, helper);

        return "events/events";
    }

    @GetMapping("/view/{id}")
    public String viewEvent(@PathVariable("id") Integer id, Model model, RedirectAttributes redirectAttributes, HttpServletRequest request) {
        try {

            Event event = eventsService.getEventById(id);

            model.addAttribute("event", event);
            return "events/event_view";
        } catch (AssessmentToolError membershipMgtError) {
            redirectAttributes.addFlashAttribute("message", membershipMgtError.getMessage());
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
