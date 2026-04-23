package com.evlaleyla.gotournamentmanager.backend.macmahon;

import com.evlaleyla.gotournamentmanager.backend.pairing.PairingService;
import com.evlaleyla.gotournamentmanager.backend.tournament.Tournament;
import com.evlaleyla.gotournamentmanager.backend.tournament.TournamentService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
public class MacMahonController {

    private final TournamentService tournamentService;
    private final PairingService pairingService;
    private final MacMahonInterfaceService macMahonInterfaceService;
    private final TournamentStandingService tournamentStandingService;

    public MacMahonController(TournamentService tournamentService,
                              PairingService pairingService,
                              MacMahonInterfaceService macMahonInterfaceService,
                              TournamentStandingService tournamentStandingService) {
        this.tournamentService = tournamentService;
        this.pairingService = pairingService;
        this.macMahonInterfaceService = macMahonInterfaceService;
        this.tournamentStandingService = tournamentStandingService;
    }

    @PostMapping("/tournaments/{id}/pairings/import-macmahon")
    public String importMacMahonPairings(@PathVariable Long id,
                                         @RequestParam(required = false) Integer roundNumber,
                                         @RequestParam("file") MultipartFile file,
                                         RedirectAttributes redirectAttributes) {
        if (roundNumber == null) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Bitte eine Runde auswählen."
            );
            return "redirect:/tournaments/" + id + "/pairings";
        }

        if (file == null || file.isEmpty()) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Bitte eine MacMahon-Datei auswählen."
            );
            return "redirect:/tournaments/" + id + "/pairings#round-" + roundNumber;
        }
        try {
            pairingService.importPairingsFromMacMahon(id, roundNumber, file);
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Die Paarungen für Runde " + roundNumber + " wurden erfolgreich aus MacMahon importiert."
            );
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    e.getMessage()
            );
        }

        return "redirect:/tournaments/" + id + "/pairings#round-" + roundNumber;
    }

    @GetMapping("/tournaments/{id}/walllist")
    public String showWallListUpload(@PathVariable Long id, Model model) {
        Tournament tournament = tournamentService.findById(id);
        List<TournamentStanding> standings = tournamentStandingService.findByTournamentId(id);

        int roundColumnCount = 0;
        if (!standings.isEmpty()) {
            roundColumnCount = standings.get(0).getRoundStatuses().size();
        }

        model.addAttribute("tournament", tournament);
        model.addAttribute("wallListEntries", standings);
        model.addAttribute("roundColumnCount", roundColumnCount);

        return "macmahon-walllist";
    }

    @PostMapping("/tournaments/{id}/walllist/import-macmahon")
    public String importMacMahonWallList(@PathVariable Long id,
                                         @RequestParam("file") MultipartFile file,
                                         RedirectAttributes redirectAttributes) {

        if (file == null || file.isEmpty()) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Bitte eine MacMahon-Wall-List-Datei auswählen."
            );
            return "redirect:/tournaments/" + id + "/walllist";
        }

        try {
            tournamentStandingService.importCurrentWallList(id, file);
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Die aktuelle Wall-List wurde erfolgreich importiert."
            );
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/tournaments/" + id + "/walllist";
    }
}