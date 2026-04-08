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
        existingTournament.setDescription(updatedTournament.getDescription());
        existingTournament.setStatus(updatedTournament.getStatus());

        return tournamentRepository.save(existingTournament);
    }

    public void deleteById(Long id) {
        tournamentRepository.deleteById(id);
    }
}