package com.evlaleyla.gotournamentmanager.backend.participant;

import com.evlaleyla.gotournamentmanager.backend.ClubOptions;
import com.evlaleyla.gotournamentmanager.backend.CountryOptions;
import com.evlaleyla.gotournamentmanager.backend.RankOptions;
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

@Controller
public class ParticipantController {

    private final ParticipantService participantService;

    public ParticipantController(ParticipantService participantService) {
        this.participantService = participantService;
    }

    @GetMapping("/participants")
    public String showParticipants(@RequestParam(required = false) String firstName,
                                   @RequestParam(required = false) String lastName,
                                   @RequestParam(required = false) String country,
                                   @RequestParam(required = false) String club,
                                   @RequestParam(required = false) String rank,
                                   Model model) {

        model.addAttribute("participants", participantService.search(firstName, lastName, country, club, rank));
        model.addAttribute("firstName", firstName);
        model.addAttribute("lastName", lastName);
        model.addAttribute("selectedCountry", country);
        model.addAttribute("selectedClub", club);
        model.addAttribute("selectedRank", rank);
        model.addAttribute("countryOptions", CountryOptions.all());
        model.addAttribute("rankOptions", RankOptions.all());
        model.addAttribute("clubsByCountry", ClubOptions.byCountry());

        return "participants";
    }

    @GetMapping("/participants/new")
    public String showParticipantForm(Model model) {
        model.addAttribute("participant", new Participant());
        model.addAttribute("isEdit", false);
        model.addAttribute("nameLocked", false);
        addParticipantFormOptions(model);
        return "participant-form";
    }

    @PostMapping("/participants")
    public String createParticipant(@Valid @ModelAttribute Participant participant,
                                    BindingResult bindingResult,
                                    Model model,
                                    RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("isEdit", false);
            model.addAttribute("nameLocked", false);
            addParticipantFormOptions(model);
            return "participant-form";
        }

        try {
            participantService.save(participant);
        } catch (IllegalArgumentException e) {
            bindingResult.reject("participant.invalid", e.getMessage());
            model.addAttribute("isEdit", false);
            model.addAttribute("nameLocked", false);
            addParticipantFormOptions(model);
            return "participant-form";
        }

        redirectAttributes.addFlashAttribute("successMessage", "Teilnehmer wurde erfolgreich angelegt.");
        return "redirect:/participants";
    }

    @GetMapping("/participants/{id}")
    public String showParticipantDetail(@PathVariable Long id, Model model) {
        model.addAttribute("participant", participantService.findById(id));
        return "participant-detail";
    }

    @GetMapping("/participants/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model) {
        model.addAttribute("participant", participantService.findById(id));
        model.addAttribute("isEdit", true);
        model.addAttribute("nameLocked", participantService.isNameLocked(id));
        addParticipantFormOptions(model);
        return "participant-form";
    }

    @PostMapping("/participants/{id}")
    public String updateParticipant(@PathVariable Long id,
                                    @Valid @ModelAttribute Participant participant,
                                    BindingResult bindingResult,
                                    Model model,
                                    RedirectAttributes redirectAttributes) {

        participant.setId(id);

        if (bindingResult.hasErrors()) {
            model.addAttribute("isEdit", true);
            model.addAttribute("nameLocked", participantService.isNameLocked(id));
            addParticipantFormOptions(model);
            return "participant-form";
        }

        try {
            participantService.update(id, participant);
        } catch (IllegalArgumentException e) {
            bindingResult.reject("participant.invalid", e.getMessage());
            model.addAttribute("isEdit", true);
            model.addAttribute("nameLocked", participantService.isNameLocked(id));
            addParticipantFormOptions(model);
            return "participant-form";
        }

        redirectAttributes.addFlashAttribute("successMessage", "Teilnehmer wurde erfolgreich aktualisiert.");
        return "redirect:/participants/" + id;
    }

    @PostMapping("/participants/{id}/delete")
    public String deleteParticipant(@PathVariable Long id,
                                    RedirectAttributes redirectAttributes) {
        try {
            participantService.deleteById(id);
            redirectAttributes.addFlashAttribute("successMessage", "Teilnehmer wurde erfolgreich gelöscht.");
            return "redirect:/participants";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/participants/" + id;
        }
    }

    private void addParticipantFormOptions(Model model) {
        model.addAttribute("countryOptions", CountryOptions.all());
        model.addAttribute("rankOptions", RankOptions.all());
        model.addAttribute("clubOptions", ClubOptions.all());
        model.addAttribute("clubsByCountry", ClubOptions.byCountry());
    }
}