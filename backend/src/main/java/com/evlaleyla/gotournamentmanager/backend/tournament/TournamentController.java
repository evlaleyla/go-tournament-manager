package com.evlaleyla.gotournamentmanager.backend.tournament;

import com.evlaleyla.gotournamentmanager.backend.macmahon.MacMahonExportService;
import com.evlaleyla.gotournamentmanager.backend.registration.RegistrationService;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.stream.IntStream;

@Controller
public class TournamentController {

    private final TournamentService tournamentService;
    private final RegistrationService registrationService;
    private final MacMahonExportService macMahonExportService;

    public TournamentController(TournamentService tournamentService,
                                RegistrationService registrationService,
                                MacMahonExportService macMahonExportService) {
        this.tournamentService = tournamentService;
        this.registrationService = registrationService;
        this.macMahonExportService = macMahonExportService;
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

    @GetMapping("/public/tournaments/{id}")
    public String showPublicTournamentDetail(@PathVariable Long id, Model model) {
        model.addAttribute("tournament", tournamentService.findById(id));
        return "public-tournament-detail";
    }

    @GetMapping("/public/tournaments")
    public String showPublicTournaments(Model model){
        model.addAttribute("tournaments", tournamentService.findAll());
        return "public-tournaments";
    }

    @GetMapping("/tournaments/{id}/startlist")
    public String showTournamentStartList(@PathVariable Long id,
                                          @RequestParam(required = false) Integer round,
                                          Model model) {
        Tournament tournament = tournamentService.findById(id);
        int selectedRound = normalizeRound(round, tournament);

        model.addAttribute("tournament", tournament);
        model.addAttribute("selectedRound", selectedRound);
        model.addAttribute("availableRounds",
                IntStream.rangeClosed(1, tournament.getNumberOfRounds()).boxed().toList());
        model.addAttribute("registrations",
                registrationService.findStartListByTournamentIdAndRound(id, selectedRound));

        return "tournament-startlist";
    }

    private int normalizeRound(Integer round, Tournament tournament) {
        Integer numberOfRounds = tournament.getNumberOfRounds();

        if (numberOfRounds == null || numberOfRounds < 1) {
            throw new IllegalArgumentException("Für dieses Turnier ist keine gültige Rundenzahl definiert.");
        }

        if (round == null) {
            return 1;
        }

        if (round < 1 || round > numberOfRounds) {
            throw new IllegalArgumentException(
                    "Die angeforderte Runde " + round +
                            " ist ungültig. Erlaubt sind nur Runden von 1 bis " + numberOfRounds + "."
            );
        }

        return round;
    }

    @GetMapping("/tournaments/{id}/startlist/export-macmahon")
    public ResponseEntity<byte[]> exportTournamentStartListForMacMahon(@PathVariable Long id) {
        Tournament tournament = tournamentService.findById(id);
        byte[] content = macMahonExportService.exportParticipantsForMacMahon(id);

        String safeTournamentName = tournament.getName().replaceAll("[^a-zA-Z0-9-_]", "_");
        String fileName = "macmahon_startliste_" + safeTournamentName + ".txt";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + fileName + "\"")
                .contentType(MediaType.parseMediaType("text/plain;charset=UTF-8"))
                .body(content);
    }

    @GetMapping("/public/tournaments/{id}/startlist")
    public String showPublicTournamentStartList(@PathVariable Long id,
                                                @RequestParam(required = false) Integer round,
                                                Model model) {
        Tournament tournament = tournamentService.findById(id);
        int selectedRound = normalizeRound(round, tournament);

        model.addAttribute("tournament", tournament);
        model.addAttribute("selectedRound", selectedRound);
        model.addAttribute("availableRounds",
                IntStream.rangeClosed(1, tournament.getNumberOfRounds()).boxed().toList());
        model.addAttribute("registrations",
                registrationService.findStartListByTournamentIdAndRound(id, selectedRound));

        return "public-tournament-startlist";
    }
}