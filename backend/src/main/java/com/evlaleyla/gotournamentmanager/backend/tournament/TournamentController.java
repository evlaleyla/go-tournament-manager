package com.evlaleyla.gotournamentmanager.backend.tournament;

import com.evlaleyla.gotournamentmanager.backend.macmahon.MacMahonExportService;
import com.evlaleyla.gotournamentmanager.backend.registration.RegistrationService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.stream.IntStream;

/**
 * Web controller for tournament administration and public tournament views.
 *
 * <p>This controller is responsible for:
 * <ul>
 *     <li>listing and managing tournaments in the admin area,</li>
 *     <li>showing tournament details and dashboards,</li>
 *     <li>providing start lists for admin and public views,</li>
 *     <li>exporting start lists for MacMahon,</li>
 *     <li>serving public tournament detail pages.</li>
 * </ul>
 * </p>
 */
@Controller
public class TournamentController {

    private static final Logger log = LoggerFactory.getLogger(TournamentController.class);

    private static final String TOURNAMENT_FORM_VIEW = "tournament-form";
    private static final String TOURNAMENT_LIST_VIEW = "tournaments";
    private static final String TOURNAMENT_DETAIL_VIEW = "tournament-detail";
    private static final String PUBLIC_TOURNAMENT_DETAIL_VIEW = "public-tournament-detail";
    private static final String PUBLIC_TOURNAMENT_LIST_VIEW = "public-tournaments";
    private static final String TOURNAMENT_STARTLIST_VIEW = "tournament-startlist";
    private static final String PUBLIC_TOURNAMENT_STARTLIST_VIEW = "public-tournament-startlist";

    private final TournamentService tournamentService;
    private final RegistrationService registrationService;
    private final MacMahonExportService macMahonExportService;
    private final TournamentDashboardService tournamentDashboardService;

    public TournamentController(TournamentService tournamentService,
                                RegistrationService registrationService,
                                MacMahonExportService macMahonExportService,
                                TournamentDashboardService tournamentDashboardService) {
        this.tournamentService = tournamentService;
        this.registrationService = registrationService;
        this.macMahonExportService = macMahonExportService;
        this.tournamentDashboardService = tournamentDashboardService;
    }

    /**
     * Shows the tournament list in the admin area.
     *
     * @param search optional search term for tournament names
     * @param status optional tournament status filter
     * @param model  Spring MVC model
     * @return the tournament list view
     */
    @GetMapping("/tournaments")
    public String showTournaments(@RequestParam(required = false) String search,
                                  @RequestParam(required = false) TournamentStatus status,
                                  Model model) {
        log.debug("Showing tournament list with search='{}' and status={}", search, status);

        model.addAttribute("tournaments", tournamentService.search(search, status));
        model.addAttribute("search", search);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("statuses", TournamentStatus.values());
        model.addAttribute("tournamentNames", tournamentService.findDistinctNames());

        return TOURNAMENT_LIST_VIEW;
    }

    /**
     * Shows the form for creating a new tournament.
     *
     * @param model Spring MVC model
     * @return the tournament form view
     */
    @GetMapping("/tournaments/new")
    public String showTournamentForm(Model model) {
        log.debug("Showing form for creating a new tournament.");

        model.addAttribute("tournament", new Tournament());
        populateTournamentFormModel(model, false);

        return TOURNAMENT_FORM_VIEW;
    }

    /**
     * Creates a new tournament.
     *
     * @param tournament          tournament form data
     * @param bindingResult       validation result
     * @param model               Spring MVC model
     * @param redirectAttributes  redirect attributes for flash messages
     * @return redirect to tournament list on success, otherwise form view
     */
    @PostMapping("/tournaments")
    public String createTournament(@Valid @ModelAttribute Tournament tournament,
                                   BindingResult bindingResult,
                                   Model model,
                                   RedirectAttributes redirectAttributes) {
        log.info("Received request to create tournament '{}'.", tournament.getName());

        validateTournamentDates(tournament, bindingResult);

        if (bindingResult.hasErrors()) {
            log.debug("Tournament creation validation failed for '{}'.", tournament.getName());
            populateTournamentFormModel(model, false);
            return TOURNAMENT_FORM_VIEW;
        }

        try {
            Tournament savedTournament = tournamentService.save(tournament);
            log.info("Tournament created successfully with id={}.", savedTournament.getId());

            redirectAttributes.addFlashAttribute("successMessage", "Turnier wurde erfolgreich angelegt.");
            return "redirect:/tournaments";

        } catch (IllegalArgumentException e) {
            log.warn("Tournament creation failed for '{}': {}", tournament.getName(), e.getMessage());

            bindingResult.reject("tournament.invalid", e.getMessage());
            populateTournamentFormModel(model, false);
            return TOURNAMENT_FORM_VIEW;
        }
    }

    /**
     * Shows the admin detail page of a tournament including dashboard data.
     *
     * @param id    tournament id
     * @param model Spring MVC model
     * @return the tournament detail view
     */
    @GetMapping("/tournaments/{id}")
    public String showTournamentDetail(@PathVariable Long id, Model model) {
        log.debug("Showing admin tournament detail page for tournamentId={}.", id);

        Tournament tournament = tournamentService.findById(id);
        model.addAttribute("tournament", tournament);
        model.addAttribute("dashboard", tournamentDashboardService.buildDashboard(tournament));

        return TOURNAMENT_DETAIL_VIEW;
    }

    /**
     * Shows the form for editing an existing tournament.
     *
     * @param id    tournament id
     * @param model Spring MVC model
     * @return the tournament form view
     */
    @GetMapping("/tournaments/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model) {
        log.debug("Showing edit form for tournamentId={}.", id);

        model.addAttribute("tournament", tournamentService.findById(id));
        populateTournamentFormModel(model, true);

        return TOURNAMENT_FORM_VIEW;
    }

    /**
     * Updates an existing tournament.
     *
     * @param id                 tournament id
     * @param tournament         updated tournament data
     * @param bindingResult      validation result
     * @param model              Spring MVC model
     * @param redirectAttributes redirect attributes for flash messages
     * @return redirect to detail page on success, otherwise form view
     */
    @PostMapping("/tournaments/{id}")
    public String updateTournament(@PathVariable Long id,
                                   @Valid @ModelAttribute Tournament tournament,
                                   BindingResult bindingResult,
                                   Model model,
                                   RedirectAttributes redirectAttributes) {
        tournament.setId(id);

        log.info("Received request to update tournamentId={}.", id);

        validateTournamentDates(tournament, bindingResult);

        if (bindingResult.hasErrors()) {
            log.debug("Tournament update validation failed for tournamentId={}.", id);
            populateTournamentFormModel(model, true);
            return TOURNAMENT_FORM_VIEW;
        }

        try {
            tournamentService.update(id, tournament);
            log.info("Tournament updated successfully for tournamentId={}.", id);

            redirectAttributes.addFlashAttribute("successMessage", "Turnier wurde erfolgreich aktualisiert.");
            return "redirect:/tournaments/" + id;

        } catch (IllegalArgumentException e) {
            log.warn("Tournament update failed for tournamentId={}: {}", id, e.getMessage());

            bindingResult.reject("tournament.invalid", e.getMessage());
            populateTournamentFormModel(model, true);
            return TOURNAMENT_FORM_VIEW;
        }
    }

    /**
     * Deletes a tournament.
     *
     * @param id                 tournament id
     * @param redirectAttributes redirect attributes for flash messages
     * @return redirect to tournament list
     */
    @PostMapping("/tournaments/{id}/delete")
    public String deleteTournament(@PathVariable Long id,
                                   RedirectAttributes redirectAttributes) {
        log.info("Received request to delete tournamentId={}.", id);

        tournamentService.deleteById(id);

        log.info("Tournament deleted successfully for tournamentId={}.", id);
        redirectAttributes.addFlashAttribute("successMessage", "Turnier wurde erfolgreich gelöscht.");

        return "redirect:/tournaments";
    }

    /**
     * Shows the public detail page of a tournament.
     *
     * @param id    tournament id
     * @param model Spring MVC model
     * @return the public tournament detail view
     */
    @GetMapping("/public/tournaments/{id}")
    public String showPublicTournamentDetail(@PathVariable Long id, Model model) {
        log.debug("Showing public tournament detail page for tournamentId={}.", id);

        model.addAttribute("tournament", tournamentService.findById(id));
        return PUBLIC_TOURNAMENT_DETAIL_VIEW;
    }

    /**
     * Shows the public list of all tournaments.
     *
     * @param model Spring MVC model
     * @return the public tournament list view
     */
    @GetMapping("/public/tournaments")
    public String showPublicTournaments(Model model) {
        log.debug("Showing public tournament list.");

        model.addAttribute("tournaments", tournamentService.findAll());
        return PUBLIC_TOURNAMENT_LIST_VIEW;
    }

    /**
     * Shows the admin start list for a selected round.
     *
     * @param id    tournament id
     * @param round optional round number
     * @param model Spring MVC model
     * @return the admin start list view
     */
    @GetMapping("/tournaments/{id}/startlist")
    public String showTournamentStartList(@PathVariable Long id,
                                          @RequestParam(required = false) Integer round,
                                          Model model) {
        log.debug("Showing admin start list for tournamentId={} and requestedRound={}.", id, round);

        Tournament tournament = tournamentService.findById(id);
        int selectedRound = normalizeRound(round, tournament);

        populateStartListModel(model, tournament, selectedRound, id);
        return TOURNAMENT_STARTLIST_VIEW;
    }

    /**
     * Exports the tournament start list in MacMahon-compatible text format.
     *
     * @param id tournament id
     * @return MacMahon export as downloadable text file
     */
    @GetMapping("/tournaments/{id}/startlist/export-macmahon")
    public ResponseEntity<byte[]> exportTournamentStartListForMacMahon(@PathVariable Long id) {
        log.info("Exporting MacMahon start list for tournamentId={}.", id);

        Tournament tournament = tournamentService.findById(id);
        byte[] content = macMahonExportService.exportParticipantsForMacMahon(id);

        String safeTournamentName = tournament.getName().replaceAll("[^a-zA-Z0-9-_]", "_");
        String fileName = "macmahon_startliste_" + safeTournamentName + ".txt";

        log.info("MacMahon export created successfully for tournamentId={} with fileName='{}'.", id, fileName);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(MediaType.parseMediaType("text/plain;charset=UTF-8"))
                .body(content);
    }

    /**
     * Shows the public start list for a selected round.
     *
     * @param id    tournament id
     * @param round optional round number
     * @param model Spring MVC model
     * @return the public start list view
     */
    @GetMapping("/public/tournaments/{id}/startlist")
    public String showPublicTournamentStartList(@PathVariable Long id,
                                                @RequestParam(required = false) Integer round,
                                                Model model) {
        log.debug("Showing public start list for tournamentId={} and requestedRound={}.", id, round);

        Tournament tournament = tournamentService.findById(id);
        int selectedRound = normalizeRound(round, tournament);

        populateStartListModel(model, tournament, selectedRound, id);
        return PUBLIC_TOURNAMENT_STARTLIST_VIEW;
    }

    /**
     * Validates date relationships of the tournament form.
     *
     * <p>The method adds field errors to the {@link BindingResult} if:
     * <ul>
     *     <li>the end date is before the start date,</li>
     *     <li>the registration deadline is after the start date.</li>
     * </ul>
     * </p>
     *
     * @param tournament     tournament form data
     * @param bindingResult  validation result
     */
    private void validateTournamentDates(Tournament tournament, BindingResult bindingResult) {
        if (tournament.getStartDate() != null
                && tournament.getEndDate() != null
                && tournament.getEndDate().isBefore(tournament.getStartDate())) {
            log.debug("Date validation failed: end date is before start date for tournament '{}'.",
                    tournament.getName());

            bindingResult.rejectValue(
                    "endDate",
                    "tournament.endDate.beforeStartDate",
                    "Das Enddatum darf nicht vor dem Startdatum liegen."
            );
        }

        if (tournament.getRegistrationDeadline() != null
                && tournament.getStartDate() != null
                && tournament.getRegistrationDeadline().isAfter(tournament.getStartDate())) {
            log.debug("Date validation failed: registration deadline is after start date for tournament '{}'.",
                    tournament.getName());

            bindingResult.rejectValue(
                    "registrationDeadline",
                    "tournament.registrationDeadline.afterStartDate",
                    "Die Anmeldefrist darf nicht nach dem Startdatum liegen."
            );
        }
    }

    /**
     * Normalizes and validates the requested round number.
     *
     * <p>If no round is provided, round 1 is used as default.</p>
     *
     * @param round      requested round number, may be {@code null}
     * @param tournament tournament entity
     * @return validated round number
     */
    private int normalizeRound(Integer round, Tournament tournament) {
        Integer numberOfRounds = tournament.getNumberOfRounds();

        if (numberOfRounds == null || numberOfRounds < 1) {
            log.warn("TournamentId={} has invalid numberOfRounds={}.",
                    tournament.getId(), numberOfRounds);

            throw new IllegalArgumentException("Für dieses Turnier ist keine gültige Rundenzahl definiert.");
        }

        if (round == null) {
            return 1;
        }

        if (round < 1 || round > numberOfRounds) {
            log.warn("Invalid round {} requested for tournamentId={} with numberOfRounds={}.",
                    round, tournament.getId(), numberOfRounds);

            throw new IllegalArgumentException(
                    "Die angeforderte Runde " + round
                            + " ist ungültig. Erlaubt sind nur Runden von 1 bis " + numberOfRounds + "."
            );
        }

        return round;
    }

    /**
     * Populates common form attributes for the tournament form view.
     *
     * @param model  Spring MVC model
     * @param isEdit whether the form is in edit mode
     */
    private void populateTournamentFormModel(Model model, boolean isEdit) {
        model.addAttribute("isEdit", isEdit);
        model.addAttribute("statuses", TournamentStatus.values());
    }

    /**
     * Populates the model for start list views used by admin and public pages.
     *
     * @param model         Spring MVC model
     * @param tournament    tournament entity
     * @param selectedRound validated selected round
     * @param tournamentId  tournament id
     */
    private void populateStartListModel(Model model,
                                        Tournament tournament,
                                        int selectedRound,
                                        Long tournamentId) {
        model.addAttribute("tournament", tournament);
        model.addAttribute("selectedRound", selectedRound);
        model.addAttribute(
                "availableRounds",
                IntStream.rangeClosed(1, tournament.getNumberOfRounds()).boxed().toList()
        );
        model.addAttribute(
                "registrations",
                registrationService.findStartListByTournamentIdAndRound(tournamentId, selectedRound)
        );
    }
}