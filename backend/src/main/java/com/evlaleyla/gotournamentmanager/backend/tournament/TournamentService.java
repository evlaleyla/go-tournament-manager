package com.evlaleyla.gotournamentmanager.backend.tournament;

import com.evlaleyla.gotournamentmanager.backend.pairing.PairingRepository;
import com.evlaleyla.gotournamentmanager.backend.registration.RegistrationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TournamentService {

    private final TournamentRepository tournamentRepository;
    private final RegistrationRepository registrationRepository;
    private final PairingRepository pairingRepository;

    public TournamentService(TournamentRepository tournamentRepository,
                             RegistrationRepository registrationRepository,
                             PairingRepository pairingRepository) {
        this.tournamentRepository = tournamentRepository;
        this.registrationRepository = registrationRepository;
        this.pairingRepository = pairingRepository;
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
        existingTournament.setRegistrationDeadline(updatedTournament.getRegistrationDeadline());

        return tournamentRepository.save(existingTournament);
    }

    public List<Tournament> search(String search, TournamentStatus status) {
        boolean hasSearch = search != null && !search.isBlank();
        boolean hasStatus = status != null;

        if (hasSearch && hasStatus) {
            return tournamentRepository.findByNameContainingIgnoreCaseAndStatus(search, status);
        }

        if (hasSearch) {
            return tournamentRepository.findByNameContainingIgnoreCase(search);
        }

        if (hasStatus) {
            return tournamentRepository.findByStatus(status);
        }

        return tournamentRepository.findAll();
    }

    public List<String> findDistinctNames() {
        return tournamentRepository.findDistinctNames();
    }

    @Transactional
    public void deleteById(Long id) {
        pairingRepository.deleteByTournamentId(id);
        registrationRepository.deleteByTournamentId(id);
        tournamentRepository.deleteById(id);
    }
}