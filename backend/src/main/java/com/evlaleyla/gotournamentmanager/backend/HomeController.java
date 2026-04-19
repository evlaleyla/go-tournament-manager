package com.evlaleyla.gotournamentmanager.backend;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String publicHome() {
        return "home";
    }

    @GetMapping("/admin")
    public String adminHome() {
        return "admin-home";
    }
}