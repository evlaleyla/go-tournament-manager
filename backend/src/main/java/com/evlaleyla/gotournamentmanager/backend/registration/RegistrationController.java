package com.evlaleyla.gotournamentmanager.backend.registration;

import com.evlaleyla.gotournamentmanager.backend.ClubOptions;
import com.evlaleyla.gotournamentmanager.backend.CountryOptions;
import com.evlaleyla.gotournamentmanager.backend.RankOptions;
import com.evlaleyla.gotournamentmanager.backend.participant.ParticipantService;
import com.evlaleyla.gotournamentmanager.backend.tournament.Tournament;
import com.evlaleyla.gotournamentmanager.backend.tournament.TournamentService;
import com.evlaleyla.gotournamentmanager.backend.tournament.TournamentStatus;
import jakarta.validation.Valid;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

@Controller
public class RegistrationController {

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

    @GetMapping("/registrations")
    public String showRegistrations(@RequestParam(required = false) Long tournamentId,
                                    Model model) {

        if (tournamentId != null) {
            model.addAttribute("registrations", registrationService.findByTournamentId(tournamentId));
        } else {
            model.addAttribute("registrations", registrationService.findAll());
        }

        model.addAttribute("tournaments", tournamentService.findAll());
        model.addAttribute("selectedTournamentId", tournamentId);

        return "registrations";
    }

    @GetMapping("/registrations/new")
    public String showRegistrationForm(@RequestParam(required = false) Long tournamentId,
                                       Model model) {
        RegistrationForm registrationForm = new RegistrationForm();
        Tournament selectedTournament = null;

        if (tournamentId != null) {
            registrationForm.setTournamentId(tournamentId);
            selectedTournament = tournamentService.findById(tournamentId);
        }

        model.addAttribute("registrationForm", registrationForm);
        model.addAttribute("selectedTournament", selectedTournament);
        model.addAttribute("isEdit", false);
        model.addAttribute("registrationId", null);
        model.addAttribute("formReloadBaseUrl", "/registrations/new");

        if (selectedTournament != null && selectedTournament.getNumberOfRounds() != null) {
            model.addAttribute("availableRounds",
                    IntStream.rangeClosed(1, selectedTournament.getNumberOfRounds()).boxed().toList());
        }

        addFormData(model);
        return "registration-form";
    }

    @PostMapping("/registrations")
    public String createRegistration(@Valid @ModelAttribute("registrationForm") RegistrationForm registrationForm,
                                     BindingResult bindingResult,
                                     Model model,
                                     RedirectAttributes redirectAttributes) {

        Tournament selectedTournament = null;

        if (registrationForm.getTournamentId() != null) {
            selectedTournament = tournamentService.findById(registrationForm.getTournamentId());
        }

        validateRegistrationDate(registrationForm, bindingResult);
        validateSelectedRounds(
                registrationForm.getTournamentId(),
                registrationForm.getSelectedRounds(),
                "selectedRounds",
                bindingResult
        );

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

        if (bindingResult.hasErrors()) {
            addFormData(model);
            model.addAttribute("selectedTournament", selectedTournament);
            model.addAttribute("isEdit", false);
            model.addAttribute("registrationId", null);
            model.addAttribute("formReloadBaseUrl", "/registrations/new");

            if (selectedTournament != null && selectedTournament.getNumberOfRounds() != null) {
                model.addAttribute("availableRounds",
                        IntStream.rangeClosed(1, selectedTournament.getNumberOfRounds()).boxed().toList());
            }

            return "registration-form";
        }

        Registration registration = registrationService.create(registrationForm);
        redirectAttributes.addFlashAttribute("successMessage", "Anmeldung wurde erfolgreich gespeichert.");
        return "redirect:/registrations/" + registration.getId();
    }

    @GetMapping("/public/tournaments/{id}/register")
    public String showPublicRegistrationForm(@PathVariable Long id, Model model) {
        Tournament tournament = tournamentService.findById(id);

        model.addAttribute("tournament", tournament);
        model.addAttribute("selfRegistrationForm", new SelfRegistrationForm());

        if (tournament.getNumberOfRounds() != null) {
            model.addAttribute("availableRounds",
                    IntStream.rangeClosed(1, tournament.getNumberOfRounds()).boxed().toList());
        }

        addPublicRegistrationFormOptions(model);
        return "public-registration-form";
    }

    @PostMapping("/public/tournaments/{id}/register")
    public String submitPublicRegistration(@PathVariable Long id,
                                           @Valid @ModelAttribute("selfRegistrationForm") SelfRegistrationForm selfRegistrationForm,
                                           BindingResult bindingResult,
                                           Model model,
                                           RedirectAttributes redirectAttributes) {

        Tournament tournament = tournamentService.findById(id);

        validatePublicRegistration(tournament, selfRegistrationForm, bindingResult);

        if (bindingResult.hasErrors()) {
            model.addAttribute("tournament", tournament);

            if (tournament.getNumberOfRounds() != null) {
                model.addAttribute("availableRounds",
                        IntStream.rangeClosed(1, tournament.getNumberOfRounds()).boxed().toList());
            }

            addPublicRegistrationFormOptions(model);
            return "public-registration-form";
        }

        try {
            registrationService.createPublicRegistration(id, selfRegistrationForm);
        } catch (IllegalArgumentException e) {
            bindingResult.reject("registration.invalid", e.getMessage());
            model.addAttribute("tournament", tournament);

            if (tournament.getNumberOfRounds() != null) {
                model.addAttribute("availableRounds",
                        IntStream.rangeClosed(1, tournament.getNumberOfRounds()).boxed().toList());
            }

            addPublicRegistrationFormOptions(model);
            return "public-registration-form";
        }

        redirectAttributes.addFlashAttribute("successMessage", "Die Anmeldung wurde erfolgreich abgesendet.");
        return "redirect:/public/tournaments/" + id + "/register";
    }

    @GetMapping("/registrations/{id}")
    public String showRegistrationDetail(@PathVariable Long id, Model model) {
        model.addAttribute("registration", registrationService.findById(id));
        return "registration-detail";
    }

    @PostMapping("/registrations/{id}/delete")
    public String deleteRegistration(@PathVariable Long id,
                                     RedirectAttributes redirectAttributes) {
        registrationService.deleteById(id);
        redirectAttributes.addFlashAttribute("successMessage", "Anmeldung wurde erfolgreich gelöscht.");
        return "redirect:/registrations";
    }

    private void addFormData(Model model) {
        model.addAttribute("tournaments", tournamentService.findAll());
        model.addAttribute("participants", participantService.findAll());
    }

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

    private void addPublicRegistrationFormOptions(Model model) {
        model.addAttribute("countryOptions", CountryOptions.all());
        model.addAttribute("rankOptions", RankOptions.all());
        model.addAttribute("clubOptions", ClubOptions.all());
        model.addAttribute("clubsByCountry", ClubOptions.byCountry());
    }

    @GetMapping("/registrations/{id}/edit")
    public String showEditRegistrationForm(@PathVariable Long id,
                                           @RequestParam(required = false) Long tournamentId,
                                           Model model) {
        Registration registration = registrationService.findById(id);

        RegistrationForm registrationForm = new RegistrationForm();
        registrationForm.setParticipantId(registration.getParticipant().getId());
        registrationForm.setSelectedRounds(new java.util.ArrayList<>(registration.getSelectedRounds()));
        registrationForm.setNotes(registration.getNotes());

        Long effectiveTournamentId = tournamentId != null
                ? tournamentId
                : registration.getTournament().getId();

        registrationForm.setTournamentId(effectiveTournamentId);

        Tournament selectedTournament = tournamentService.findById(effectiveTournamentId);

        model.addAttribute("registrationForm", registrationForm);
        model.addAttribute("selectedTournament", selectedTournament);
        model.addAttribute("isEdit", true);
        model.addAttribute("registrationId", id);
        model.addAttribute("formReloadBaseUrl", "/registrations/" + id + "/edit");

        if (selectedTournament.getNumberOfRounds() != null) {
            model.addAttribute("availableRounds",
                    IntStream.rangeClosed(1, selectedTournament.getNumberOfRounds()).boxed().toList());
        }

        addFormData(model);
        return "registration-form";
    }

    @PostMapping("/registrations/{id}")
    public String updateRegistration(@PathVariable Long id,
                                     @Valid @ModelAttribute("registrationForm") RegistrationForm registrationForm,
                                     BindingResult bindingResult,
                                     Model model,
                                     RedirectAttributes redirectAttributes) {

        Tournament selectedTournament = null;

        if (registrationForm.getTournamentId() != null) {
            selectedTournament = tournamentService.findById(registrationForm.getTournamentId());
        }

        validateSelectedRounds(
                registrationForm.getTournamentId(),
                registrationForm.getSelectedRounds(),
                "selectedRounds",
                bindingResult
        );

        if (registrationForm.getTournamentId() != null
                && registrationForm.getParticipantId() != null
                && registrationService.existsByTournamentIdAndParticipantIdAndIdNot(
                registrationForm.getTournamentId(),
                registrationForm.getParticipantId(),
                id)) {

            bindingResult.rejectValue(
                    "participantId",
                    "registration.duplicate",
                    "Dieser Teilnehmer ist bereits für das ausgewählte Turnier angemeldet."
            );
        }

        if (bindingResult.hasErrors()) {
            addFormData(model);
            model.addAttribute("selectedTournament", selectedTournament);
            model.addAttribute("isEdit", true);
            model.addAttribute("registrationId", id);
            model.addAttribute("formReloadBaseUrl", "/registrations/" + id + "/edit");

            if (selectedTournament != null && selectedTournament.getNumberOfRounds() != null) {
                model.addAttribute("availableRounds",
                        IntStream.rangeClosed(1, selectedTournament.getNumberOfRounds()).boxed().toList());
            }

            return "registration-form";
        }

        Registration updatedRegistration = registrationService.update(id, registrationForm);
        redirectAttributes.addFlashAttribute("successMessage", "Anmeldung wurde erfolgreich aktualisiert.");
        return "redirect:/registrations/" + updatedRegistration.getId();
    }
}