package com.evlaleyla.gotournamentmanager.backend;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * Centralized exception handler for MVC controllers.
 *
 * <p>This advice converts common backend exceptions into a user-friendly error page
 * and ensures that an appropriate HTTP status code is returned.</p>
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static final String ERROR_VIEW = "app-error";
    private static final String PUBLIC_HEADER = "publicHeader";
    private static final String ADMIN_HEADER = "adminHeader";

    /**
     * Handles business or validation related exceptions that indicate a bad client request.
     *
     * @param exception the thrown exception
     * @param model the MVC model used for the error page
     * @param request the current HTTP request
     * @param response the current HTTP response
     * @return the name of the error view
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public String handleIllegalArgumentException(IllegalArgumentException exception,
                                                 Model model,
                                                 HttpServletRequest request,
                                                 HttpServletResponse response) {

        logger.warn(
                "Handled IllegalArgumentException for request URI '{}': {}",
                request.getRequestURI(),
                exception.getMessage()
        );

        response.setStatus(HttpStatus.BAD_REQUEST.value());

        prepareErrorModel(
                model,
                request,
                "Ungültige Anfrage",
                exception.getMessage()
        );

        return ERROR_VIEW;
    }

    /**
     * Handles database integrity violations, for example when delete or update operations
     * conflict with existing references or constraints.
     *
     * @param exception the thrown exception
     * @param model the MVC model used for the error page
     * @param request the current HTTP request
     * @param response the current HTTP response
     * @return the name of the error view
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public String handleDataIntegrityViolation(DataIntegrityViolationException exception,
                                               Model model,
                                               HttpServletRequest request,
                                               HttpServletResponse response) {

        logger.warn(
                "Handled DataIntegrityViolationException for request URI '{}': {}",
                request.getRequestURI(),
                exception.getMessage(),
                exception
        );

        response.setStatus(HttpStatus.CONFLICT.value());

        prepareErrorModel(
                model,
                request,
                "Aktion nicht möglich",
                "Die gewünschte Aktion konnte wegen bestehender Verknüpfungen oder ungültiger Daten nicht ausgeführt werden."
        );

        return ERROR_VIEW;
    }

    /**
     * Handles all unexpected exceptions that were not processed by more specific handlers.
     *
     * @param exception the thrown exception
     * @param model the MVC model used for the error page
     * @param request the current HTTP request
     * @param response the current HTTP response
     * @return the name of the error view
     */
    @ExceptionHandler(Exception.class)
    public String handleGenericException(Exception exception,
                                         Model model,
                                         HttpServletRequest request,
                                         HttpServletResponse response) {

        logger.error(
                "Unhandled exception for request URI '{}'",
                request.getRequestURI(),
                exception
        );

        response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());

        prepareErrorModel(
                model,
                request,
                "Unerwarteter Fehler",
                "Es ist ein unerwarteter Fehler aufgetreten. Bitte versuchen Sie es erneut."
        );

        return ERROR_VIEW;
    }

    /**
     * Populates the MVC model with all information required by the shared error page.
     *
     * @param model the MVC model
     * @param request the current HTTP request
     * @param errorTitle the title shown on the error page
     * @param errorMessage the message shown on the error page
     */
    private void prepareErrorModel(Model model,
                                   HttpServletRequest request,
                                   String errorTitle,
                                   String errorMessage) {
        model.addAttribute("errorTitle", errorTitle);
        model.addAttribute("errorMessage", errorMessage);
        model.addAttribute("headerFragment", resolveHeaderFragment(request));
    }

    /**
     * Determines which header fragment should be rendered on the error page
     * based on the current request path.
     *
     * <p>Public pages use the public header, while administrative pages use
     * the admin header.</p>
     *
     * @param request the current HTTP request
     * @return the Thymeleaf fragment name for the header
     */
    private String resolveHeaderFragment(HttpServletRequest request) {
        String requestUri = request.getRequestURI();

        if (requestUri == null || requestUri.isBlank()) {
            return PUBLIC_HEADER;
        }

        if (requestUri.equals("/") || requestUri.startsWith("/public")) {
            return PUBLIC_HEADER;
        }

        return ADMIN_HEADER;
    }
}