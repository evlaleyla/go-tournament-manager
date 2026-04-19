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

    public MacMahonController(TournamentService tournamentService,
                              PairingService pairingService,
                              MacMahonInterfaceService macMahonInterfaceService) {
        this.tournamentService = tournamentService;
        this.pairingService = pairingService;
        this.macMahonInterfaceService = macMahonInterfaceService;
    }

    @PostMapping("/tournaments/{id}/pairings/import-macmahon")
    public String importMacMahonPairings(@PathVariable Long id,
                                         @RequestParam(required = false) Integer roundNumber,
                                         @RequestParam MultipartFile file,
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
                    "MacMahon-Paarungen wurden erfolgreich importiert bzw. aktualisiert."
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

        model.addAttribute("tournament", tournament);
        model.addAttribute("wallListEntries", null);
        model.addAttribute("roundColumnCount", 0);

        return "macmahon-walllist";
    }

    @PostMapping("/tournaments/{id}/walllist/import-macmahon")
    public String importMacMahonWallList(@PathVariable Long id,
                                         MultipartFile file,
                                         Model model) {
        Tournament tournament = tournamentService.findById(id);

        model.addAttribute("tournament", tournament);

        if (file == null || file.isEmpty()) {
            model.addAttribute(
                    "errorMessage",
                    "Bitte eine MacMahon-Wall-List-Datei auswählen."
            );
            model.addAttribute("wallListEntries", null);
            model.addAttribute("roundColumnCount", 0);
            return "macmahon-walllist";
        }

        try {
            List<MacMahonWallListEntry> entries = macMahonInterfaceService.parseWallList(file);

            int roundColumnCount = entries.isEmpty()
                    ? 0
                    : entries.get(0).getRoundStatuses().size();

            model.addAttribute("wallListEntries", entries);
            model.addAttribute("roundColumnCount", roundColumnCount);
            model.addAttribute(
                    "successMessage",
                    "MacMahon-Wall-List wurde erfolgreich eingelesen."
            );

        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("wallListEntries", null);
            model.addAttribute("roundColumnCount", 0);
        }

        return "macmahon-walllist";
    }
}