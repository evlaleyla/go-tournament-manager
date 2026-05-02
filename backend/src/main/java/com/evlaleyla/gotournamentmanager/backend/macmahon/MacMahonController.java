package com.evlaleyla.gotournamentmanager.backend.macmahon;

import com.evlaleyla.gotournamentmanager.backend.pairing.PairingService;
import com.evlaleyla.gotournamentmanager.backend.tournament.Tournament;
import com.evlaleyla.gotournamentmanager.backend.tournament.TournamentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Web controller for MacMahon-related tournament operations.
 *
 * <p>This controller handles:
 * <ul>
 *     <li>importing MacMahon pairing files for a specific round,</li>
 *     <li>importing MacMahon wall-list files,</li>
 *     <li>displaying internal and public wall-list views,</li>
 *     <li>publishing and unpublishing wall-list data.</li>
 * </ul>
 *
 * <p>User-facing messages remain German because the application UI is German,
 * while code, comments, and logging are written in English for professional consistency.
 */
@Controller
public class MacMahonController {

    private static final Logger log = LoggerFactory.getLogger(MacMahonController.class);

    private static final String INTERNAL_WALL_LIST_VIEW = "macmahon-walllist";
    private static final String PUBLIC_WALL_LIST_VIEW = "public-tournament-walllist";

    private final TournamentService tournamentService;
    private final PairingService pairingService;
    private final TournamentStandingService tournamentStandingService;

    public MacMahonController(TournamentService tournamentService,
                              PairingService pairingService,
                              TournamentStandingService tournamentStandingService) {
        this.tournamentService = tournamentService;
        this.pairingService = pairingService;
        this.tournamentStandingService = tournamentStandingService;
    }

    /**
     * Imports pairing data from a MacMahon export file for a specific round.
     */
    @PostMapping("/tournaments/{id}/pairings/import-macmahon")
    public String importMacMahonPairings(@PathVariable Long id,
                                         @RequestParam(required = false) Integer roundNumber,
                                         @RequestParam("file") MultipartFile file,
                                         RedirectAttributes redirectAttributes) {

        log.info(
                "Received MacMahon pairing import request for tournamentId={}, roundNumber={}, fileName={}",
                id,
                roundNumber,
                getOriginalFilename(file)
        );

        if (roundNumber == null) {
            log.warn("Pairing import rejected because no round number was provided for tournamentId={}", id);
            redirectAttributes.addFlashAttribute("errorMessage", "Bitte eine Runde auswählen.");
            return buildPairingsRedirect(id, null);
        }

        if (file == null || file.isEmpty()) {
            log.warn(
                    "Pairing import rejected because file was missing or empty for tournamentId={}, roundNumber={}",
                    id,
                    roundNumber
            );
            redirectAttributes.addFlashAttribute("errorMessage", "Bitte eine MacMahon-Datei auswählen.");
            return buildPairingsRedirect(id, roundNumber);
        }

        try {
            pairingService.importPairingsFromMacMahon(id, roundNumber, file);

            log.info(
                    "Successfully imported MacMahon pairings for tournamentId={}, roundNumber={}, fileName={}",
                    id,
                    roundNumber,
                    getOriginalFilename(file)
            );

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Die Paarungen für Runde " + roundNumber + " wurden erfolgreich aus MacMahon importiert."
            );
        } catch (IllegalArgumentException e) {
            log.warn(
                    "MacMahon pairing import failed for tournamentId={}, roundNumber={}, reason={}",
                    id,
                    roundNumber,
                    e.getMessage()
            );
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return buildPairingsRedirect(id, roundNumber);
    }

    /**
     * Shows the internal wall-list administration page for a tournament.
     */
    @GetMapping("/tournaments/{id}/walllist")
    public String showWallListUpload(@PathVariable Long id, Model model) {
        log.debug("Loading internal wall-list page for tournamentId={}", id);

        Tournament tournament = tournamentService.findById(id);
        List<TournamentStanding> standings = tournamentStandingService.findByTournamentId(id);

        populateWallListModel(model, tournament, standings);

        log.debug(
                "Loaded internal wall-list page for tournamentId={} with {} standing entries",
                id,
                standings.size()
        );

        return INTERNAL_WALL_LIST_VIEW;
    }

    /**
     * Imports the current wall-list from a MacMahon export file.
     *
     * <p>After import, the wall-list is stored internally and must be published separately
     * before it becomes visible in the public area.
     */
    @PostMapping("/tournaments/{id}/walllist/import-macmahon")
    public String importMacMahonWallList(@PathVariable Long id,
                                         @RequestParam("file") MultipartFile file,
                                         RedirectAttributes redirectAttributes) {

        log.info(
                "Received MacMahon wall-list import request for tournamentId={}, fileName={}",
                id,
                getOriginalFilename(file)
        );

        if (file == null || file.isEmpty()) {
            log.warn("Wall-list import rejected because file was missing or empty for tournamentId={}", id);
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Bitte eine MacMahon-Wall-List-Datei auswählen."
            );
            return buildWallListRedirect(id);
        }

        try {
            tournamentStandingService.importCurrentWallList(id, file);

            log.info(
                    "Successfully imported MacMahon wall-list for tournamentId={}, fileName={}",
                    id,
                    getOriginalFilename(file)
            );

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Die aktuelle Wall-List wurde erfolgreich importiert."
            );
        } catch (IllegalArgumentException e) {
            log.warn(
                    "MacMahon wall-list import failed for tournamentId={}, reason={}",
                    id,
                    e.getMessage()
            );
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return buildWallListRedirect(id);
    }

    /**
     * Shows the public wall-list page.
     *
     * <p>If the wall-list is not published, the public view is still rendered,
     * but without standing data.
     */
    @GetMapping("/public/tournaments/{id}/walllist")
    public String showPublicWallList(@PathVariable Long id, Model model) {
        log.debug("Loading public wall-list page for tournamentId={}", id);

        Tournament tournament = tournamentService.findById(id);
        List<TournamentStanding> standings = List.of();

        if (tournament.isWallListPublished()) {
            standings = tournamentStandingService.findByTournamentId(id);
            log.debug(
                    "Wall-list is published for tournamentId={}, loaded {} public standing entries",
                    id,
                    standings.size()
            );
        } else {
            log.debug("Wall-list is not published for tournamentId={}, showing empty public view", id);
        }

        populateWallListModel(model, tournament, standings);

        return PUBLIC_WALL_LIST_VIEW;
    }

    /**
     * Publishes the currently stored wall-list for public visibility.
     */
    @PostMapping("/tournaments/{id}/walllist/publish")
    public String publishWallList(@PathVariable Long id,
                                  RedirectAttributes redirectAttributes) {

        log.info("Received wall-list publish request for tournamentId={}", id);

        try {
            tournamentStandingService.publishWallList(id);

            log.info("Successfully published wall-list for tournamentId={}", id);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Die Wall-List wurde veröffentlicht."
            );
        } catch (IllegalArgumentException e) {
            log.warn(
                    "Wall-list publish failed for tournamentId={}, reason={}",
                    id,
                    e.getMessage()
            );
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return buildWallListRedirect(id);
    }

    /**
     * Removes the public visibility of the current wall-list.
     */
    @PostMapping("/tournaments/{id}/walllist/unpublish")
    public String unpublishWallList(@PathVariable Long id,
                                    RedirectAttributes redirectAttributes) {

        log.info("Received wall-list unpublish request for tournamentId={}", id);

        try {
            tournamentStandingService.unpublishWallList(id);

            log.info("Successfully unpublished wall-list for tournamentId={}", id);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Die Veröffentlichung der Wall-List wurde zurückgenommen."
            );
        } catch (IllegalArgumentException e) {
            log.warn(
                    "Wall-list unpublish failed for tournamentId={}, reason={}",
                    id,
                    e.getMessage()
            );
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return buildWallListRedirect(id);
    }

    /**
     * Adds all wall-list related view attributes to the model.
     */
    private void populateWallListModel(Model model,
                                       Tournament tournament,
                                       List<TournamentStanding> standings) {
        model.addAttribute("tournament", tournament);
        model.addAttribute("wallListEntries", standings);
        model.addAttribute("roundColumnCount", determineRoundColumnCount(standings));
    }

    /**
     * Determines how many round columns need to be rendered in the wall-list table.
     *
     * <p>The current implementation assumes that all standing entries have the same
     * number of round status values. Therefore, the first entry is sufficient.
     */
    private int determineRoundColumnCount(List<TournamentStanding> standings) {
        if (standings == null || standings.isEmpty()) {
            return 0;
        }
        return standings.get(0).getRoundStatuses().size();
    }

    /**
     * Builds the redirect URL back to the pairing page.
     *
     * <p>If a round number is available, the redirect points directly to the corresponding
     * round anchor in the UI.
     */
    private String buildPairingsRedirect(Long tournamentId, Integer roundNumber) {
        if (roundNumber == null) {
            return "redirect:/tournaments/" + tournamentId + "/pairings";
        }
        return "redirect:/tournaments/" + tournamentId + "/pairings#round-" + roundNumber;
    }

    /**
     * Builds the redirect URL back to the wall-list page.
     */
    private String buildWallListRedirect(Long tournamentId) {
        return "redirect:/tournaments/" + tournamentId + "/walllist";
    }

    /**
     * Safely extracts the original filename for logging purposes.
     */
    private String getOriginalFilename(MultipartFile file) {
        if (file == null || file.getOriginalFilename() == null || file.getOriginalFilename().isBlank()) {
            return "<unknown>";
        }
        return file.getOriginalFilename();
    }
}