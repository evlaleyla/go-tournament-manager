package com.evlaleyla.gotournamentmanager.backend.macmahon;

import com.evlaleyla.gotournamentmanager.backend.tournament.Tournament;
import com.evlaleyla.gotournamentmanager.backend.tournament.TournamentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service responsible for managing imported tournament standing data
 * originating from MacMahon wall-list files.
 *
 * <p>The service supports three main use cases:</p>
 * <ul>
 *     <li>loading persisted standings for a tournament,</li>
 *     <li>importing and replacing the current wall-list,</li>
 *     <li>publishing and unpublishing the imported wall-list.</li>
 * </ul>
 */
@Service
public class TournamentStandingService {

    private static final Logger log = LoggerFactory.getLogger(TournamentStandingService.class);

    private final TournamentService tournamentService;
    private final TournamentStandingRepository tournamentStandingRepository;
    private final MacMahonInterfaceService macMahonInterfaceService;

    public TournamentStandingService(TournamentService tournamentService,
                                     TournamentStandingRepository tournamentStandingRepository,
                                     MacMahonInterfaceService macMahonInterfaceService) {
        this.tournamentService = tournamentService;
        this.tournamentStandingRepository = tournamentStandingRepository;
        this.macMahonInterfaceService = macMahonInterfaceService;
    }

    /**
     * Loads all persisted standing entries for the given tournament ordered by place.
     *
     * @param tournamentId the technical id of the tournament
     * @return ordered list of standing entries for the tournament
     */
    public List<TournamentStanding> findByTournamentId(Long tournamentId) {
        log.debug("Loading standings for tournamentId={}", tournamentId);

        List<TournamentStanding> standings =
                tournamentStandingRepository.findByTournamentIdOrderByPlaceAsc(tournamentId);

        log.debug("Loaded {} standing entries for tournamentId={}", standings.size(), tournamentId);
        return standings;
    }

    /**
     * Imports the current MacMahon wall-list for a tournament.
     *
     * <p>The import fully replaces previously persisted standing data for the tournament.
     * After a successful import, the wall-list is automatically marked as unpublished
     * so that newly imported data is not exposed publicly without an explicit publish action.</p>
     *
     * @param tournamentId the technical id of the tournament
     * @param file         the uploaded MacMahon wall-list file
     */
    @Transactional
    public void importCurrentWallList(Long tournamentId, MultipartFile file) {
        log.info("Starting wall-list import for tournamentId={}", tournamentId);

        Tournament tournament = tournamentService.findById(tournamentId);
        List<MacMahonWallListEntry> entries = macMahonInterfaceService.parseWallList(file);

        log.debug("Parsed {} wall-list entries for tournamentId={}", entries.size(), tournamentId);

        tournamentStandingRepository.deleteByTournamentId(tournamentId);
        log.debug("Deleted existing standing entries for tournamentId={}", tournamentId);

        List<TournamentStanding> standings = entries.stream()
                .map(entry -> mapToEntity(tournament, entry))
                .toList();

        tournamentStandingRepository.saveAll(standings);
        log.debug("Saved {} standing entries for tournamentId={}", standings.size(), tournamentId);

        tournament.setLastWallListImportAt(LocalDateTime.now());
        tournament.setWallListPublished(false);
        tournament.setWallListPublishedAt(null);
        tournamentService.save(tournament);

        log.info(
                "Successfully imported wall-list for tournamentId={} and reset publication status",
                tournamentId
        );
    }

    /**
     * Publishes the currently imported wall-list of a tournament.
     *
     * @param tournamentId the technical id of the tournament
     * @throws IllegalArgumentException if no wall-list has been imported yet
     */
    @Transactional
    public void publishWallList(Long tournamentId) {
        log.info("Publishing wall-list for tournamentId={}", tournamentId);

        Tournament tournament = tournamentService.findById(tournamentId);
        List<TournamentStanding> standings =
                tournamentStandingRepository.findByTournamentIdOrderByPlaceAsc(tournamentId);

        if (standings.isEmpty()) {
            log.warn("Wall-list publication rejected because no standings exist for tournamentId={}", tournamentId);
            throw new IllegalArgumentException(
                    "Für dieses Turnier wurde noch keine Wall-List importiert."
            );
        }

        tournament.setWallListPublished(true);
        tournament.setWallListPublishedAt(LocalDateTime.now());
        tournamentService.save(tournament);

        log.info("Wall-list published for tournamentId={}", tournamentId);
    }

    /**
     * Removes the public visibility of the current wall-list of a tournament.
     *
     * @param tournamentId the technical id of the tournament
     * @throws IllegalArgumentException if the wall-list is currently not published
     */
    @Transactional
    public void unpublishWallList(Long tournamentId) {
        log.info("Unpublishing wall-list for tournamentId={}", tournamentId);

        Tournament tournament = tournamentService.findById(tournamentId);

        if (!tournament.isWallListPublished()) {
            log.warn(
                    "Wall-list unpublish rejected because it is already unpublished for tournamentId={}",
                    tournamentId
            );
            throw new IllegalArgumentException(
                    "Die Wall-List ist aktuell nicht veröffentlicht."
            );
        }

        tournament.setWallListPublished(false);
        tournament.setWallListPublishedAt(null);
        tournamentService.save(tournament);

        log.info("Wall-list unpublished for tournamentId={}", tournamentId);
    }

    /**
     * Maps one parsed MacMahon wall-list entry to a persistable standing entity.
     *
     * @param tournament the owning tournament
     * @param entry      the parsed wall-list entry
     * @return the mapped standing entity
     */
    private TournamentStanding mapToEntity(Tournament tournament, MacMahonWallListEntry entry) {
        TournamentStanding standing = new TournamentStanding();
        standing.setTournament(tournament);
        standing.setPlace(entry.getPlace());
        standing.setName(entry.getName());
        standing.setClub(entry.getClub());
        standing.setLevel(entry.getLevel());
        standing.setScore(entry.getScore());
        standing.setRoundStatuses(entry.getRoundStatuses());
        standing.setPoints(entry.getPoints());
        standing.setScoreX(entry.getScoreX());
        standing.setSos(entry.getSos());
        standing.setSosos(entry.getSosos());
        return standing;
    }
}