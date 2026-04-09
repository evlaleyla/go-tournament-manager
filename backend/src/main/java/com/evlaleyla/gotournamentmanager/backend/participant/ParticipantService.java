package com.evlaleyla.gotournamentmanager.backend.participant;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ParticipantService {

    private final ParticipantRepository participantRepository;

    public ParticipantService(ParticipantRepository participantRepository) {
        this.participantRepository = participantRepository;
    }

    public List<Participant> findAll() {
        return participantRepository.findAll();
    }

    public Participant save(Participant participant) {
        return participantRepository.save(participant);
    }

    public Participant findById(Long id) {
        return participantRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Participant not found: " + id));
    }

    public Participant update(Long id, Participant updatedParticipant) {
        Participant existingParticipant = findById(id);

        existingParticipant.setFirstName(updatedParticipant.getFirstName());
        existingParticipant.setLastName(updatedParticipant.getLastName());
        existingParticipant.setEmail(updatedParticipant.getEmail());
        existingParticipant.setClub(updatedParticipant.getClub());
        existingParticipant.setCountry(updatedParticipant.getCountry());
        existingParticipant.setRank(updatedParticipant.getRank());
        existingParticipant.setBirthDate(updatedParticipant.getBirthDate());

        return participantRepository.save(existingParticipant);
    }

    public void deleteById(Long id) {
        participantRepository.deleteById(id);
    }
}