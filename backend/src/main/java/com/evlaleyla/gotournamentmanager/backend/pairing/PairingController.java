package com.evlaleyla.gotournamentmanager.backend.pairing;

import com.evlaleyla.gotournamentmanager.backend.tournament.Tournament;
import com.evlaleyla.gotournamentmanager.backend.tournament.TournamentService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
        var tournament = tournamentService.findById(id);

        Map<Integer, List<Pairing>> pairingsByRound = new LinkedHashMap<>();

        for (int round = 1; round <= tournament.getNumberOfRounds(); round++) {
            pairingsByRound.put(round, pairingService.findByTournamentIdAndRound(id, round));
        }

        model.addAttribute("tournament", tournament);
        model.addAttribute("pairingsByRound", pairingsByRound);
        model.addAttribute("availableRounds",
                IntStream.rangeClosed(1, tournament.getNumberOfRounds()).boxed().toList());

        return "tournament-pairings";
    }

    @PostMapping("/tournaments/{id}/pairings/import")
    public String importPairings(@PathVariable Long id,
                                 MultipartFile file,
                                 Integer roundSection,
                                 RedirectAttributes redirectAttributes) {

        String redirectTarget = "redirect:/tournaments/" + id + "/pairings" +
                (roundSection != null ? "#round-" + roundSection : "");

        if (file == null || file.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Bitte eine CSV-Datei auswählen.");
            return redirectTarget;
        }

        try {
            pairingService.replacePairingsFromCsv(id, roundSection, file);
            redirectAttributes.addFlashAttribute("successMessage", "Paarungen wurden erfolgreich importiert.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return redirectTarget;
    }

    @GetMapping("/pairings/{id}/result")
    public String showResultForm(@PathVariable Long id, Model model) {
        Pairing pairing = pairingService.findById(id);

        PairingResultForm pairingResultForm = new PairingResultForm();
        fillFormFromResult(pairing.getResult(), pairingResultForm);

        model.addAttribute("pairing", pairing);
        model.addAttribute("pairingResultForm", pairingResultForm);

        return "pairing-result-form";
    }

    @PostMapping("/pairings/{id}/result")
    public String saveResult(@PathVariable Long id,
                             @ModelAttribute PairingResultForm pairingResultForm,
                             Model model,
                             RedirectAttributes redirectAttributes) {

        Pairing pairing = pairingService.findById(id);

        try {
            String resultCode = buildResultCode(pairingResultForm);
            Pairing updatedPairing = pairingService.updateResult(id, resultCode);

            redirectAttributes.addFlashAttribute("successMessage", "Ergebnis wurde erfolgreich gespeichert.");
            return "redirect:/tournaments/" + updatedPairing.getTournament().getId()
                    + "/pairings#round-" + updatedPairing.getRoundNumber();

        } catch (IllegalArgumentException e) {
            model.addAttribute("pairing", pairing);
            model.addAttribute("pairingResultForm", pairingResultForm);
            model.addAttribute("errorMessage", e.getMessage());
            return "pairing-result-form";
        }
    }

    private void fillFormFromResult(String result, PairingResultForm form) {
        if (result == null || result.isBlank()) {
            return;
        }

        if ("0".equals(result)) {
            form.setResultOption("JIGO");
            return;
        }

        if (result.startsWith("B+")) {
            fillWinnerResult("BLACK", result.substring(2), form);
            return;
        }

        if (result.startsWith("W+")) {
            fillWinnerResult("WHITE", result.substring(2), form);
        }
    }

    private void fillWinnerResult(String winnerPrefix, String suffix, PairingResultForm form) {
        switch (suffix) {
            case "R" -> form.setResultOption(winnerPrefix + "_RESIGNATION");
            case "T" -> form.setResultOption(winnerPrefix + "_TIME");
            case "F" -> form.setResultOption(winnerPrefix + "_FORFEIT");
            default -> {
                form.setResultOption(winnerPrefix + "_POINTS");
                form.setPointMargin(suffix.replace(".", ","));
            }
        }
    }

    private String buildResultCode(PairingResultForm form) {
        String option = form.getResultOption();

        if (option == null || option.isBlank()) {
            return null;
        }

        return switch (option) {
            case "BLACK_RESIGNATION" -> "B+R";
            case "WHITE_RESIGNATION" -> "W+R";
            case "BLACK_TIME" -> "B+T";
            case "WHITE_TIME" -> "W+T";
            case "BLACK_FORFEIT" -> "B+F";
            case "WHITE_FORFEIT" -> "W+F";
            case "JIGO" -> "0";
            case "BLACK_POINTS" -> "B+" + normalizePointMargin(form.getPointMargin());
            case "WHITE_POINTS" -> "W+" + normalizePointMargin(form.getPointMargin());
            default -> throw new IllegalArgumentException("Ungültige Ergebnisauswahl.");
        };
    }

    private String normalizePointMargin(String pointMargin) {
        if (pointMargin == null || pointMargin.isBlank()) {
            throw new IllegalArgumentException("Bitte eine Punktedifferenz eingeben.");
        }

        String normalized = pointMargin.trim().replace(",", ".");

        if (!normalized.matches("\\d+(\\.\\d+)?")) {
            throw new IllegalArgumentException("Die Punktedifferenz ist ungültig.");
        }

        return normalized;
    }

    @GetMapping("/public/tournaments/{id}/pairings")
    public String showPublicTournamentPairings(@PathVariable Long id, Model model) {
        Tournament tournament = tournamentService.findById(id);

        Map<Integer, List<Pairing>> pairingsByRound = new LinkedHashMap<>();

        for (int round = 1; round <= tournament.getNumberOfRounds(); round++) {
            pairingsByRound.put(round, pairingService.findByTournamentIdAndRound(id, round));
        }

        model.addAttribute("tournament", tournament);
        model.addAttribute("pairingsByRound", pairingsByRound);
        model.addAttribute("availableRounds",
                IntStream.rangeClosed(1, tournament.getNumberOfRounds()).boxed().toList());

        return "public-tournament-pairings";
    }

    @GetMapping("/tournaments/{id}/pairings/template")
    public ResponseEntity<byte[]> downloadPairingTemplate(@PathVariable Long id,
                                                          @RequestParam Integer round) {
        Tournament tournament = tournamentService.findById(id);
        validateRound(round, tournament);

        String csv = """
            Runde;Tisch;Schwarz;Weiß;Ergebnis
            %d;1;Anna Müller;Max Schmidt;
            %d;2;Lisa Bauer;Tom Weber;
            """.formatted(round, round);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"round_template_" + round + ".csv\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(addUtf8Bom(csv));
    }

    private void validateRound(Integer round, Tournament tournament) {
        if (round == null) {
            throw new IllegalArgumentException("Die Runde ist erforderlich.");
        }

        if (round < 1 || round > tournament.getNumberOfRounds()) {
            throw new IllegalArgumentException(
                    "Die Runde " + round + " liegt außerhalb der definierten Turnierrunden."
            );
        }
    }

    private byte[] addUtf8Bom(String csv) {
        byte[] bom = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
        byte[] content = csv.getBytes(StandardCharsets.UTF_8);

        byte[] csvWithBom = new byte[bom.length + content.length];
        System.arraycopy(bom, 0, csvWithBom, 0, bom.length);
        System.arraycopy(content, 0, csvWithBom, bom.length, content.length);

        return csvWithBom;
    }
}