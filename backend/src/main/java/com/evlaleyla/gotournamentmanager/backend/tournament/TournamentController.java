package com.evlaleyla.gotournamentmanager.backend.tournament;

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
public class TournamentController {

    private final TournamentService tournamentService;

    public TournamentController(TournamentService tournamentService) {
        this.tournamentService = tournamentService;
    }

    @GetMapping("/tournaments")
    public String showTournaments(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) TournamentStatus status,
            Model model) {

        model.addAttribute("tournaments", tournamentService.search(search, status));
        model.addAttribute("search", search);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("statuses", TournamentStatus.values());
        model.addAttribute("tournamentNames", tournamentService.findDistinctNames());

        return "tournaments";
    }

    @GetMapping("/tournaments/new")
    public String showTournamentForm(Model model) {
        model.addAttribute("tournament", new Tournament());
        model.addAttribute("isEdit", false);
        model.addAttribute("statuses", TournamentStatus.values());
        return "tournament-form";
    }

    @PostMapping("/tournaments")
    public String createTournament(@Valid @ModelAttribute Tournament tournament,
                                   BindingResult bindingResult,
                                   Model model,
                                   RedirectAttributes redirectAttributes) {

        validateTournamentDates(tournament, bindingResult);

        if (bindingResult.hasErrors()) {
            model.addAttribute("isEdit", false);
            model.addAttribute("statuses", TournamentStatus.values());
            return "tournament-form";
        }

        tournamentService.save(tournament);
        redirectAttributes.addFlashAttribute("successMessage", "Turnier wurde erfolgreich angelegt.");
        return "redirect:/tournaments";
    }

    @GetMapping("/tournaments/{id}")
    public String showTournamentDetail(@PathVariable Long id, Model model) {
        model.addAttribute("tournament", tournamentService.findById(id));
        return "tournament-detail";
    }

    @GetMapping("/tournaments/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model) {
        model.addAttribute("tournament", tournamentService.findById(id));
        model.addAttribute("isEdit", true);
        model.addAttribute("statuses", TournamentStatus.values());
        return "tournament-form";
    }

    @PostMapping("/tournaments/{id}")
    public String updateTournament(@PathVariable Long id,
                                   @Valid @ModelAttribute Tournament tournament,
                                   BindingResult bindingResult,
                                   Model model,
                                   RedirectAttributes redirectAttributes) {

        tournament.setId(id);

        validateTournamentDates(tournament, bindingResult);

        if (bindingResult.hasErrors()) {
            model.addAttribute("isEdit", true);
            model.addAttribute("statuses", TournamentStatus.values());
            return "tournament-form";
        }

        tournamentService.update(id, tournament);
        redirectAttributes.addFlashAttribute("successMessage", "Turnier wurde erfolgreich aktualisiert.");
        return "redirect:/tournaments/" + id;
    }

    @PostMapping("/tournaments/{id}/delete")
    public String deleteTournament(@PathVariable Long id,
                                   RedirectAttributes redirectAttributes) {
        tournamentService.deleteById(id);
        redirectAttributes.addFlashAttribute("successMessage", "Turnier wurde erfolgreich gelöscht.");
        return "redirect:/tournaments";
    }

    private void validateTournamentDates(Tournament tournament, BindingResult bindingResult) {
        if (tournament.getStartDate() != null
                && tournament.getEndDate() != null
                && tournament.getEndDate().isBefore(tournament.getStartDate())) {
            bindingResult.rejectValue(
                    "endDate",
                    "tournament.endDate.beforeStartDate",
                    "Das Enddatum darf nicht vor dem Startdatum liegen."
            );
        }

        if (tournament.getRegistrationDeadline() != null
                && tournament.getStartDate() != null
                && tournament.getRegistrationDeadline().isAfter(tournament.getStartDate())) {
            bindingResult.rejectValue(
                    "registrationDeadline",
                    "tournament.registrationDeadline.afterStartDate",
                    "Die Anmeldefrist darf nicht nach dem Startdatum liegen."
            );
        }
    }
}