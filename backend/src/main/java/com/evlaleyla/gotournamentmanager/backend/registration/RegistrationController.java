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

        if (tournamentId != null) {
            registrationForm.setTournamentId(tournamentId);
        }

        model.addAttribute("registrationForm", registrationForm);
        addFormData(model);
        return "registration-form";
    }

    @PostMapping("/registrations")
    public String createRegistration(@Valid @ModelAttribute("registrationForm") RegistrationForm registrationForm,
                                     BindingResult bindingResult,
                                     Model model,
                                     RedirectAttributes redirectAttributes) {

        validateRegistrationDate(registrationForm, bindingResult);
        validatePlannedRounds(registrationForm.getTournamentId(), registrationForm.getPlannedRounds(), bindingResult);


        if (registrationService.existsByTournamentIdAndParticipantId(
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
            return "registration-form";
        }

        Registration registration = registrationService.create(registrationForm);
        redirectAttributes.addFlashAttribute("successMessage", "Anmeldung wurde erfolgreich gespeichert.");
        return "redirect:/registrations/" + registration.getId();
    }

    private void validatePlannedRounds(Long tournamentId, Integer plannedRounds, BindingResult bindingResult) {
        if (tournamentId == null || plannedRounds == null) {
            return;
        }

        Tournament tournament = tournamentService.findById(tournamentId);

        if (tournament.getNumberOfRounds() != null && plannedRounds > tournament.getNumberOfRounds()) {
            bindingResult.rejectValue(
                    "plannedRounds",
                    "registration.plannedRounds.tooHigh",
                    "Die geplante Rundenzahl darf nicht größer als die Anzahl der Turnierrunden sein."
            );
        }
    }

    @GetMapping("/tournaments/{id}/register")
    public String showPublicRegistrationForm(@PathVariable Long id, Model model) {
        Tournament tournament = tournamentService.findById(id);

        model.addAttribute("tournament", tournament);
        model.addAttribute("selfRegistrationForm", new SelfRegistrationForm());
        addPublicRegistrationFormOptions(model);
        return "public-registration-form";
    }

    @PostMapping("/tournaments/{id}/register")
    public String submitPublicRegistration(@PathVariable Long id,
                                           @Valid @ModelAttribute("selfRegistrationForm") SelfRegistrationForm selfRegistrationForm,
                                           BindingResult bindingResult,
                                           Model model,
                                           RedirectAttributes redirectAttributes) {

        Tournament tournament = tournamentService.findById(id);

        validatePublicRegistration(tournament, selfRegistrationForm, bindingResult);

        if (bindingResult.hasErrors()) {
            model.addAttribute("tournament", tournament);
            addPublicRegistrationFormOptions(model);
            return "public-registration-form";
        }

        try {
            registrationService.createPublicRegistration(id, selfRegistrationForm);
        } catch (IllegalArgumentException e) {
            bindingResult.reject("registration.invalid", e.getMessage());
            model.addAttribute("tournament", tournament);
            addPublicRegistrationFormOptions(model);
            return "public-registration-form";
        }

        redirectAttributes.addFlashAttribute("successMessage", "Die Anmeldung wurde erfolgreich abgesendet.");
        return "redirect:/tournaments/" + id + "/register";
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
        model.addAttribute("countryOptions", CountryOptions.all());
        model.addAttribute("rankOptions", RankOptions.all());
        model.addAttribute("clubOptions", ClubOptions.all());
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

        if (form.getPlannedRounds() != null
                && tournament.getNumberOfRounds() != null
                && form.getPlannedRounds() > tournament.getNumberOfRounds()) {
            bindingResult.rejectValue(
                    "plannedRounds",
                    "registration.plannedRounds.tooHigh",
                    "Die geplante Rundenzahl darf nicht größer als die Anzahl der Turnierrunden sein."
            );
        }

    }

    private void addPublicRegistrationFormOptions(Model model) {
        model.addAttribute("countryOptions", CountryOptions.all());
        model.addAttribute("rankOptions", RankOptions.all());
        model.addAttribute("clubOptions", ClubOptions.all());
        model.addAttribute("clubsByCountry", ClubOptions.byCountry());
    }
}