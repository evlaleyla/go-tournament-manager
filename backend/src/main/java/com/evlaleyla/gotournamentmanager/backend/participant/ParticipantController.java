package com.evlaleyla.gotournamentmanager.backend.participant;

import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ParticipantController {

    private final ParticipantService participantService;

    public ParticipantController(ParticipantService participantService) {
        this.participantService = participantService;
    }

    @GetMapping("/participants")
    public String showParticipants(Model model) {
        model.addAttribute("participants", participantService.findAll());
        return "participants";
    }

    @GetMapping("/participants/new")
    public String showParticipantForm(Model model) {
        model.addAttribute("participant", new Participant());
        model.addAttribute("isEdit", false);
        return "participant-form";
    }

    @PostMapping("/participants")
    public String createParticipant(@Valid @ModelAttribute Participant participant,
                                    BindingResult bindingResult,
                                    Model model,
                                    RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("isEdit", false);
            return "participant-form";
        }

        try {
            participantService.save(participant);
        } catch (IllegalArgumentException e) {
            bindingResult.rejectValue("email", "participant.email.duplicate", e.getMessage());
            model.addAttribute("isEdit", false);
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
            return "participant-form";
        }

        try {
            participantService.update(id, participant);
        } catch (IllegalArgumentException e) {
            bindingResult.rejectValue("email", "participant.email.duplicate", e.getMessage());
            model.addAttribute("isEdit", true);
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
}