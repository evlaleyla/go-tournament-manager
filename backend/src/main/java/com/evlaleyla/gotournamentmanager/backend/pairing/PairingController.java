package com.evlaleyla.gotournamentmanager.backend.pairing;

import com.evlaleyla.gotournamentmanager.backend.tournament.Tournament;
import com.evlaleyla.gotournamentmanager.backend.tournament.TournamentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

/**
 * Web controller responsible for displaying tournament pairings,
 * publishing and unpublishing round pairings, and storing pairing results.
 *
 * <p>This controller coordinates UI requests and delegates all business logic
 * to the corresponding service layer.</p>
 */
@Controller
public class PairingController {

    private static final Logger log = LoggerFactory.getLogger(PairingController.class);

    private final PairingService pairingService;
    private final TournamentService tournamentService;

    public PairingController(PairingService pairingService,
                             TournamentService tournamentService) {
        this.pairingService = pairingService;
        this.tournamentService = tournamentService;
    }

    /**
     * Displays the internal pairing overview for a tournament.
     *
     * <p>For each configured round, the corresponding pairings are loaded and
     * grouped by round number. In addition, the controller provides the set of
     * already published rounds so that the UI can show the current publication state.</p>
     *
     * @param id    the tournament ID
     * @param model the Spring MVC model
     * @return the internal pairing view
     */
    @GetMapping("/tournaments/{id}/pairings")
    public String showTournamentPairings(@PathVariable Long id, Model model) {
        log.debug("Loading internal pairing overview for tournamentId={}", id);

        Tournament tournament = tournamentService.findById(id);
        Map<Integer, List<Pairing>> pairingsByRound = buildPairingsByRound(id, tournament, false);
        Set<Integer> publishedRounds = pairingService.findPublishedRoundNumbers(id);
        List<Integer> availableRounds = buildAvailableRounds(tournament);

        model.addAttribute("tournament", tournament);
        model.addAttribute("pairingsByRound", pairingsByRound);
        model.addAttribute("publishedRounds", publishedRounds);
        model.addAttribute("availableRounds", availableRounds);

        log.debug(
                "Internal pairing overview prepared for tournamentId={} with {} configured rounds and {} published rounds",
                id,
                availableRounds.size(),
                publishedRounds.size()
        );

        return "tournament-pairings";
    }

    /**
     * Publishes all pairings of a specific round for the given tournament.
     *
     * @param tournamentId       the tournament ID
     * @param roundNumber        the round number to publish
     * @param redirectAttributes flash message container
     * @return redirect to the round anchor in the pairing overview
     */
    @PostMapping("/tournaments/{tournamentId}/pairings/{roundNumber}/publish")
    public String publishRound(@PathVariable Long tournamentId,
                               @PathVariable Integer roundNumber,
                               RedirectAttributes redirectAttributes) {
        log.info("Publishing pairings for tournamentId={}, roundNumber={}", tournamentId, roundNumber);

        try {
            pairingService.publishRound(tournamentId, roundNumber);

            log.info("Successfully published pairings for tournamentId={}, roundNumber={}", tournamentId, roundNumber);
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Die Paarungen für Runde " + roundNumber + " wurden veröffentlicht."
            );
        } catch (IllegalArgumentException e) {
            log.warn(
                    "Failed to publish pairings for tournamentId={}, roundNumber={}: {}",
                    tournamentId,
                    roundNumber,
                    e.getMessage()
            );
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return buildRoundRedirect(tournamentId, roundNumber);
    }

    /**
     * Removes the public visibility of all pairings of a specific round.
     *
     * @param tournamentId       the tournament ID
     * @param roundNumber        the round number to unpublish
     * @param redirectAttributes flash message container
     * @return redirect to the round anchor in the pairing overview
     */
    @PostMapping("/tournaments/{tournamentId}/pairings/{roundNumber}/unpublish")
    public String unpublishRound(@PathVariable Long tournamentId,
                                 @PathVariable Integer roundNumber,
                                 RedirectAttributes redirectAttributes) {
        log.info("Unpublishing pairings for tournamentId={}, roundNumber={}", tournamentId, roundNumber);

        try {
            pairingService.unpublishRound(tournamentId, roundNumber);

            log.info("Successfully unpublished pairings for tournamentId={}, roundNumber={}", tournamentId, roundNumber);
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Die Veröffentlichung für Runde " + roundNumber + " wurde zurückgenommen."
            );
        } catch (IllegalArgumentException e) {
            log.warn(
                    "Failed to unpublish pairings for tournamentId={}, roundNumber={}: {}",
                    tournamentId,
                    roundNumber,
                    e.getMessage()
            );
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return buildRoundRedirect(tournamentId, roundNumber);
    }

    /**
     * Stores or clears the result of a single pairing.
     *
     * <p>The form sends a UI-specific option value which is translated into the
     * internal domain result code before it is forwarded to the service layer.</p>
     *
     * @param id                 the pairing ID
     * @param pairingResultForm  submitted result form
     * @param redirectAttributes flash message container
     * @return redirect to the affected round section
     */
    @PostMapping("/pairings/{id}/result")
    public String saveResult(@PathVariable Long id,
                             @ModelAttribute PairingResultForm pairingResultForm,
                             RedirectAttributes redirectAttributes) {

        Pairing pairing = pairingService.findById(id);
        log.info(
                "Saving result for pairingId={}, tournamentId={}, roundNumber={}",
                id,
                pairing.getTournament().getId(),
                pairing.getRoundNumber()
        );

        try {
            String resultCode = mapResultOptionToCode(pairingResultForm);
            Pairing updatedPairing = pairingService.updateResult(id, resultCode);

            log.info(
                    "Successfully saved result for pairingId={} with resultCode={}",
                    id,
                    resultCode
            );

            redirectAttributes.addFlashAttribute("successMessage", "Ergebnis wurde erfolgreich gespeichert.");
            return buildRoundRedirect(
                    updatedPairing.getTournament().getId(),
                    updatedPairing.getRoundNumber()
            );

        } catch (IllegalArgumentException e) {
            log.warn("Failed to save result for pairingId={}: {}", id, e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());

            return buildRoundRedirect(
                    pairing.getTournament().getId(),
                    pairing.getRoundNumber()
            );
        }
    }

    /**
     * Displays the public pairing overview of a tournament.
     *
     * <p>Only published pairings are loaded for each round.</p>
     *
     * @param id    the tournament ID
     * @param model the Spring MVC model
     * @return the public pairing view
     */
    @GetMapping("/public/tournaments/{id}/pairings")
    public String showPublicTournamentPairings(@PathVariable Long id, Model model) {
        log.debug("Loading public pairing overview for tournamentId={}", id);

        Tournament tournament = tournamentService.findById(id);
        Map<Integer, List<Pairing>> pairingsByRound = buildPairingsByRound(id, tournament, true);
        List<Integer> availableRounds = buildAvailableRounds(tournament);

        model.addAttribute("tournament", tournament);
        model.addAttribute("pairingsByRound", pairingsByRound);
        model.addAttribute("availableRounds", availableRounds);

        log.debug(
                "Public pairing overview prepared for tournamentId={} with {} configured rounds",
                id,
                availableRounds.size()
        );

        return "public-tournament-pairings";
    }

    /**
     * Builds a round-to-pairings map for the given tournament.
     *
     * @param tournamentId the tournament ID
     * @param tournament   the loaded tournament
     * @param publicOnly   whether only published pairings should be loaded
     * @return ordered map of round numbers to pairing lists
     */
    private Map<Integer, List<Pairing>> buildPairingsByRound(Long tournamentId,
                                                             Tournament tournament,
                                                             boolean publicOnly) {
        Map<Integer, List<Pairing>> pairingsByRound = new LinkedHashMap<>();

        for (int round = 1; round <= tournament.getNumberOfRounds(); round++) {
            List<Pairing> pairings = publicOnly
                    ? pairingService.findPublishedByTournamentIdAndRound(tournamentId, round)
                    : pairingService.findByTournamentIdAndRound(tournamentId, round);

            pairingsByRound.put(round, pairings);
        }

        return pairingsByRound;
    }

    /**
     * Builds the list of available round numbers for the UI.
     *
     * @param tournament the tournament entity
     * @return list of round numbers from 1 to numberOfRounds
     */
    private List<Integer> buildAvailableRounds(Tournament tournament) {
        return IntStream.rangeClosed(1, tournament.getNumberOfRounds())
                .boxed()
                .toList();
    }

    /**
     * Translates the submitted UI option into the internal result code.
     *
     * @param form the submitted result form
     * @return internal result code or {@code null} if the pairing remains open
     */
    private String mapResultOptionToCode(PairingResultForm form) {
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

    /**
     * Builds the redirect URL to a specific round section in the internal pairing view.
     *
     * @param tournamentId the tournament ID
     * @param roundNumber  the round number
     * @return redirect URL with anchor
     */
    private String buildRoundRedirect(Long tournamentId, Integer roundNumber) {
        return "redirect:/tournaments/" + tournamentId + "/pairings#round-" + roundNumber;
    }
}