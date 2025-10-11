package org.paykesho.clientportal.events.controller;

import org.commons.entity.announcement.Announcement;
import org.commons.utils.PaykeshoError;
import org.paykesho.clientportal.events.enums.Constants;
import org.paykesho.clientportal.events.service.EventCodeService;
import org.paykesho.clientportal.events.service.EventsService;
import org.paykesho.clientportal.paging.PagingAndSortingHelper;
import org.paykesho.clientportal.paging.PagingAndSortingParam;
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
    public String listAllEventsByPage(@PagingAndSortingParam(listName = "eventList", moduleURL = "/events") PagingAndSortingHelper helper, @PathVariable("pageNum") int pageNum) throws PaykeshoError {
        eventsService.listByPage(pageNum, helper);

        return "events/events";
    }

    @GetMapping("/view/{uuid}")
    public String viewEvent(@PathVariable("uuid") String uuid, Model model, RedirectAttributes redirectAttributes, HttpServletRequest request) {
        try {

            Announcement announcement = eventsService.getEventByUuid(uuid);

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
