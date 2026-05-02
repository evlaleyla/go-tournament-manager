package com.evlaleyla.gotournamentmanager.backend.registration;

import com.evlaleyla.gotournamentmanager.backend.ClubOptions;
import com.evlaleyla.gotournamentmanager.backend.CountryOptions;
import com.evlaleyla.gotournamentmanager.backend.RankOptions;
import com.evlaleyla.gotournamentmanager.backend.participant.ParticipantService;
import com.evlaleyla.gotournamentmanager.backend.tournament.Tournament;
import com.evlaleyla.gotournamentmanager.backend.tournament.TournamentService;
import com.evlaleyla.gotournamentmanager.backend.tournament.TournamentStatus;
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

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

/**
 * Controller responsible for managing tournament registrations
 * in both the administrative and public application areas.
 *
 * <p>This controller handles:
 * <ul>
 *     <li>Administrative creation, editing, listing and deletion of registrations</li>
 *     <li>Public self-registration for tournaments</li>
 *     <li>Validation of selected rounds and registration eligibility</li>
 * </ul>
 * </p>
 */
@Controller
public class RegistrationController {

    private static final Logger LOGGER = LoggerFactory.getLogger(RegistrationController.class);

    private static final String VIEW_REGISTRATION_FORM = "registration-form";
    private static final String VIEW_PUBLIC_REGISTRATION_FORM = "public-registration-form";
    private static final String VIEW_REGISTRATIONS = "registrations";
    private static final String VIEW_REGISTRATION_DETAIL = "registration-detail";

    private static final String ATTR_REGISTRATION_FORM = "registrationForm";
    private static final String ATTR_SELF_REGISTRATION_FORM = "selfRegistrationForm";
    private static final String ATTR_SELECTED_TOURNAMENT = "selectedTournament";
    private static final String ATTR_AVAILABLE_ROUNDS = "availableRounds";
    private static final String ATTR_IS_EDIT = "isEdit";
    private static final String ATTR_REGISTRATION_ID = "registrationId";
    private static final String ATTR_FORM_RELOAD_BASE_URL = "formReloadBaseUrl";
    private static final String ATTR_PARTICIPANT_AND_TOURNAMENT_LOCKED = "participantAndTournamentLocked";

    private final RegistrationService registrationService;
    private final TournamentService tournamentService;
    private final ParticipantService participantService;

    public RegistrationController(RegistrationService registrationService,
                                  TournamentService tournamentService,
                                  ParticipantService participantService) {
        this.registrationService = registrationService;
        this.tournamentService = tournamentService;
        this.participantService = participantService;
    }

    /**
     * Displays all registrations or only the registrations of a selected tournament.
     *
     * @param tournamentId optional tournament filter
     * @param model        Spring MVC model
     * @return registrations overview page
     */
    @GetMapping("/registrations")
    public String showRegistrations(@RequestParam(required = false) Long tournamentId,
                                    Model model) {

        LOGGER.debug("Rendering registration overview. tournamentId={}", tournamentId);

        if (tournamentId != null) {
            model.addAttribute("registrations", registrationService.findByTournamentId(tournamentId));
        } else {
            model.addAttribute("registrations", registrationService.findAll());
        }

        model.addAttribute("tournaments", tournamentService.findAll());
        model.addAttribute("selectedTournamentId", tournamentId);

        return VIEW_REGISTRATIONS;
    }

    /**
     * Displays the administrative registration creation form.
     *
     * @param tournamentId optional preselected tournament id
     * @param model        Spring MVC model
     * @return registration form view
     */
    @GetMapping("/registrations/new")
    public String showRegistrationForm(@RequestParam(required = false) Long tournamentId,
                                       Model model) {
        LOGGER.debug("Rendering registration creation form. tournamentId={}", tournamentId);

        RegistrationForm registrationForm = new RegistrationForm();
        Tournament selectedTournament = null;

        if (tournamentId != null) {
            registrationForm.setTournamentId(tournamentId);
            selectedTournament = tournamentService.findById(tournamentId);
        }

        prepareAdminRegistrationForm(
                model,
                registrationForm,
                selectedTournament,
                false,
                null,
                false,
                "/registrations/new"
        );

        return VIEW_REGISTRATION_FORM;
    }

    /**
     * Creates a new administrative registration.
     *
     * @param registrationForm     bound registration form data
     * @param bindingResult        validation result
     * @param model                Spring MVC model
     * @param redirectAttributes   redirect flash scope
     * @return redirect or registration form view
     */
    @PostMapping("/registrations")
    public String createRegistration(@Valid @ModelAttribute(ATTR_REGISTRATION_FORM) RegistrationForm registrationForm,
                                     BindingResult bindingResult,
                                     Model model,
                                     RedirectAttributes redirectAttributes) {

        LOGGER.debug(
                "Processing registration creation. tournamentId={}, participantId={}",
                registrationForm.getTournamentId(),
                registrationForm.getParticipantId()
        );

        Tournament selectedTournament = resolveSelectedTournament(registrationForm.getTournamentId());

        validateSelectedRounds(
                registrationForm.getTournamentId(),
                registrationForm.getSelectedRounds(),
                "selectedRounds",
                bindingResult
        );

        validateDuplicateRegistrationOnCreate(registrationForm, bindingResult);

        if (bindingResult.hasErrors()) {
            LOGGER.debug(
                    "Registration creation failed due to validation errors. tournamentId={}, participantId={}",
                    registrationForm.getTournamentId(),
                    registrationForm.getParticipantId()
            );

            prepareAdminRegistrationForm(
                    model,
                    registrationForm,
                    selectedTournament,
                    false,
                    null,
                    false,
                    "/registrations/new"
            );

            return VIEW_REGISTRATION_FORM;
        }

        try {
            Registration registration = registrationService.create(registrationForm);

            LOGGER.info(
                    "Registration created successfully. registrationId={}, tournamentId={}, participantId={}",
                    registration.getId(),
                    registration.getTournament().getId(),
                    registration.getParticipant().getId()
            );

            redirectAttributes.addFlashAttribute("successMessage", "Anmeldung wurde erfolgreich gespeichert.");
            return "redirect:/registrations/" + registration.getId();
        } catch (IllegalArgumentException e) {
            LOGGER.warn(
                    "Registration creation rejected by business validation. tournamentId={}, participantId={}, message={}",
                    registrationForm.getTournamentId(),
                    registrationForm.getParticipantId(),
                    e.getMessage()
            );

            bindingResult.reject("registration.invalid", e.getMessage());

            prepareAdminRegistrationForm(
                    model,
                    registrationForm,
                    selectedTournament,
                    false,
                    null,
                    false,
                    "/registrations/new"
            );

            return VIEW_REGISTRATION_FORM;
        }
    }

    /**
     * Displays the public self-registration form for a tournament.
     *
     * @param id    tournament id
     * @param model Spring MVC model
     * @return public registration form view
     */
    @GetMapping("/public/tournaments/{id}/register")
    public String showPublicRegistrationForm(@PathVariable Long id, Model model) {
        LOGGER.debug("Rendering public registration form. tournamentId={}", id);

        Tournament tournament = tournamentService.findById(id);

        model.addAttribute("tournament", tournament);
        model.addAttribute(ATTR_SELF_REGISTRATION_FORM, new SelfRegistrationForm());
        addAvailableRounds(model, tournament);
        addPublicRegistrationFormOptions(model);

        return VIEW_PUBLIC_REGISTRATION_FORM;
    }

    /**
     * Processes a public self-registration submission.
     *
     * @param id                   tournament id
     * @param selfRegistrationForm bound public form data
     * @param bindingResult        validation result
     * @param model                Spring MVC model
     * @param redirectAttributes   redirect flash scope
     * @return redirect or public registration form view
     */
    @PostMapping("/public/tournaments/{id}/register")
    public String submitPublicRegistration(@PathVariable Long id,
                                           @Valid @ModelAttribute(ATTR_SELF_REGISTRATION_FORM) SelfRegistrationForm selfRegistrationForm,
                                           BindingResult bindingResult,
                                           Model model,
                                           RedirectAttributes redirectAttributes) {

        LOGGER.debug("Processing public registration submission. tournamentId={}, email={}", id, selfRegistrationForm.getEmail());

        Tournament tournament = tournamentService.findById(id);

        validatePublicRegistration(tournament, selfRegistrationForm, bindingResult);

        if (bindingResult.hasErrors()) {
            LOGGER.debug("Public registration validation failed. tournamentId={}, email={}", id, selfRegistrationForm.getEmail());

            preparePublicRegistrationForm(model, tournament, selfRegistrationForm);
            return VIEW_PUBLIC_REGISTRATION_FORM;
        }

        try {
            registrationService.createPublicRegistration(id, selfRegistrationForm);

            LOGGER.info("Public registration submitted successfully. tournamentId={}, email={}", id, selfRegistrationForm.getEmail());

            redirectAttributes.addFlashAttribute("successMessage", "Die Anmeldung wurde erfolgreich abgesendet.");
            return "redirect:/public/tournaments/" + id + "/register";
        } catch (IllegalArgumentException e) {
            LOGGER.warn(
                    "Public registration rejected by business validation. tournamentId={}, email={}, message={}",
                    id,
                    selfRegistrationForm.getEmail(),
                    e.getMessage()
            );

            bindingResult.reject("registration.invalid", e.getMessage());
            preparePublicRegistrationForm(model, tournament, selfRegistrationForm);
            return VIEW_PUBLIC_REGISTRATION_FORM;
        }
    }

    /**
     * Displays registration details.
     *
     * @param id    registration id
     * @param model Spring MVC model
     * @return registration detail view
     */
    @GetMapping("/registrations/{id}")
    public String showRegistrationDetail(@PathVariable Long id, Model model) {
        LOGGER.debug("Rendering registration detail page. registrationId={}", id);

        model.addAttribute("registration", registrationService.findById(id));
        return VIEW_REGISTRATION_DETAIL;
    }

    /**
     * Deletes an existing registration.
     *
     * @param id                 registration id
     * @param redirectAttributes redirect flash scope
     * @return redirect target
     */
    @PostMapping("/registrations/{id}/delete")
    public String deleteRegistration(@PathVariable Long id,
                                     RedirectAttributes redirectAttributes) {
        LOGGER.debug("Processing registration deletion. registrationId={}", id);

        try {
            registrationService.deleteById(id);

            LOGGER.info("Registration deleted successfully. registrationId={}", id);

            redirectAttributes.addFlashAttribute("successMessage", "Anmeldung wurde erfolgreich gelöscht.");
            return "redirect:/registrations";
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Registration deletion failed. registrationId={}, message={}", id, e.getMessage());

            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/registrations/" + id;
        }
    }

    /**
     * Displays the administrative edit form for an existing registration.
     *
     * @param id           registration id
     * @param tournamentId optional replacement tournament id used when the form is reloaded
     * @param model        Spring MVC model
     * @return registration form view
     */
    @GetMapping("/registrations/{id}/edit")
    public String showEditRegistrationForm(@PathVariable Long id,
                                           @RequestParam(required = false) Long tournamentId,
                                           Model model) {
        LOGGER.debug("Rendering registration edit form. registrationId={}, tournamentId={}", id, tournamentId);

        Registration registration = registrationService.findById(id);

        RegistrationForm registrationForm = new RegistrationForm();
        registrationForm.setParticipantId(registration.getParticipant().getId());
        registrationForm.setSelectedRounds(new ArrayList<>(registration.getSelectedRounds()));
        registrationForm.setNotes(registration.getNotes());

        Long effectiveTournamentId = tournamentId != null
                ? tournamentId
                : registration.getTournament().getId();

        registrationForm.setTournamentId(effectiveTournamentId);

        Tournament selectedTournament = tournamentService.findById(effectiveTournamentId);
        boolean participantAndTournamentLocked = registrationService.isParticipantOrTournamentLocked(id);

        prepareAdminRegistrationForm(
                model,
                registrationForm,
                selectedTournament,
                true,
                id,
                participantAndTournamentLocked,
                "/registrations/" + id + "/edit"
        );

        return VIEW_REGISTRATION_FORM;
    }

    /**
     * Updates an existing registration.
     *
     * @param id                  registration id
     * @param registrationForm    bound registration form data
     * @param bindingResult       validation result
     * @param model               Spring MVC model
     * @param redirectAttributes  redirect flash scope
     * @return redirect or registration form view
     */
    @PostMapping("/registrations/{id}")
    public String updateRegistration(@PathVariable Long id,
                                     @Valid @ModelAttribute(ATTR_REGISTRATION_FORM) RegistrationForm registrationForm,
                                     BindingResult bindingResult,
                                     Model model,
                                     RedirectAttributes redirectAttributes) {

        LOGGER.debug(
                "Processing registration update. registrationId={}, tournamentId={}, participantId={}",
                id,
                registrationForm.getTournamentId(),
                registrationForm.getParticipantId()
        );

        Tournament selectedTournament = resolveSelectedTournament(registrationForm.getTournamentId());
        boolean participantAndTournamentLocked = registrationService.isParticipantOrTournamentLocked(id);

        validateSelectedRounds(
                registrationForm.getTournamentId(),
                registrationForm.getSelectedRounds(),
                "selectedRounds",
                bindingResult
        );

        validateDuplicateRegistrationOnUpdate(id, registrationForm, bindingResult);

        if (bindingResult.hasErrors()) {
            LOGGER.debug("Registration update failed due to validation errors. registrationId={}", id);

            prepareAdminRegistrationForm(
                    model,
                    registrationForm,
                    selectedTournament,
                    true,
                    id,
                    participantAndTournamentLocked,
                    "/registrations/" + id + "/edit"
            );

            return VIEW_REGISTRATION_FORM;
        }

        try {
            Registration updatedRegistration = registrationService.update(id, registrationForm);

            LOGGER.info(
                    "Registration updated successfully. registrationId={}, tournamentId={}, participantId={}",
                    updatedRegistration.getId(),
                    updatedRegistration.getTournament().getId(),
                    updatedRegistration.getParticipant().getId()
            );

            redirectAttributes.addFlashAttribute("successMessage", "Anmeldung wurde erfolgreich aktualisiert.");
            return "redirect:/registrations/" + updatedRegistration.getId();
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Registration update rejected by business validation. registrationId={}, message={}", id, e.getMessage());

            bindingResult.reject("registration.invalid", e.getMessage());

            prepareAdminRegistrationForm(
                    model,
                    registrationForm,
                    selectedTournament,
                    true,
                    id,
                    participantAndTournamentLocked,
                    "/registrations/" + id + "/edit"
            );

            return VIEW_REGISTRATION_FORM;
        }
    }

    /**
     * Populates the model for the administrative registration form.
     *
     * <p>This helper ensures that the same model structure is used consistently
     * across create, update and validation error scenarios.</p>
     */
    private void prepareAdminRegistrationForm(Model model,
                                              RegistrationForm registrationForm,
                                              Tournament selectedTournament,
                                              boolean isEdit,
                                              Long registrationId,
                                              boolean participantAndTournamentLocked,
                                              String formReloadBaseUrl) {

        model.addAttribute(ATTR_REGISTRATION_FORM, registrationForm);
        model.addAttribute(ATTR_SELECTED_TOURNAMENT, selectedTournament);
        model.addAttribute(ATTR_IS_EDIT, isEdit);
        model.addAttribute(ATTR_REGISTRATION_ID, registrationId);
        model.addAttribute(ATTR_FORM_RELOAD_BASE_URL, formReloadBaseUrl);
        model.addAttribute(ATTR_PARTICIPANT_AND_TOURNAMENT_LOCKED, participantAndTournamentLocked);

        addFormData(model);
        addAvailableRounds(model, selectedTournament);
    }

    /**
     * Populates the model for the public registration form.
     */
    private void preparePublicRegistrationForm(Model model,
                                               Tournament tournament,
                                               SelfRegistrationForm selfRegistrationForm) {
        model.addAttribute("tournament", tournament);
        model.addAttribute(ATTR_SELF_REGISTRATION_FORM, selfRegistrationForm);
        addAvailableRounds(model, tournament);
        addPublicRegistrationFormOptions(model);
    }

    /**
     * Adds tournament and participant selection data used by the administrative form.
     */
    private void addFormData(Model model) {
        model.addAttribute("tournaments", tournamentService.findAll());
        model.addAttribute("participants", participantService.findAll());
    }

    /**
     * Adds the available rounds of the selected tournament to the model.
     */
    private void addAvailableRounds(Model model, Tournament tournament) {
        if (tournament != null && tournament.getNumberOfRounds() != null) {
            model.addAttribute(
                    ATTR_AVAILABLE_ROUNDS,
                    IntStream.rangeClosed(1, tournament.getNumberOfRounds()).boxed().toList()
            );
        }
    }

    /**
     * Loads the selected tournament if a tournament id is present.
     *
     * @param tournamentId optional tournament id
     * @return tournament entity or {@code null}
     */
    private Tournament resolveSelectedTournament(Long tournamentId) {
        if (tournamentId == null) {
            return null;
        }

        return tournamentService.findById(tournamentId);
    }

    /**
     * Validates whether a registration is still allowed with respect to the registration deadline.
     */
    private void validateRegistrationDate(RegistrationForm registrationForm, BindingResult bindingResult) {
        if (registrationForm.getTournamentId() == null) {
            return;
        }

        Tournament tournament = tournamentService.findById(registrationForm.getTournamentId());
        LocalDate registrationDate = LocalDate.now();

        if (tournament.getRegistrationDeadline() != null
                && registrationDate.isAfter(tournament.getRegistrationDeadline())) {
            bindingResult.reject(
                    "registration.registrationDate.afterDeadline",
                    "Eine Anmeldung nach Ablauf der Anmeldefrist ist nicht mehr möglich."
            );
        }
    }

    /**
     * Validates public registration constraints such as tournament status,
     * registration deadline, duplicate registration and selected rounds.
     */
    private void validatePublicRegistration(Tournament tournament,
                                            SelfRegistrationForm form,
                                            BindingResult bindingResult) {

        if (tournament.getStatus() != TournamentStatus.REGISTRATION_OPEN) {
            bindingResult.reject(
                    "registration.status.closed",
                    "Für dieses Turnier ist die Online-Anmeldung derzeit nicht geöffnet."
            );
        }

        if (tournament.getRegistrationDeadline() != null
                && LocalDate.now().isAfter(tournament.getRegistrationDeadline())) {
            bindingResult.reject(
                    "registration.deadline.expired",
                    "Die Anmeldefrist für dieses Turnier ist bereits abgelaufen."
            );
        }

        if (registrationService.existsByTournamentIdAndParticipantEmail(tournament.getId(), form.getEmail())) {
            bindingResult.rejectValue(
                    "email",
                    "registration.duplicate.email",
                    "Mit dieser E-Mail-Adresse besteht bereits eine Anmeldung für dieses Turnier."
            );
        }

        validateSelectedRounds(
                tournament.getId(),
                form.getSelectedRounds(),
                "selectedRounds",
                bindingResult
        );
    }

    /**
     * Validates the selected rounds of a registration.
     *
     * <p>The validation checks:
     * <ul>
     *     <li>at least one selected round</li>
     *     <li>a valid tournament configuration</li>
     *     <li>no duplicate round selections</li>
     *     <li>only existing round numbers</li>
     * </ul>
     * </p>
     */
    private void validateSelectedRounds(Long tournamentId,
                                        List<Integer> selectedRounds,
                                        String fieldName,
                                        BindingResult bindingResult) {

        if (tournamentId == null) {
            return;
        }

        Tournament tournament = tournamentService.findById(tournamentId);

        if (selectedRounds == null || selectedRounds.isEmpty()) {
            bindingResult.rejectValue(
                    fieldName,
                    "registration.selectedRounds.empty",
                    "Bitte mindestens eine Runde auswählen."
            );
            return;
        }

        if (tournament.getNumberOfRounds() == null || tournament.getNumberOfRounds() < 1) {
            bindingResult.rejectValue(
                    fieldName,
                    "registration.selectedRounds.invalidTournament",
                    "Für dieses Turnier sind keine gültigen Runden hinterlegt."
            );
            return;
        }

        Set<Integer> uniqueRounds = new LinkedHashSet<>(selectedRounds);

        if (uniqueRounds.size() != selectedRounds.size()) {
            bindingResult.rejectValue(
                    fieldName,
                    "registration.selectedRounds.duplicate",
                    "Jede Runde darf nur einmal ausgewählt werden."
            );
            return;
        }

        for (Integer round : uniqueRounds) {
            if (round == null) {
                bindingResult.rejectValue(
                        fieldName,
                        "registration.selectedRounds.null",
                        "Die ausgewählte Runde ist ungültig."
                );
                return;
            }

            if (round < 1 || round > tournament.getNumberOfRounds()) {
                bindingResult.rejectValue(
                        fieldName,
                        "registration.selectedRounds.outOfRange",
                        "Es dürfen nur vorhandene Turnierrunden ausgewählt werden."
                );
                return;
            }
        }
    }

    /**
     * Validates duplicate registrations during creation.
     */
    private void validateDuplicateRegistrationOnCreate(RegistrationForm registrationForm,
                                                       BindingResult bindingResult) {
        if (registrationForm.getTournamentId() != null
                && registrationForm.getParticipantId() != null
                && registrationService.existsByTournamentIdAndParticipantId(
                registrationForm.getTournamentId(),
                registrationForm.getParticipantId())) {

            bindingResult.rejectValue(
                    "participantId",
                    "registration.duplicate",
                    "Dieser Teilnehmer ist bereits für das ausgewählte Turnier angemeldet."
            );
        }
    }

    /**
     * Validates duplicate registrations during update.
     */
    private void validateDuplicateRegistrationOnUpdate(Long registrationId,
                                                       RegistrationForm registrationForm,
                                                       BindingResult bindingResult) {
        if (registrationForm.getTournamentId() != null
                && registrationForm.getParticipantId() != null
                && registrationService.existsByTournamentIdAndParticipantIdAndIdNot(
                registrationForm.getTournamentId(),
                registrationForm.getParticipantId(),
                registrationId)) {

            bindingResult.rejectValue(
                    "participantId",
                    "registration.duplicate",
                    "Dieser Teilnehmer ist bereits für das ausgewählte Turnier angemeldet."
            );
        }
    }

    /**
     * Adds static option data required by the public self-registration form.
     */
    private void addPublicRegistrationFormOptions(Model model) {
        model.addAttribute("countryOptions", CountryOptions.all());
        model.addAttribute("rankOptions", RankOptions.all());
        model.addAttribute("clubOptions", ClubOptions.all());
        model.addAttribute("clubsByCountry", ClubOptions.byCountry());
    }
}