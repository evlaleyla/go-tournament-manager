# Data Model

The prototype uses a relational data model through Spring Data JPA. The central entities are tournaments, participants, registrations, pairings and tournament standings.

## Main entities

### Tournament

Represents a Go tournament.

Important fields:

- `id`
- `name`
- `location`
- `startDate`
- `endDate`
- `registrationDeadline`
- `numberOfRounds`
- `description`
- `status`
- `lastWallListImportAt`
- `wallListPublished`
- `wallListPublishedAt`

A tournament can have many registrations, pairings and standing entries.

### Participant

Represents a person who can participate in one or more tournaments.

Important fields:

- `id`
- `firstName`
- `lastName`
- `email`
- `club`
- `country`
- `rank`
- `birthDate`

The email address is unique. Participant data is normalized and validated before persistence.

### Registration

Represents the registration of one participant for one tournament.

Important fields:

- `id`
- `tournament`
- `participant`
- `registrationDate`
- `selectedRounds`
- `rankAtRegistration`
- `clubAtRegistration`
- `countryAtRegistration`
- `notes`

The combination of tournament and participant is unique.  
The selected rounds define in which rounds the participant is eligible to play.

### Pairing

Represents one pairing in a tournament round.

Important fields:

- `id`
- `tournament`
- `roundNumber`
- `tableNumber`
- `blackPlayer`
- `whitePlayer`
- `result`
- `handicap`
- `bye`
- `published`

Pairings are unique per tournament, round and table.  
The `published` flag controls whether a pairing is visible in the public area.

Supported internal result codes:

| Code | Meaning |
|---|---|
| `B` | Black wins |
| `W` | White wins |
| `J` | Jigo |
| `L` | Both lose |
| `D` | Both win |
| `null` | Result open |

### TournamentStanding

Represents one imported standing row from a MacMahon wall list.

Important fields:

- `id`
- `tournament`
- `place`
- `name`
- `club`
- `level`
- `score`
- `roundStatusesSerialized`
- `points`
- `scoreX`
- `sos`
- `sosos`

Round status values are stored as a serialized string and exposed as a list for rendering.

## Relationships

- One tournament has many registrations.
- One participant can have many registrations.
- One tournament has many pairings.
- One tournament has many standing entries.
- Pairings and standings store participant names as imported from MacMahon, because MacMahon exchange is name-based.

## Data consistency rules

The prototype includes several safeguards:

- A participant cannot be deleted while registrations exist.
- A participant name cannot be changed if the name is already referenced in imported pairings or standings.
- A registration cannot be deleted if imported pairings or standings reference the registered participant in the tournament.
- The number of tournament rounds cannot be reduced if registrations, pairings or wall-list data already reference removed rounds.
- MacMahon imports validate round numbers, duplicate tables and participant eligibility.
