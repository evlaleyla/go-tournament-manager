package com.evlaleyla.gotournamentmanager.backend.participant;

import com.evlaleyla.gotournamentmanager.backend.ClubOptions;
import com.evlaleyla.gotournamentmanager.backend.CountryOptions;
import com.evlaleyla.gotournamentmanager.backend.RankOptions;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Web controller for managing participant-related administration views and actions.
 *
 * <p>This controller is responsible for:
 * <ul>
 *     <li>listing and filtering participants,</li>
 *     <li>displaying create and edit forms,</li>
 *     <li>showing participant details,</li>
 *     <li>creating, updating, and deleting participants.</li>
 * </ul>
 *
 * <p>User-facing messages remain in German because the application's UI language is German.
 */
@Controller
public class ParticipantController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ParticipantController.class);

    private static final String PARTICIPANTS_VIEW = "participants";
    private static final String PARTICIPANT_FORM_VIEW = "participant-form";
    private static final String PARTICIPANT_DETAIL_VIEW = "participant-detail";

    private static final String REDIRECT_PARTICIPANTS = "redirect:/participants";

    private final ParticipantService participantService;

    public ParticipantController(ParticipantService participantService) {
        this.participantService = participantService;
    }

    /**
     * Displays the participant overview with optional filter criteria.
     */
    @GetMapping("/participants")
    public String showParticipants(@RequestParam(required = false) String firstName,
                                   @RequestParam(required = false) String lastName,
                                   @RequestParam(required = false) String country,
                                   @RequestParam(required = false) String club,
                                   @RequestParam(required = false) String rank,
                                   Model model) {

        LOGGER.debug(
                "Loading participant overview with filters: firstName='{}', lastName='{}', country='{}', club='{}', rank='{}'.",
                firstName, lastName, country, club, rank
        );

        model.addAttribute(
                "participants",
                participantService.search(firstName, lastName, country, club, rank)
        );
        model.addAttribute("firstName", firstName);
        model.addAttribute("lastName", lastName);
        model.addAttribute("selectedCountry", country);
        model.addAttribute("selectedClub", club);
        model.addAttribute("selectedRank", rank);

        addParticipantFilterOptions(model);

        return PARTICIPANTS_VIEW;
    }

    /**
     * Displays the empty form for creating a new participant.
     */
    @GetMapping("/participants/new")
    public String showParticipantForm(Model model) {
        LOGGER.debug("Opening participant creation form.");

        model.addAttribute("participant", new Participant());
        prepareParticipantForm(model, false, false);

        return PARTICIPANT_FORM_VIEW;
    }

    /**
     * Creates a new participant after validating the submitted form data.
     */
    @PostMapping("/participants")
    public String createParticipant(@Valid @ModelAttribute Participant participant,
                                    BindingResult bindingResult,
                                    Model model,
                                    RedirectAttributes redirectAttributes) {

        LOGGER.info("Received request to create a new participant.");

        if (bindingResult.hasErrors()) {
            LOGGER.warn("Participant creation failed due to validation errors. Error count: {}.",
                    bindingResult.getErrorCount());

            prepareParticipantForm(model, false, false);
            return PARTICIPANT_FORM_VIEW;
        }

        try {
            Participant savedParticipant = participantService.save(participant);

            LOGGER.info("Participant created successfully with id={}.", savedParticipant.getId());

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Teilnehmer wurde erfolgreich angelegt."
            );
            return REDIRECT_PARTICIPANTS;

        } catch (IllegalArgumentException e) {
            LOGGER.warn("Participant creation failed due to business validation: {}.", e.getMessage());

            bindingResult.reject("participant.invalid", e.getMessage());
            prepareParticipantForm(model, false, false);
            return PARTICIPANT_FORM_VIEW;
        }
    }

    /**
     * Displays the detail page for a single participant.
     */
    @GetMapping("/participants/{id}")
    public String showParticipantDetail(@PathVariable Long id, Model model) {
        LOGGER.debug("Loading participant detail page for participantId={}.", id);

        model.addAttribute("participant", participantService.findById(id));
        return PARTICIPANT_DETAIL_VIEW;
    }

    /**
     * Displays the edit form for an existing participant.
     */
    @GetMapping("/participants/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model) {
        LOGGER.debug("Opening participant edit form for participantId={}.", id);

        boolean nameLocked = participantService.isNameLocked(id);

        model.addAttribute("participant", participantService.findById(id));
        prepareParticipantForm(model, true, nameLocked);

        return PARTICIPANT_FORM_VIEW;
    }

    /**
     * Updates an existing participant after validating the submitted form data.
     */
    @PostMapping("/participants/{id}")
    public String updateParticipant(@PathVariable Long id,
                                    @Valid @ModelAttribute Participant participant,
                                    BindingResult bindingResult,
                                    Model model,
                                    RedirectAttributes redirectAttributes) {

        LOGGER.info("Received request to update participantId={}.", id);

        participant.setId(id);
        boolean nameLocked = participantService.isNameLocked(id);

        if (bindingResult.hasErrors()) {
            LOGGER.warn("Participant update failed due to validation errors for participantId={}. Error count: {}.",
                    id, bindingResult.getErrorCount());

            prepareParticipantForm(model, true, nameLocked);
            return PARTICIPANT_FORM_VIEW;
        }

        try {
            participantService.update(id, participant);

            LOGGER.info("Participant updated successfully for participantId={}.", id);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Teilnehmer wurde erfolgreich aktualisiert."
            );
            return "redirect:/participants/" + id;

        } catch (IllegalArgumentException e) {
            LOGGER.warn("Participant update failed for participantId={} due to business validation: {}.",
                    id, e.getMessage());

            bindingResult.reject("participant.invalid", e.getMessage());
            prepareParticipantForm(model, true, nameLocked);
            return PARTICIPANT_FORM_VIEW;
        }
    }

    /**
     * Deletes an existing participant if no blocking references exist.
     */
    @PostMapping("/participants/{id}/delete")
    public String deleteParticipant(@PathVariable Long id,
                                    RedirectAttributes redirectAttributes) {

        LOGGER.info("Received request to delete participantId={}.", id);

        try {
            participantService.deleteById(id);

            LOGGER.info("Participant deleted successfully for participantId={}.", id);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Teilnehmer wurde erfolgreich gelöscht."
            );
            return REDIRECT_PARTICIPANTS;

        } catch (IllegalArgumentException e) {
            LOGGER.warn("Participant deletion failed for participantId={} due to business validation: {}.",
                    id, e.getMessage());

            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/participants/" + id;
        }
    }

    /**
     * Adds common model attributes required by the participant form.
     */
    private void prepareParticipantForm(Model model, boolean isEdit, boolean nameLocked) {
        model.addAttribute("isEdit", isEdit);
        model.addAttribute("nameLocked", nameLocked);
        addParticipantFormOptions(model);
    }

    /**
     * Adds reference data required by the participant create/edit form.
     */
    private void addParticipantFormOptions(Model model) {
        model.addAttribute("countryOptions", CountryOptions.all());
        model.addAttribute("rankOptions", RankOptions.all());
        model.addAttribute("clubOptions", ClubOptions.all());
        model.addAttribute("clubsByCountry", ClubOptions.byCountry());
    }

    /**
     * Adds reference data required by the participant list filters.
     */
    private void addParticipantFilterOptions(Model model) {
        model.addAttribute("countryOptions", CountryOptions.all());
        model.addAttribute("rankOptions", RankOptions.all());
        model.addAttribute("clubsByCountry", ClubOptions.byCountry());
    }
}