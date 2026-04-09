package com.evlaleyla.gotournamentmanager.backend.tournament;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TournamentService {

    private final TournamentRepository tournamentRepository;

    public TournamentService(TournamentRepository tournamentRepository) {
        this.tournamentRepository = tournamentRepository;
    }

    public List<Tournament> findAll() {
        return tournamentRepository.findAll();
    }

    public List<String> findDistinctNames() {
        return tournamentRepository.findDistinctNames();
    }

    public List<Tournament> search(String search, TournamentStatus status) {
        boolean hasSearch = search != null && !search.isBlank();
        boolean hasStatus = status != null;

        if (hasSearch && hasStatus) {
            return tournamentRepository.findByNameContainingIgnoreCaseAndStatusOrderByStartDateAsc(search, status);
        }

        if (hasSearch) {
            return tournamentRepository.findByNameContainingIgnoreCaseOrderByStartDateAsc(search);
        }

        if (hasStatus) {
            return tournamentRepository.findByStatusOrderByStartDateAsc(status);
        }

        return tournamentRepository.findAll()
                .stream()
                .sorted((a, b) -> a.getStartDate().compareTo(b.getStartDate()))
                .toList();
    }

    public Tournament save(Tournament tournament) {
        return tournamentRepository.save(tournament);
    }

    public Tournament findById(Long id) {
        return tournamentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tournament not found: " + id));
    }

    public Tournament update(Long id, Tournament updatedTournament) {
        Tournament existingTournament = findById(id);

        existingTournament.setName(updatedTournament.getName());
        existingTournament.setLocation(updatedTournament.getLocation());
        existingTournament.setStartDate(updatedTournament.getStartDate());
        existingTournament.setEndDate(updatedTournament.getEndDate());
        existingTournament.setRegistrationDeadline(updatedTournament.getRegistrationDeadline());
        existingTournament.setDescription(updatedTournament.getDescription());
        existingTournament.setStatus(updatedTournament.getStatus());

        return tournamentRepository.save(existingTournament);
    }

    public void deleteById(Long id) {
        tournamentRepository.deleteById(id);
    }
}