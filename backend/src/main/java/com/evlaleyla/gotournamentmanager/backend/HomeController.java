package com.evlaleyla.gotournamentmanager.backend;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller for the application's entry pages.
 *
 * <p>This controller provides the public landing page and the administrative
 * dashboard entry page.</p>
 */
@Controller
public class HomeController {

    private static final Logger logger = LoggerFactory.getLogger(HomeController.class);

    private static final String PUBLIC_HOME_VIEW = "home";
    private static final String ADMIN_HOME_VIEW = "admin-home";

    /**
     * Displays the public start page of the application.
     *
     * @return the view name of the public home page
     */
    @GetMapping("/")
    public String publicHome() {
        logger.debug("Rendering public home page.");
        return PUBLIC_HOME_VIEW;
    }

    /**
     * Displays the administrative start page of the application.
     *
     * @return the view name of the admin home page
     */
    @GetMapping("/admin")
    public String adminHome() {
        logger.debug("Rendering admin home page.");
        return ADMIN_HOME_VIEW;
    }
}