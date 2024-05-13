package org.chaz.backoffice.events.controller;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import org.chaz.backoffice.config.Utility;
import org.chaz.backoffice.events.enums.Constants;
import org.chaz.backoffice.events.fileHandlers.EventExcelExporter;
import org.chaz.backoffice.events.service.EventCodeService;
import org.chaz.backoffice.events.service.EventsService;
import org.chaz.backoffice.paging.PagingAndSortingHelper;
import org.chaz.backoffice.paging.PagingAndSortingParam;
import org.chaz.backoffice.settings.email.EmailSettingBag;
import org.chaz.backoffice.settings.service.SettingService;
import org.chaz.backoffice.utils.AmazonS3Util;
import org.membershipcommons.entity.State;
import org.membershipcommons.entity.event.*;
import org.membershipcommons.enums.AttendeeType;
import org.membershipcommons.enums.PaymentMethod;
import org.membershipcommons.utils.MembershipMgtError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.imageio.ImageIO;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;
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

    private static void addRegQrCodeToEachAttendee(List<EventAttendeeRegistration> attendeeRegistrations, List<EventVendorRegistration> vendorRegistrations) throws WriterException, IOException {
        int width = 200;
        int height = 100; // Adjust the height as needed
        BarcodeFormat barcodeFormat = BarcodeFormat.CODE_128;

        for (EventAttendeeRegistration e : attendeeRegistrations) {

            if (e.getEventRegId() != null && !e.getEventRegId().isEmpty()) {
                BitMatrix bitMatrix = new MultiFormatWriter().encode(e.getEventRegId(), barcodeFormat, width, height);
                BufferedImage bufferedImage = MatrixToImageWriter.toBufferedImage(bitMatrix);

                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                ImageIO.write(bufferedImage, "png", outputStream);

                byte[] imageBytes = outputStream.toByteArray();
                String base64Image = Base64.getEncoder().encodeToString(imageBytes);

                String dataUrl = "data:image/png;base64," + base64Image;
                e.setQrCode(dataUrl);
            }
        }

        for (EventVendorRegistration v : vendorRegistrations) {

            if (v.getEventRegId() != null && !v.getEventRegId().isEmpty()) {
                BitMatrix bitMatrix = new MultiFormatWriter().encode(v.getEventRegId(), barcodeFormat, width, height);
                BufferedImage bufferedImage = MatrixToImageWriter.toBufferedImage(bitMatrix);

                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                ImageIO.write(bufferedImage, "png", outputStream);

                byte[] imageBytes = outputStream.toByteArray();
                String base64Image = Base64.getEncoder().encodeToString(imageBytes);

                String dataUrl = "data:image/png;base64," + base64Image;
                v.setQrCode(dataUrl);
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
    public String saveEvent(Event event, RedirectAttributes redirectAttributes, @RequestParam("image") MultipartFile multipartFile) throws IOException, MembershipMgtError {

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
    public String editEvent(@PathVariable(name = "id") Integer id, RedirectAttributes redirectAttributes, Model model) throws MembershipMgtError {
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
    public String deleteEvent(@PathVariable(name = "id") Integer id, RedirectAttributes redirectAttributes) throws MembershipMgtError {
        if (id == null) {
            throw new MembershipMgtError("no parameter id");
        }

        eventsService.delete(id);

        String eventPhotosDir = "event_photos/" + id;
        AmazonS3Util.removeFolder(eventPhotosDir);
        redirectAttributes.addFlashAttribute(Constants.MESSAGE, "event deleted successfully");
        return Constants.REDIRECT_TO_Events;
    }

    @GetMapping("/{id}/enabled/{status}")
    public String updateEventEnabledStatus(@PathVariable(name = "id") Integer id, @PathVariable(name = "status") boolean enabled, RedirectAttributes redirectAttributes, Model model) throws MembershipMgtError {

        if (id == null) {
            throw new MembershipMgtError("no parameter id");
        }

        try {
            Event event = eventsService.getEventById(id);
            eventsService.updateEventStatus(id, enabled);
            String status = enabled ? "enabled" : "has been disabled";
            String operationMSG = event.getName() + " has been " + status;
            redirectAttributes.addFlashAttribute(Constants.MESSAGE, operationMSG);
        } catch (MembershipMgtError supremaError) {
            redirectAttributes.addFlashAttribute(Constants.MESSAGE, supremaError.getMessage());
        }
        return Constants.REDIRECT_TO_Events;

    }

    private String getRedirectURLtoAffectedEvent(Event event) throws MembershipMgtError {
        if (event == null) {
            throw new MembershipMgtError("no event provided");
        }
        String eventName = event.getName();
        return Constants.REDIRECT_TO_Events_KEYWORD + eventName;
    }

    @GetMapping("/view/{id}")
    public String viewEvent(@PathVariable("id") Integer id, Model model, RedirectAttributes redirectAttributes) {
        try {

            Event event = eventsService.getEventById(id);

            List<EventAttendeeRegistration> attendeeRegistrations = eventsService.getEventAttendees(event);
            List<EventVendorRegistration> vendorRegistrations = eventsService.getEventVendors(event);
            List<EventAttendeeAttendance> eventAttendeeAttendances = eventsService.getEventAttendeeAttendance(event);
            List<EventAccomodationOption> eventAccomodationOptions;
            List<EventVendorPackage> packages = eventsService.getVendorPackages();
            List<State> stateList = settingService.listStatesByCountry("Zambia");

            addRegQrCodeToEachAttendee(attendeeRegistrations, vendorRegistrations);

            if (event.isRequiresResponse()) {
                eventAccomodationOptions = eventsService.getEventAccomodationOptions();
                model.addAttribute("eventAccomodationOptions", eventAccomodationOptions);
            }

            model.addAttribute("stateList", stateList);
            model.addAttribute("packages", packages);
            model.addAttribute("event", event);
            model.addAttribute("eventLink", "https://membership.chaz.domains/events/registration/" + event.getId());
            model.addAttribute("qrCodeContent", "/events/generateQRCode?qrContent=" + "https://membership.chaz.domains/events/registration/" + event.getId());
            model.addAttribute("attendeeRegistrations", attendeeRegistrations);
            model.addAttribute("vendorRegistrations", vendorRegistrations);
            model.addAttribute("eventAttendeeAttendances", eventAttendeeAttendances);

            return "events/event_view";
        } catch (MembershipMgtError | WriterException | IOException membershipMgtError) {
            redirectAttributes.addFlashAttribute("message", membershipMgtError.getMessage());
            return "redirect:/events";
        }
    }

    @PostMapping("/save-attendee")
    public String saveEventAttendeeRegistration(RedirectAttributes redirectAttributes, HttpServletRequest request) throws MembershipMgtError {

        Event event = eventsService.getEventById(Integer.valueOf(request.getParameter("eventId")));

        if (event == null) {
            throw new MembershipMgtError("No event");
        }

        if (event.isEnabled()) {
            String attendeeTitlePosition = request.getParameter("attendeeTitlePosition");
            String attendeeType = request.getParameter("attendeeType");
            String attendeeName = request.getParameter("attendeeName");
            String attendeePhoneNumber = request.getParameter("attendeePhoneNumber");
            String attendeeEmail = request.getParameter("attendeeEmail");
            String attendeeOrganization = request.getParameter("attendeeOrganization");
            String attendeeProvince = request.getParameter("attendeeProvince");
            String attendeeDistrict = request.getParameter("attendeeDistrict");
            String requiresAccomodation = request.getParameter("requiresAccomodation");
            String accomodationOption = request.getParameter("accomodationOption");
            String accomodationSharing = request.getParameter("accomodationSharing");
            String attendeeNRC = request.getParameter("attendeeNRC");

            EventAttendeeRegistration eventAttendeeRegistration = new EventAttendeeRegistration();
            eventAttendeeRegistration.setEvent(event);
            eventAttendeeRegistration.setEventRegId(String.valueOf(Instant.now().getEpochSecond()));
            eventAttendeeRegistration.setPackNumber(getPackNumber(event));
            eventAttendeeRegistration.setApproved(false);
            eventAttendeeRegistration.setRegistrationDate(new Date());
            eventAttendeeRegistration.setOrganization(attendeeOrganization);
            eventAttendeeRegistration.setProvince(settingService.getProvinceByName(attendeeProvince));
            eventAttendeeRegistration.setDistrict(attendeeDistrict);
            eventAttendeeRegistration.setEmail(attendeeEmail);
            eventAttendeeRegistration.setNrc(attendeeNRC);
            eventAttendeeRegistration.setPhoneNumber(attendeePhoneNumber);
            eventAttendeeRegistration.setName(attendeeName);
            eventAttendeeRegistration.setTitlePosition(attendeeTitlePosition);
            eventAttendeeRegistration.setAttendeeType(AttendeeType.valueOf(attendeeType));
            eventAttendeeRegistration.setRequiresAccomodation(Boolean.parseBoolean(requiresAccomodation));

            if (Boolean.parseBoolean(requiresAccomodation)) {
                EventAccomodationOption eventAccomodationOption = eventsService.getEventAccomodationOptionById(Integer.valueOf(accomodationOption));
                if (eventAccomodationOption != null) {
                    eventAttendeeRegistration.setEventAccomodationOption(eventAccomodationOption);
                }
                eventAttendeeRegistration.setAccomodationSharing(Boolean.parseBoolean(accomodationSharing));
            }


            EventAttendeeRegistration savedEventAttendeeRegistration = eventsService.saveAttendee(eventAttendeeRegistration);

            if (savedEventAttendeeRegistration.getEmail() != null) {
                sendEmailToContactPersonThread(savedEventAttendeeRegistration);
            }

            redirectAttributes.addFlashAttribute(Constants.MESSAGE, "Event Operation Successful: " + savedEventAttendeeRegistration.getName());
            return Constants.REDIRECT_TO_Event_ID + event.getId();
        } else {
            return Constants.REDIRECT_TO_Events;
        }

    }

    private String getPackNumber(Event event) throws MembershipMgtError {
        String packNumber = "";
        int listSize = eventsService.getEventAttendees(event).size();

        int adjustedSize = listSize + 1;

        if (listSize >= 100) {
            packNumber = String.valueOf(adjustedSize);
        } else if (listSize >= 10) {
            packNumber = "0" + adjustedSize;
        } else {
            packNumber = "00" + adjustedSize;
        }
        return packNumber;
    }

    @PostMapping("/save-vendor")
    public String saveEventVendorRegistration(RedirectAttributes redirectAttributes, HttpServletRequest request) throws MembershipMgtError {

        Event event = eventsService.getEventById(Integer.valueOf(request.getParameter("eventId")));

        if (event == null) {
            throw new MembershipMgtError("No event");
        }

        if (event.isEnabled()) {

            String vendorName = request.getParameter("vendorName");
            String vendorPhoneNumber = request.getParameter("vendorPhoneNumber");
            String vendorEmail = request.getParameter("vendorEmail");
            String vendorOrganization = request.getParameter("vendorOrganization");
            String eventVendorPackage = request.getParameter("eventVendorPackage");
            String paymentMethod = request.getParameter("paymentMethod");

            EventVendorRegistration eventVendorRegistration = new EventVendorRegistration();
            eventVendorRegistration.setEvent(event);
            eventVendorRegistration.setEventRegId(String.valueOf(Instant.now().getEpochSecond()));
            eventVendorRegistration.setApproved(false);
            eventVendorRegistration.setRegistrationDate(new Date());
            eventVendorRegistration.setOrganization(vendorOrganization);
            eventVendorRegistration.setEmail(vendorEmail);
            eventVendorRegistration.setPhoneNumber(vendorPhoneNumber);
            eventVendorRegistration.setName(vendorName);
            eventVendorRegistration.setPaymentMethod(PaymentMethod.valueOf(paymentMethod));

            EventVendorPackage vendorPackage = eventsService.getEventVendorPackageById(Integer.valueOf(eventVendorPackage));
            if (vendorPackage != null) {
                eventVendorRegistration.setEventVendorPackage(vendorPackage);
            }

            EventVendorRegistration savedEventVendorRegistration = eventsService.saveVendor(eventVendorRegistration);

            redirectAttributes.addFlashAttribute(Constants.MESSAGE, "Event Operation Successful: " + savedEventVendorRegistration.getName());
            return Constants.REDIRECT_TO_Event_ID + event.getId();

        } else {
            return Constants.REDIRECT_TO_Events;
        }

    }

    @GetMapping("/generateQRCode")
    public void generateQRCode(String qrContent, HttpServletResponse response) throws IOException {
        response.setContentType("image/png");
        byte[] qrCode = eventCodeService.generateQRCode(qrContent, 500, 500);
        OutputStream outputStream = response.getOutputStream();
        outputStream.write(qrCode);
    }

    @GetMapping("/approve-attendee/{id}")
    public String approveAttendee(@PathVariable("id") Integer id, RedirectAttributes redirectAttributes) {
        try {

            EventAttendeeRegistration eventAttendeeRegistration = eventsService.getEventAttendeeById(id);
            eventAttendeeRegistration.setApproved(true);
            eventsService.saveAttendee(eventAttendeeRegistration);

            return Constants.REDIRECT_TO_Event_ID + eventAttendeeRegistration.getEvent().getId();

        } catch (MembershipMgtError membershipMgtError) {
            redirectAttributes.addFlashAttribute("message", membershipMgtError.getMessage());
            return "redirect:/events";
        }
    }

    @GetMapping("/accommodation-assigned/{id}")
    public String attendeeAccommodationAssigned(@PathVariable("id") Integer id, RedirectAttributes redirectAttributes) {
        try {

            EventAttendeeRegistration eventAttendeeRegistration = eventsService.getEventAttendeeById(id);
            eventAttendeeRegistration.setApproved(true);
            eventsService.saveAttendee(eventAttendeeRegistration);

            return Constants.REDIRECT_TO_Event_ID + eventAttendeeRegistration.getEvent().getId();

        } catch (MembershipMgtError membershipMgtError) {
            redirectAttributes.addFlashAttribute("message", membershipMgtError.getMessage());
            return "redirect:/events";
        }
    }

    @GetMapping("/approve-vendor/{id}")
    public String approveVendor(@PathVariable("id") Integer id, RedirectAttributes redirectAttributes) {
        try {

            EventVendorRegistration eventVendorRegistration = eventsService.getEventVendorRegistrationById(id);
            eventVendorRegistration.setApproved(true);
            eventsService.saveVendor(eventVendorRegistration);

            return Constants.REDIRECT_TO_Event_ID + eventVendorRegistration.getEvent().getId();

        } catch (MembershipMgtError membershipMgtError) {
            redirectAttributes.addFlashAttribute("message", membershipMgtError.getMessage());
            return "redirect:/events";
        }
    }

    @GetMapping("/registration/{id}")
    public String registrationHome(@PathVariable("id") Integer id, Model model) throws MembershipMgtError {
        String pageTitle = "Event Registration";
        Event event = eventsService.getEventById(id);

        if (event == null) {
            throw new MembershipMgtError("No event");
        }

        model.addAttribute("event", event);
        model.addAttribute("qrCodeContent", "/events/generateQRCode?qrContent=" + "https://membership.chaz.domains/events/registration/" + event.getId());
        model.addAttribute("pageTitle", pageTitle);
        return "events/registration";
    }

    @GetMapping("/registration/vendor/{id}")
    public String vendorRegistration(@PathVariable("id") Integer id, Model model) throws MembershipMgtError {
        String pageTitle = "Vendor";
        Event event = eventsService.getEventById(id);

        if (event == null) {
            throw new MembershipMgtError("No event");
        }

        List<EventVendorPackage> packages = eventsService.getVendorPackages();

        EventVendorRegistration eventVendorRegistration = new EventVendorRegistration();
        eventVendorRegistration.setEvent(event);

        model.addAttribute("event", event);
        model.addAttribute("packages", packages);
        model.addAttribute("eventVendorRegistration", eventVendorRegistration);
        model.addAttribute("qrCodeContent", "/events/generateQRCode?qrContent=" + "https://membership.chaz.domains/events/registration/" + event.getId());
        model.addAttribute("pageTitle", pageTitle);
        return "events/registration_vendor";
    }

    @GetMapping("/registration/attendee/{id}")
    public String attendeeRegistration(@PathVariable("id") Integer id, Model model) throws MembershipMgtError {
        String pageTitle = "Attendee";
        Event event = eventsService.getEventById(id);

        if (event == null) {
            throw new MembershipMgtError("No event");
        }

        if (event.isEnabled()) {
            List<State> stateList = settingService.listStatesByCountry("Zambia");

            EventAttendeeRegistration eventAttendeeRegistration = new EventAttendeeRegistration();
            eventAttendeeRegistration.setEvent(event);

            List<EventAccomodationOption> eventAccomodationOptions;

            if (event.isRequiresResponse()) {
                eventAccomodationOptions = eventsService.getEventAccomodationOptions();
                model.addAttribute("eventAccomodationOptions", eventAccomodationOptions);
            }

            model.addAttribute("event", event);
            model.addAttribute("stateList", stateList);
            model.addAttribute("eventAttendeeRegistration", eventAttendeeRegistration);
            model.addAttribute("qrCodeContent", "/events/generateQRCode?qrContent=" + "https://membership.chaz.domains/events/registration/" + event.getId());
            model.addAttribute("pageTitle", pageTitle);
            return "events/registration_attendee";
        } else {
            return Constants.REDIRECT_TO_Events + event.getId();
        }

    }

    @PostMapping("/registration/attendee/save")
    public String saveEventAttendeeRegistration(EventAttendeeRegistration eventAttendeeRegistration, Model model) throws MembershipMgtError {
        if (eventAttendeeRegistration.getEvent() == null) {
            throw new MembershipMgtError("No event");
        }
        String feedback = "";

        if (eventAttendeeRegistration.getEvent().isEnabled()) {

            eventAttendeeRegistration.setEventRegId(String.valueOf(Instant.now().getEpochSecond()));
            eventAttendeeRegistration.setPackNumber(getPackNumber(eventAttendeeRegistration.getEvent()));
            eventAttendeeRegistration.setApproved(false);
            eventAttendeeRegistration.setAttendeeType(AttendeeType.DELEGATE);
            eventAttendeeRegistration.setRegistrationDate(new Date());

            EventAttendeeRegistration savedEventAttendeeRegistration = eventsService.saveAttendee(eventAttendeeRegistration);

            if (savedEventAttendeeRegistration != null) {
                feedback = "Congratulations!, you have successfully registered as attendee for " + eventAttendeeRegistration.getEvent().getName();

                if (savedEventAttendeeRegistration.getEmail() != null) {
                    sendEmailToContactPersonThread(savedEventAttendeeRegistration);
                }
            } else {
                feedback = "Error in registration";
            }

            model.addAttribute("event", eventAttendeeRegistration.getEvent());
            model.addAttribute("feedback", feedback);

            return "events/registration_feedback";

        } else {
            return "redirect:/";
        }

    }

    @PostMapping("/registration/vendor/save")
    public String saveEventVendorRegistration(EventVendorRegistration eventVendorRegistration, Model model) throws MembershipMgtError {
        if (eventVendorRegistration.getEvent() == null) {
            throw new MembershipMgtError("No event");
        }

        if (eventVendorRegistration.getEvent().isEnabled()) {

            String feedback = "";
            eventVendorRegistration.setApproved(false);
            eventVendorRegistration.setEventRegId(String.valueOf(Instant.now().getEpochSecond()));

            eventVendorRegistration.setRegistrationDate(new Date());

            EventVendorRegistration savedEventVendorRegistration = eventsService.saveVendor(eventVendorRegistration);

            if (savedEventVendorRegistration != null) {
                feedback = "Congratulations!, you have successfully registered as vendor for " + eventVendorRegistration.getEvent().getName();
            } else {
                feedback = "Error in registration";
            }

            model.addAttribute("event", eventVendorRegistration.getEvent());
            model.addAttribute("feedback", feedback);

            return "events/registration_feedback";


        } else {
            return "redirect:/";
        }
    }

    @GetMapping("/export/{format}/{type}/{eventId}")
    public void exportUsersByFormat(@PathVariable(name = "format") String format, @PathVariable(name = "type") String type, @PathVariable(name = "eventId") Integer eventId, HttpServletResponse response) throws MembershipMgtError, IOException, WriterException {
        switch (format) {
            case "csv":
                //exportToCSV(response);
                break;
            case "excel":
                exportToExcel(response, type, eventId);
                break;
            case "pdf":
                //exportToPDF(response);
                break;
            default:
                throw new MembershipMgtError("no format provided");
        }
    }

    public void exportToExcel(HttpServletResponse response, String type, Integer eventId) throws IOException, MembershipMgtError, WriterException {
        Event event = eventsService.getEventById(eventId);

        if (event == null) {
            throw new MembershipMgtError("No event");
        }

        List<EventVendorRegistration> eventVendorRegistrations = null;
        List<EventAttendeeRegistration> eventAttendeeRegistrations = null;
        List<EventAttendeeAttendance> eventAttendeeAttendances = null;
        EventExcelExporter exporter = new EventExcelExporter();

        switch (type) {
            case "attendees" -> {
                eventAttendeeRegistrations = eventsService.getEventAttendees(event);
                exporter.exportAttendee(eventAttendeeRegistrations, response);
            }
            case "vendors" -> {
                eventVendorRegistrations = eventsService.getEventVendors(event);
                exporter.exportVendors(eventVendorRegistrations, response);
            }
            case "attendance" -> {
                eventAttendeeAttendances = eventsService.getEventAttendeeAttendance(event);
                exporter.exportAttendeeAttendances(eventAttendeeAttendances, response);
            }
            default -> {
            }
        }

    }

    @GetMapping("/attendee/delete/{id}")
    public String deleteAttendee(@PathVariable("id") Integer id, RedirectAttributes redirectAttributes) {
        try {

            EventAttendeeRegistration eventAttendeeRegistration = eventsService.getEventAttendeeById(id);
            eventsService.deleteAttendee(eventAttendeeRegistration);

            return Constants.REDIRECT_TO_Event_ID + eventAttendeeRegistration.getEvent().getId();

        } catch (MembershipMgtError membershipMgtError) {
            redirectAttributes.addFlashAttribute("message", membershipMgtError.getMessage());
            return "redirect:/events";
        }
    }

    @GetMapping("/vendor/delete/{id}")
    public String deleteVendor(@PathVariable("id") Integer id, RedirectAttributes redirectAttributes) {
        try {

            EventVendorRegistration eventVendorRegistration = eventsService.getEventVendorRegistrationById(id);
            eventsService.deleteVendor(eventVendorRegistration);

            return Constants.REDIRECT_TO_Event_ID + eventVendorRegistration.getEvent().getId();

        } catch (MembershipMgtError membershipMgtError) {
            redirectAttributes.addFlashAttribute("message", membershipMgtError.getMessage());
            return "redirect:/events";
        }
    }

    @GetMapping("/attendee/retrieve/{barcodeData}")
    public String retrieveAttendeeViaBarcode(@PathVariable("barcodeData") String barcodeData, Model model) throws MembershipMgtError {
        EventAttendeeRegistration eventAttendeeRegistration = eventsService.getEventAttendeeByEventRegId(barcodeData);
        model.addAttribute("eventAttendeeRegistration", eventAttendeeRegistration);
        return "events/attendee_attendance_modal";
    }

    @PostMapping("/attendance/save-attendance")
    public String saveEventAttendeeAttendance(RedirectAttributes redirectAttributes, HttpServletRequest request) throws MembershipMgtError {

        Event event = eventsService.getEventById(Integer.valueOf(request.getParameter("eventId")));


        if (event == null) {
            throw new MembershipMgtError("No event");
        }

        if (event.isEnabled()) {
            EventAttendeeRegistration eventAttendeeRegistration = eventsService.getEventAttendeeById(Integer.valueOf(request.getParameter("eventAttendeeId")));

            if (eventAttendeeRegistration == null) {
                throw new MembershipMgtError("No event");
            }

            Integer daySelect = Integer.valueOf(request.getParameter("daySelect"));

            EventAttendeeAttendance eventAttendeeAttendance = new EventAttendeeAttendance();
            eventAttendeeAttendance.setEventAttendeeRegistration(eventAttendeeRegistration);
            eventAttendeeAttendance.setSignedDate(new Date());
            eventAttendeeAttendance.setEventDay(daySelect);

            eventsService.saveAttendeeAttendance(eventAttendeeAttendance);

            redirectAttributes.addFlashAttribute(Constants.MESSAGE, "Event Operation Successful");
            return Constants.REDIRECT_TO_Event_ID + event.getId();

        } else {
            return Constants.REDIRECT_TO_Events;
        }

    }

    @GetMapping("/attendee-attendance/delete/{id}")
    public String deleteAttendeeAttendance(@PathVariable("id") Integer id, RedirectAttributes redirectAttributes) {
        try {

            EventAttendeeAttendance eventAttendeeAttendance = eventsService.getEventAttendeeAttendanceById(id);
            eventsService.deleteEventAttendeeAttendance(eventAttendeeAttendance);

            return Constants.REDIRECT_TO_Event_ID + eventAttendeeAttendance.getEventAttendeeRegistration().getEvent().getId();

        } catch (MembershipMgtError membershipMgtError) {
            redirectAttributes.addFlashAttribute("message", membershipMgtError.getMessage());
            return "redirect:/events";
        }
    }

    @GetMapping("/attendee/print-id/{id}")
    public String printAttendeeId(@PathVariable("id") Integer id, RedirectAttributes redirectAttributes, Model model) {
        try {

            EventAttendeeRegistration eventAttendeeRegistration = eventsService.getEventAttendeeById(id);

            if (eventAttendeeRegistration.getEventRegId() != null && !eventAttendeeRegistration.getEventRegId().isEmpty()) {
                int width = 80;
                int height = 40; // Adjust the height as needed
                BarcodeFormat barcodeFormat = BarcodeFormat.CODE_128;

                BitMatrix bitMatrix = new MultiFormatWriter().encode(eventAttendeeRegistration.getEventRegId(), barcodeFormat, width, height);
                BufferedImage bufferedImage = MatrixToImageWriter.toBufferedImage(bitMatrix);

                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                ImageIO.write(bufferedImage, "png", outputStream);

                byte[] imageBytes = outputStream.toByteArray();
                String base64Image = Base64.getEncoder().encodeToString(imageBytes);

                String dataUrl = "data:image/png;base64," + base64Image;
                eventAttendeeRegistration.setQrCode(dataUrl);
            }

            model.addAttribute("eventAttendeeRegistration", eventAttendeeRegistration);

            if (eventAttendeeRegistration.getAttendeeType().equals(AttendeeType.DELEGATE)) {
                model.addAttribute("frontImage", "/images/46th_delegate_id_front.png");
                model.addAttribute("backImage", "/images/46th_delegate_id_back.png");
            } else if (eventAttendeeRegistration.getAttendeeType().equals(AttendeeType.CHURCH_LEADER)) {
                model.addAttribute("frontImage", "/images/46th_church_leader_id_front.png");
                model.addAttribute("backImage", "/images/46th_delegate_id_back.png");
            } else if (eventAttendeeRegistration.getAttendeeType().equals(AttendeeType.BOARD_MEMBER)) {
                model.addAttribute("frontImage", "/images/46th_board_member_id_front.png");
                model.addAttribute("backImage", "/images/46th_delegate_id_back.png");
            } else if (eventAttendeeRegistration.getAttendeeType().equals(AttendeeType.CHAZ_ED)) {
                model.addAttribute("frontImage", "/images/46th_ed_id_front.png");
                model.addAttribute("backImage", "/images/46th_delegate_id_back.png");
            }

            return "events/46th_general_council_id";
        } catch (MembershipMgtError membershipMgtError) {
            redirectAttributes.addFlashAttribute("message", membershipMgtError.getMessage());
            return "redirect:/events";
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (WriterException e) {
            throw new RuntimeException(e);
        }
    }

    @GetMapping("/vendor/print-id/{id}")
    public String printVendorId(@PathVariable("id") Integer id, RedirectAttributes redirectAttributes, Model model) {
        try {

            EventVendorRegistration eventVendorRegistration = eventsService.getEventVendorRegistrationById(id);

            if (eventVendorRegistration.getEventRegId() != null && !eventVendorRegistration.getEventRegId().isEmpty()) {
                int width = 80;
                int height = 40; // Adjust the height as needed
                BarcodeFormat barcodeFormat = BarcodeFormat.CODE_128;

                BitMatrix bitMatrix = new MultiFormatWriter().encode(eventVendorRegistration.getEventRegId(), barcodeFormat, width, height);
                BufferedImage bufferedImage = MatrixToImageWriter.toBufferedImage(bitMatrix);

                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                ImageIO.write(bufferedImage, "png", outputStream);

                byte[] imageBytes = outputStream.toByteArray();
                String base64Image = Base64.getEncoder().encodeToString(imageBytes);

                String dataUrl = "data:image/png;base64," + base64Image;
                eventVendorRegistration.setQrCode(dataUrl);
            }

            model.addAttribute("eventVendorRegistration", eventVendorRegistration);

            model.addAttribute("frontImage", "/images/46th_vendor_id_front.png");
            model.addAttribute("backImage", "/images/46th_delegate_id_back.png");

            return "events/46th_general_council_exhibitor_id";
        } catch (MembershipMgtError membershipMgtError) {
            redirectAttributes.addFlashAttribute("message", membershipMgtError.getMessage());
            return "redirect:/events";
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (WriterException e) {
            throw new RuntimeException(e);
        }
    }

    private void sendEmailToContactPersonThread(EventAttendeeRegistration eventAttendeeRegistration) {
        new Thread(() -> {
            try {
                sendEmail(eventAttendeeRegistration);
            } catch (Exception e) {
                LOGGER.error(e.getMessage());
            }
        }).start();
    }

    private void sendEmail(EventAttendeeRegistration eventAttendeeRegistration) throws MessagingException, UnsupportedEncodingException {
        EmailSettingBag emailSettingBags = settingService.getEmailSettingsBag();
        JavaMailSenderImpl mailSender = Utility.prepareMailSender(emailSettingBags);
        String feedback = "Congratulations!, you have successfully registered as attendee for " + eventAttendeeRegistration.getEvent().getName();

        String toAddress = eventAttendeeRegistration.getEmail();
        String subject = "Registration " + eventAttendeeRegistration.getEvent().getName();
        String content = "<p>Greetings from CHAZ,</p>" + "<p>\"" + feedback + "\"</p>" + "<br>" + "<p>Event Reg ID #" + eventAttendeeRegistration.getEventRegId() + ".</p>" + "<p>Thank you.</p>";

        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage);

        helper.setFrom(emailSettingBags.getFromAddress(), emailSettingBags.getSenderName());
        helper.setTo(toAddress);
        helper.setSubject(subject);

        helper.setText(content, true);

        mailSender.send(mimeMessage);
    }

    private void sendEmailWithMessage(EventAttendeeRegistration eventAttendeeRegistration, String message) throws MessagingException, UnsupportedEncodingException {
        EmailSettingBag emailSettingBags = settingService.getEmailSettingsBag();
        JavaMailSenderImpl mailSender = Utility.prepareMailSender(emailSettingBags);

        String toAddress = eventAttendeeRegistration.getEmail();
        String subject = "Updates: " + eventAttendeeRegistration.getEvent().getName();
        String content = "<p>Greetings from CHAZ,</p>" + "<p>\"" + message + "\"</p>" + "<br>" + "<p>Event Reg ID #" + eventAttendeeRegistration.getEventRegId() + ".</p>" + "<p>Thank you.</p>";

        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage);

        helper.setFrom(emailSettingBags.getFromAddress(), emailSettingBags.getSenderName());
        helper.setTo(toAddress);
        helper.setSubject(subject);

        helper.setText(content, true);

        mailSender.send(mimeMessage);
    }

    //send reminder to all attendees
    @PostMapping("/attendees/send-reminder")
    public String sendReminderToAttendees(RedirectAttributes redirectAttributes, HttpServletRequest request) throws MembershipMgtError {

        Event event = eventsService.getEventById(Integer.valueOf(request.getParameter("eventId")));

        if (event == null) {
            throw new MembershipMgtError("No event");
        }

        if (event.isEnabled()) {

            String eventUpdateMessage = request.getParameter("eventUpdateMessage");

            List<EventAttendeeRegistration> eventAttendeeRegistrations = eventsService.getEventAttendees(event);

            if (eventAttendeeRegistrations != null) {
                for (EventAttendeeRegistration eventAttendeeRegistration : eventAttendeeRegistrations) {
                    new Thread(() -> {
                        try {
                            sendEmailWithMessage(eventAttendeeRegistration, eventUpdateMessage);
                        } catch (Exception e) {
                            LOGGER.error(e.getMessage());
                        }
                    }).start();
                }
            }

            redirectAttributes.addFlashAttribute(Constants.MESSAGE, "Event Operation Successful");
            return Constants.REDIRECT_TO_Event_ID + event.getId();

        } else {
            return Constants.REDIRECT_TO_Events;
        }

    }

}
