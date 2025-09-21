package org.paykesho.clientportal;

import org.commons.entity.settings.Country;
import org.paykesho.clientportal.events.service.EventsService;
import org.paykesho.clientportal.settings.service.SettingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MainController {

    private static final Logger logger = LoggerFactory.getLogger(MainController.class);

    // @Autowired
    // private ClientService clientService; // Unused field
    // @Autowired
    // private OrganizationService organizationService; // Unused field
    // @Autowired
    // private MerchantService merchantService; // Unused field
    @Autowired
    private EventsService eventsService;
    @Autowired
    private SettingService settingService;

    @GetMapping("")
    public String viewHomePage(Model model) {
        try {
            logger.debug("Loading client portal home page");

            Country country = settingService.getCountryByName("Zambia");
            if (country != null) {
                model.addAttribute("country", country);
            } else {
                logger.warn("Country 'Zambia' not found in settings");
            }

            try {
                model.addAttribute("upcomingEvents", eventsService.getUpcomingEvents(30));
            } catch (Exception ex) {
                logger.warn("Failed to load upcoming events: {}", ex.getMessage());
                model.addAttribute("upcomingEvents", null);
            }

            logger.debug("Successfully loaded client portal home page");
            return "index";

        } catch (Exception ex) {
            logger.error("Error loading client portal home page: {}", ex.getMessage(), ex);
            model.addAttribute("errorMessage", "Failed to load home page. Please try again later.");
            return "error/500";
        }
    }

    @GetMapping("/faq")
    public String viewFAQPage(Model model) {
        try {
            logger.debug("Loading FAQ page");

            Country country = settingService.getCountryByName("Zambia");
            if (country != null) {
                model.addAttribute("country", country);
            } else {
                logger.warn("Country 'Zambia' not found in settings");
            }

            try {
                model.addAttribute("upcomingEvents", eventsService.getUpcomingEvents(30));
            } catch (Exception ex) {
                logger.warn("Failed to load upcoming events for FAQ page: {}", ex.getMessage());
                model.addAttribute("upcomingEvents", null);
            }

            return "faq";

        } catch (Exception ex) {
            logger.error("Error loading FAQ page: {}", ex.getMessage(), ex);
            model.addAttribute("errorMessage", "Failed to load FAQ page. Please try again later.");
            return "error/500";
        }
    }

    @GetMapping("/contact")
    public String viewContactPage(Model model) {
        try {
            logger.debug("Loading contact page");

            Country country = settingService.getCountryByName("Zambia");
            if (country != null) {
                model.addAttribute("country", country);
            } else {
                logger.warn("Country 'Zambia' not found in settings");
            }

            try {
                model.addAttribute("upcomingEvents", eventsService.getUpcomingEvents(30));
            } catch (Exception ex) {
                logger.warn("Failed to load upcoming events for contact page: {}", ex.getMessage());
                model.addAttribute("upcomingEvents", null);
            }

            return "contact";

        } catch (Exception ex) {
            logger.error("Error loading contact page: {}", ex.getMessage(), ex);
            model.addAttribute("errorMessage", "Failed to load contact page. Please try again later.");
            return "error/500";
        }
    }

    @GetMapping("/business")
    public String viewBusinessPage(Model model) {
        try {
            logger.debug("Loading business page");

            Country country = settingService.getCountryByName("Zambia");
            if (country != null) {
                model.addAttribute("country", country);
            } else {
                logger.warn("Country 'Zambia' not found in settings");
            }

            try {
                model.addAttribute("upcomingEvents", eventsService.getUpcomingEvents(30));
            } catch (Exception ex) {
                logger.warn("Failed to load upcoming events for business page: {}", ex.getMessage());
                model.addAttribute("upcomingEvents", null);
            }

            return "business";

        } catch (Exception ex) {
            logger.error("Error loading business page: {}", ex.getMessage(), ex);
            model.addAttribute("errorMessage", "Failed to load business page. Please try again later.");
            return "error/500";
        }
    }

    @GetMapping("/how-it-works")
    public String viewHowItWorksPage(Model model) {
        try {
            logger.debug("Loading how-it-works page");

            Country country = settingService.getCountryByName("Zambia");
            if (country != null) {
                model.addAttribute("country", country);
            } else {
                logger.warn("Country 'Zambia' not found in settings");
            }

            try {
                model.addAttribute("upcomingEvents", eventsService.getUpcomingEvents(30));
            } catch (Exception ex) {
                logger.warn("Failed to load upcoming events for how-it-works page: {}", ex.getMessage());
                model.addAttribute("upcomingEvents", null);
            }

            return "how_it_works";

        } catch (Exception ex) {
            logger.error("Error loading how-it-works page: {}", ex.getMessage(), ex);
            model.addAttribute("errorMessage", "Failed to load how-it-works page. Please try again later.");
            return "error/500";
        }
    }

    @GetMapping("/login")
    public String viewLoginPage(Model model) {
        try {
            logger.debug("Loading login page");
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
                model.addAttribute("pageTitle", "Sign Up");
                return "security/login";
            }
            return "redirect:/clients/dashboard";
        } catch (Exception ex) {
            logger.error("Error loading login page: {}", ex.getMessage(), ex);
            return "security/login";
        }
    }

    @GetMapping("/sign-up")
    public String viewSignUpPage(Model model) {
        try {
            logger.debug("Loading sign-up page");
            return "register/signup_home";
        } catch (Exception ex) {
            logger.error("Error loading sign-up page: {}", ex.getMessage(), ex);
            model.addAttribute("errorMessage", "Failed to load sign-up page. Please try again later.");
            return "error/500";
        }
    }
}
