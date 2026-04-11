package com.evlaleyla.gotournamentmanager.backend.tournament;

import com.evlaleyla.gotournamentmanager.backend.participant.Participant;
import com.evlaleyla.gotournamentmanager.backend.registration.Registration;
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

import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Controller
public class TournamentController {

    private final TournamentService tournamentService;
    private final RegistrationService registrationService;

    public TournamentController(TournamentService tournamentService,
                                RegistrationService registrationService) {
        this.tournamentService = tournamentService;
        this.registrationService = registrationService;
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
    public String showTournamentStartList(@PathVariable Long id, Model model) {
        model.addAttribute("tournament", tournamentService.findById(id));
        model.addAttribute("registrations", registrationService.findStartListByTournamentId(id));
        return "tournament-startlist";
    }
    @GetMapping("/tournaments/{id}/startlist/export")
    public ResponseEntity<byte[]> exportTournamentStartList(@PathVariable Long id) {
        Tournament tournament = tournamentService.findById(id);
        List<Registration> registrations = registrationService.findStartListByTournamentId(id);

        StringBuilder csv = new StringBuilder();

        csv.append("Nr;Nachname;Vorname;E-Mail;Verein;Land;Rang;Geburtsdatum;Anmeldedatum\n");

        for (int i = 0; i < registrations.size(); i++) {
            Registration registration = registrations.get(i);
            Participant participant = registration.getParticipant();

            csv.append(i + 1).append(";");
            csv.append(escapeCsv(participant.getLastName())).append(";");
            csv.append(escapeCsv(participant.getFirstName())).append(";");
            csv.append(escapeCsv(participant.getEmail())).append(";");
            csv.append(escapeCsv(participant.getClub())).append(";");
            csv.append(escapeCsv(participant.getCountry())).append(";");
            csv.append(escapeCsv(participant.getRank())).append(";");
            csv.append(escapeCsv(formatDate(participant.getBirthDate()))).append(";");
            csv.append(escapeCsv(formatDate(registration.getRegistrationDate()))).append("\n");
        }

        String fileName = "startliste_" + tournament.getName().replaceAll("[^a-zA-Z0-9-_]", "_") + ".csv";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(addUtf8Bom(csv.toString()));
    }

    private byte[] addUtf8Bom(String content) {
        byte[] bom = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);

        byte[] result = new byte[bom.length + bytes.length];
        System.arraycopy(bom, 0, result, 0, bom.length);
        System.arraycopy(bytes, 0, result, bom.length, bytes.length);

        return result;
    }

    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }

        String escapedValue = value.replace("\"", "\"\"");
        return "\"" + escapedValue + "\"";
    }

    private String formatDate(java.time.LocalDate date) {
        if (date == null) {
            return "";
        }
        return date.format(DateTimeFormatter.ISO_DATE);
    }
}