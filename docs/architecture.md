# Architecture

The prototype is implemented as a Java web application using Spring Boot, Spring MVC, Thymeleaf and Spring Data JPA.

The application follows a layered structure:

- Controllers handle HTTP requests and prepare Thymeleaf views.
- Services contain business logic, validation and workflow coordination.
- Repositories provide database access through Spring Data JPA.
- Entities represent persistent tournament, participant, registration, pairing and standing data.
- Thymeleaf templates render the administrative and public user interface.

## Main modules

### Tournament management

Handles tournament creation, editing, filtering, deletion and public tournament display.

Main classes:

- `Tournament`
- `TournamentStatus`
- `TournamentController`
- `TournamentService`
- `TournamentRepository`
- `TournamentDashboardService`
- `TournamentDashboardData`

### Participant management

Handles participant master data, search and filtering, validation of country, club and rank information, and deletion restrictions when imported tournament data already references a participant.

Main classes:

- `Participant`
- `ParticipantController`
- `ParticipantService`
- `ParticipantRepository`

### Registration management

Handles internal registrations and public self-registration. Registrations store a snapshot of participant-related data such as rank, club and country at the time of registration. They also store the selected rounds in which a participant intends to play.

Main classes:

- `Registration`
- `RegistrationForm`
- `SelfRegistrationForm`
- `RegistrationController`
- `RegistrationService`
- `RegistrationRepository`

### Pairing management

Handles imported pairings, result editing, publication and public display. Pairings are imported from MacMahon export files and stored per tournament, round and table.

Main classes:

- `Pairing`
- `PairingController`
- `PairingService`
- `PairingRepository`
- `PairingResultForm`

### MacMahon integration

Provides import and export functionality for interaction with the existing MacMahon software.

Implemented functions include:

- export of registered participants into a MacMahon-compatible text format,
- import of round pairings from MacMahon files,
- import of MacMahon wall-list files,
- publication and unpublication of pairings and standings.

Main classes:

- `MacMahonExportService`
- `MacMahonInterfaceService`
- `MacMahonController`
- `MacMahonPairingImportRow`
- `MacMahonWallListEntry`
- `TournamentStanding`
- `TournamentStandingService`
- `TournamentStandingRepository`

### Shared support classes

Utility and infrastructure classes provide option lists, validation helpers, exception handling and reference checks.

Main classes:

- `CountryOptions`
- `CountryOption`
- `ClubOptions`
- `RankOptions`
- `TournamentDataReferenceService`
- `GlobalExceptionHandler`
- `HomeController`

## User interface

The user interface is rendered server-side with Thymeleaf. It consists of two areas:

- an administrative area for organizers,
- a public area for participants and viewers.

The public area provides tournament information, online registration, start lists, published pairings and published standings.  
The administrative area provides tournament, participant and registration management, MacMahon import/export, result editing and controlled publication.
