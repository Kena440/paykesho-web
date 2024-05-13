package org.chaz.membershipsite.events.controller;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.chaz.membershipsite.events.enums.Constants;
import org.chaz.membershipsite.events.service.EventCodeService;
import org.chaz.membershipsite.events.service.EventsService;
import org.chaz.membershipsite.memberadmins.service.MemberAdminService;
import org.chaz.membershipsite.memberinstitution.service.MemberInstitutionService;
import org.chaz.membershipsite.paging.PagingAndSortingHelper;
import org.chaz.membershipsite.paging.PagingAndSortingParam;
import org.membershipcommons.entity.event.Event;
import org.membershipcommons.entity.event.EventAccomodationOption;
import org.membershipcommons.entity.event.EventAttendeeRegistration;
import org.membershipcommons.entity.members.MemberAdmin;
import org.membershipcommons.entity.members.MemberInstitution;
import org.membershipcommons.utils.MembershipMgtError;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;

@Controller
@RequestMapping("/events")
public class EventsController {

    @Autowired
    private EventsService eventsService;
    @Autowired
    private EventCodeService eventCodeService;
    @Autowired
    private MemberAdminService memberAdminService;
    @Autowired
    private MemberInstitutionService memberInstitutionService;

    private static void addRegQrCodeToEachAttendee(List<EventAttendeeRegistration> attendeeRegistrations) throws WriterException, IOException {
        for (EventAttendeeRegistration e : attendeeRegistrations) {

            if (e.getEventRegId() != null && !e.getEventRegId().isEmpty()) {
                QRCodeWriter qrCodeWriter = new QRCodeWriter();
                BitMatrix bitMatrix = qrCodeWriter.encode(e.getEventRegId(), BarcodeFormat.QR_CODE, 200, 200);
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);

                byte[] imageBytes = outputStream.toByteArray();
                String base64Image = Base64.getEncoder().encodeToString(imageBytes);

                String dataUrl = "data:image/png;base64," + base64Image;
                e.setQrCode(dataUrl);
            }
        }
    }

    @GetMapping("")
    public String listFirstPage() {
        return Constants.REDIRECT_TO_Events_WITHOUT_KEYWORD;
    }

    @GetMapping("/page/{pageNum}")
    public String listAllEventsByPage(@PagingAndSortingParam(listName = "eventList", moduleURL = "/events") PagingAndSortingHelper helper, @PathVariable("pageNum") int pageNum) throws MembershipMgtError {
        eventsService.listByPage(pageNum, helper);

        return "events/events";
    }

    @GetMapping("/view/{id}")
    public String viewEvent(@PathVariable("id") Integer id, Model model, RedirectAttributes redirectAttributes, HttpServletRequest request) {
        try {
            MemberAdmin memberAdmin = memberAdminService.getAuthenticatedMemberAdmin(request);
            MemberInstitution memberInstitution = memberInstitutionService.getMemberInstitutionByMemberAdmin(memberAdmin);

            Event event = eventsService.getEventById(id);
            List<EventAttendeeRegistration> attendeeRegistrations = eventsService.getEventAttendeesByMemberInstitution(memberInstitution, event);
            List<EventAccomodationOption> eventAccomodationOptions;

            addRegQrCodeToEachAttendee(attendeeRegistrations);

            if (event.isRequiresResponse()) {
                eventAccomodationOptions = eventsService.getEventAccomodationOptions();
                model.addAttribute("eventAccomodationOptions", eventAccomodationOptions);
            }

            model.addAttribute("event", event);
            model.addAttribute("qrCodeContent", "/events/generateQRCode?qrContent=" + "https://membership.chaz.domains/events/registration/" + event.getId());
            model.addAttribute("attendeeRegistrations", attendeeRegistrations);
            model.addAttribute("memberInstitution", memberInstitution);

            return "events/event_view";
        } catch (MembershipMgtError | WriterException | IOException membershipMgtError) {
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

    @PostMapping("/save-attendee")
    public String saveEventAttendeeRegistration(RedirectAttributes redirectAttributes, HttpServletRequest request) throws MembershipMgtError {

        Event event = eventsService.getEventById(Integer.valueOf(request.getParameter("eventId")));
        MemberAdmin memberAdmin = memberAdminService.getAuthenticatedMemberAdmin(request);
        MemberInstitution memberInstitution = memberInstitutionService.getMemberInstitutionByMemberAdmin(memberAdmin);

        if (event == null) {
            throw new MembershipMgtError("No event");
        }

        String attendeeName = request.getParameter("attendeeName");
        String attendeePhoneNumber = request.getParameter("attendeePhoneNumber");
        String attendeeEmail = request.getParameter("attendeeEmail");
        String requiresAccomodation = request.getParameter("requiresAccomodation");
        String accomodationOption = request.getParameter("accomodationOption");
        String accomodationSharing = request.getParameter("accomodationSharing");

        EventAttendeeRegistration eventAttendeeRegistration = new EventAttendeeRegistration();
        eventAttendeeRegistration.setEvent(event);
        eventAttendeeRegistration.setEventRegId(String.valueOf(Instant.now().getEpochSecond()));
        eventAttendeeRegistration.setMemberInstitution(memberInstitution);
        eventAttendeeRegistration.setApproved(false);
        eventAttendeeRegistration.setRegistrationDate(new Date());
        eventAttendeeRegistration.setOrganization(memberInstitution.getName());
        eventAttendeeRegistration.setEmail(attendeeEmail);
        eventAttendeeRegistration.setPhoneNumber(attendeePhoneNumber);
        eventAttendeeRegistration.setName(attendeeName);
        eventAttendeeRegistration.setRequiresAccomodation(Boolean.parseBoolean(requiresAccomodation));

        if (Boolean.parseBoolean(requiresAccomodation)) {
            EventAccomodationOption eventAccomodationOption = eventsService.getEventAccomodationOptionById(Integer.valueOf(accomodationOption));
            if (eventAccomodationOption != null) {
                eventAttendeeRegistration.setEventAccomodationOption(eventAccomodationOption);
            }
            eventAttendeeRegistration.setAccomodationSharing(Boolean.parseBoolean(accomodationSharing));
        }

        EventAttendeeRegistration savedEventAttendeeRegistration = eventsService.saveAttendee(eventAttendeeRegistration);

        redirectAttributes.addFlashAttribute(Constants.MESSAGE, "Event Operation Successful: " + savedEventAttendeeRegistration.getName());
        return Constants.REDIRECT_TO_Event_ID + event.getId();
    }

}
