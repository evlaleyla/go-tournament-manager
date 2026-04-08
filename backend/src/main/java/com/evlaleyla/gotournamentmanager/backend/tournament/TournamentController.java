package com.evlaleyla.gotournamentmanager.backend.tournament;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class TournamentController {

    private final TournamentService tournamentService;

    public TournamentController(TournamentService tournamentService) {
        this.tournamentService = tournamentService;
    }

    @GetMapping("/tournaments")
    public String showTournaments(Model model) {
        model.addAttribute("tournaments", tournamentService.findAll());
        return "tournaments";
    }

    @GetMapping("/tournaments/new")
    public String showTournamentForm(Model model) {
        model.addAttribute("tournament", new Tournament());
        model.addAttribute("isEdit", false);
        return "tournament-form";
    }

    @PostMapping("/tournaments")
    public String createTournament(@ModelAttribute Tournament tournament) {
        tournamentService.save(tournament);
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
        return "tournament-form";
    }

    @PostMapping("/tournaments/{id}")
    public String updateTournament(@PathVariable Long id, @ModelAttribute Tournament tournament) {
        tournamentService.update(id, tournament);
        return "redirect:/tournaments/" + id;
    }

    @PostMapping("/tournaments/{id}/delete")
    public String deleteTournament(@PathVariable Long id) {
        tournamentService.deleteById(id);
        return "redirect:/tournaments";
    }
}