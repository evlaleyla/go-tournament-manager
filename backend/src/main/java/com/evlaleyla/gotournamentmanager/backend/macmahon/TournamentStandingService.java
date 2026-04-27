package com.evlaleyla.gotournamentmanager.backend.macmahon;

import com.evlaleyla.gotournamentmanager.backend.tournament.Tournament;
import com.evlaleyla.gotournamentmanager.backend.tournament.TournamentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TournamentStandingService {

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

    public List<TournamentStanding> findByTournamentId(Long tournamentId) {
        return tournamentStandingRepository.findByTournamentIdOrderByPlaceAsc(tournamentId);
    }

    @Transactional
    public void importCurrentWallList(Long tournamentId, MultipartFile file) {
        Tournament tournament = tournamentService.findById(tournamentId);

        List<MacMahonWallListEntry> entries = macMahonInterfaceService.parseWallList(file);

        tournamentStandingRepository.deleteByTournamentId(tournamentId);

        List<TournamentStanding> standings = entries.stream()
                .map(entry -> mapToEntity(tournament, entry))
                .toList();

        tournamentStandingRepository.saveAll(standings);

        tournament.setLastWallListImportAt(LocalDateTime.now());
        tournament.setWallListPublished(false);
        tournament.setWallListPublishedAt(null);
        tournamentService.save(tournament);
    }

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

    @Transactional
    public void publishWallList(Long tournamentId) {
        Tournament tournament = tournamentService.findById(tournamentId);

        List<TournamentStanding> standings = tournamentStandingRepository.findByTournamentIdOrderByPlaceAsc(tournamentId);

        if (standings.isEmpty()) {
            throw new IllegalArgumentException(
                    "Für dieses Turnier wurde noch keine Wall-List importiert."
            );
        }

        tournament.setWallListPublished(true);
        tournament.setWallListPublishedAt(LocalDateTime.now());
        tournamentService.save(tournament);
    }

    @Transactional
    public void unpublishWallList(Long tournamentId) {
        Tournament tournament = tournamentService.findById(tournamentId);

        if (!tournament.isWallListPublished()) {
            throw new IllegalArgumentException(
                    "Die Wall-List ist aktuell nicht veröffentlicht."
            );
        }

        tournament.setWallListPublished(false);
        tournament.setWallListPublishedAt(null);
        tournamentService.save(tournament);
    }
}