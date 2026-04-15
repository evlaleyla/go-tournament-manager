package com.evlaleyla.gotournamentmanager.backend;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public String handleIllegalArgumentException(IllegalArgumentException e,
                                                 Model model,
                                                 HttpServletResponse response) {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        model.addAttribute("errorTitle", "Ungültige Anfrage");
        model.addAttribute("errorMessage", e.getMessage());
        return "app-error";
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public String handleDataIntegrityViolation(DataIntegrityViolationException e,
                                               Model model,
                                               HttpServletResponse response) {
        response.setStatus(HttpServletResponse.SC_CONFLICT);
        model.addAttribute("errorTitle", "Aktion nicht möglich");
        model.addAttribute("errorMessage",
                "Die gewünschte Aktion konnte wegen bestehender Verknüpfungen oder ungültiger Daten nicht ausgeführt werden.");
        return "app-error";
    }

    @ExceptionHandler(Exception.class)
    public String handleGenericException(Exception e,
                                         Model model,
                                         HttpServletResponse response) {
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        model.addAttribute("errorTitle", "Unerwarteter Fehler");
        model.addAttribute("errorMessage",
                "Es ist ein unerwarteter Fehler aufgetreten. Bitte versuchen Sie es erneut.");
        return "app-error";
    }
}