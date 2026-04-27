package com.evlaleyla.gotournamentmanager.backend.pairing;

import com.evlaleyla.gotournamentmanager.backend.tournament.Tournament;
import com.evlaleyla.gotournamentmanager.backend.tournament.TournamentService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

@Controller
public class PairingController {

    private final PairingService pairingService;
    private final TournamentService tournamentService;

    public PairingController(PairingService pairingService,
                             TournamentService tournamentService) {
        this.pairingService = pairingService;
        this.tournamentService = tournamentService;
    }

    @GetMapping("/tournaments/{id}/pairings")
    public String showTournamentPairings(@PathVariable Long id, Model model) {
        Tournament tournament = tournamentService.findById(id);

        Map<Integer, List<Pairing>> pairingsByRound = new LinkedHashMap<>();

        for (int round = 1; round <= tournament.getNumberOfRounds(); round++) {
            pairingsByRound.put(round, pairingService.findByTournamentIdAndRound(id, round));
        }

        Set<Integer> publishedRounds = pairingService.findPublishedRoundNumbers(id);

        model.addAttribute("tournament", tournament);
        model.addAttribute("pairingsByRound", pairingsByRound);
        model.addAttribute("publishedRounds", publishedRounds);
        model.addAttribute("availableRounds",
                IntStream.rangeClosed(1, tournament.getNumberOfRounds()).boxed().toList());

        return "tournament-pairings";
    }

    @PostMapping("/tournaments/{tournamentId}/pairings/{roundNumber}/publish")
    public String publishRound(@PathVariable Long tournamentId,
                               @PathVariable Integer roundNumber,
                               RedirectAttributes redirectAttributes) {
        try {
            pairingService.publishRound(tournamentId, roundNumber);
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Die Paarungen für Runde " + roundNumber + " wurden veröffentlicht."
            );
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/tournaments/" + tournamentId + "/pairings#round-" + roundNumber;
    }

    @PostMapping("/tournaments/{tournamentId}/pairings/{roundNumber}/unpublish")
    public String unpublishRound(@PathVariable Long tournamentId,
                                 @PathVariable Integer roundNumber,
                                 RedirectAttributes redirectAttributes) {
        try {
            pairingService.unpublishRound(tournamentId, roundNumber);
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Die Veröffentlichung für Runde " + roundNumber + " wurde zurückgenommen."
            );
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/tournaments/" + tournamentId + "/pairings#round-" + roundNumber;
    }

    @PostMapping("/pairings/{id}/result")
    public String saveResult(@PathVariable Long id,
                             @ModelAttribute PairingResultForm pairingResultForm,
                             RedirectAttributes redirectAttributes) {

        Pairing pairing = pairingService.findById(id);

        try {
            String resultCode = buildResultCode(pairingResultForm);
            Pairing updatedPairing = pairingService.updateResult(id, resultCode);

            redirectAttributes.addFlashAttribute("successMessage", "Ergebnis wurde erfolgreich gespeichert.");
            return "redirect:/tournaments/" + updatedPairing.getTournament().getId()
                    + "/pairings#round-" + updatedPairing.getRoundNumber();

        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/tournaments/" + pairing.getTournament().getId()
                    + "/pairings#round-" + pairing.getRoundNumber();
        }
    }

    private String buildResultCode(PairingResultForm form) {
        String option = form.getResultOption();

        if (option == null || option.isBlank()) {
            return null;
        }

        return switch (option) {
            case "BLACK_WINS" -> "B";
            case "WHITE_WINS" -> "W";
            case "JIGO" -> "J";
            case "BOTH_LOSE" -> "L";
            case "BOTH_WIN" -> "D";
            default -> throw new IllegalArgumentException("Ungültige Ergebnisauswahl.");
        };
    }

    @GetMapping("/public/tournaments/{id}/pairings")
    public String showPublicTournamentPairings(@PathVariable Long id, Model model) {
        Tournament tournament = tournamentService.findById(id);

        Map<Integer, List<Pairing>> pairingsByRound = new LinkedHashMap<>();

        for (int round = 1; round <= tournament.getNumberOfRounds(); round++) {
            pairingsByRound.put(round, pairingService.findPublishedByTournamentIdAndRound(id, round));
        }

        model.addAttribute("tournament", tournament);
        model.addAttribute("pairingsByRound", pairingsByRound);
        model.addAttribute("availableRounds",
                IntStream.rangeClosed(1, tournament.getNumberOfRounds()).boxed().toList());

        return "public-tournament-pairings";
    }
}