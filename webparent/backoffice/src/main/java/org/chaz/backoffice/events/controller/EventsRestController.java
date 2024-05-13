package org.chaz.backoffice.events.controller;

import org.chaz.backoffice.events.service.EventsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/events/rest")
public class EventsRestController {

    @Autowired
    private EventsService eventsService;

    @PostMapping("/check-event")
    public String checkDuplicateCode(Integer id, String name) {
        return eventsService.isEventUnique(id, name) ? "OK" : "Duplicated";
    }
}
