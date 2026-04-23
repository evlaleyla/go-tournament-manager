package com.evlaleyla.gotournamentmanager.backend;

import jakarta.servlet.http.HttpServletRequest;
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
                                                 HttpServletRequest request,
                                                 HttpServletResponse response) {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        prepareErrorModel(model, request, "Ungültige Anfrage", e.getMessage());
        return "app-error";
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public String handleDataIntegrityViolation(DataIntegrityViolationException e,
                                               Model model,
                                               HttpServletRequest request,
                                               HttpServletResponse response) {
        response.setStatus(HttpServletResponse.SC_CONFLICT);
        prepareErrorModel(
                model,
                request,
                "Aktion nicht möglich",
                "Die gewünschte Aktion konnte wegen bestehender Verknüpfungen oder ungültiger Daten nicht ausgeführt werden."
        );
        return "app-error";
    }

    @ExceptionHandler(Exception.class)
    public String handleGenericException(Exception e,
                                         Model model,
                                         HttpServletRequest request,
                                         HttpServletResponse response) {
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        prepareErrorModel(
                model,
                request,
                "Unerwarteter Fehler",
                "Es ist ein unerwarteter Fehler aufgetreten. Bitte versuchen Sie es erneut."
        );
        return "app-error";
    }

    private void prepareErrorModel(Model model,
                                   HttpServletRequest request,
                                   String errorTitle,
                                   String errorMessage) {
        model.addAttribute("errorTitle", errorTitle);
        model.addAttribute("errorMessage", errorMessage);
        model.addAttribute("headerFragment", resolveHeaderFragment(request));
    }

    private String resolveHeaderFragment(HttpServletRequest request) {
        String uri = request.getRequestURI();

        if (uri == null || uri.isBlank()) {
            return "publicHeader";
        }

        if (uri.equals("/") || uri.startsWith("/public")) {
            return "publicHeader";
        }

        return "adminHeader";
    }
}