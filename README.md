# Go Tournament Manager

Open-source web application prototype for organizing Go tournaments.

This project was developed as part of a bachelor thesis. It supports organizers with tournament administration, online registration, participant management, start lists and file-based data exchange with MacMahon.

The application does not include its own pairing algorithm. Pairings and rankings are created in MacMahon and imported into the application.

## Features

- tournament management
- participant management
- registration management
- public online registration
- round-based start lists
- MacMahon-compatible start list export as `.txt`
- import of MacMahon pairing files
- result management for imported pairings
- controlled publication of pairings and results
- import of MacMahon wall-list / ranking files
- controlled publication of rankings
- separate administration and public views

## Tech Stack

- Java 21
- Spring Boot
- Maven
- Thymeleaf
- Spring Data JPA
- H2 Database
- HTML, CSS and JavaScript

## Project Status

Early prototype / bachelor thesis project.

The prototype is not a complete production-ready system. Features such as authentication, role management, multilingual support, participant-based result entry and offline support are not included in the first version.

## Getting Started

### Requirements

- Java 21
- Maven

### Run the Application

```bash
mvn spring-boot:run
```

Then open the application in your browser:

```text
http://localhost:8080
```

## License

This project is licensed under the MIT License.

See the `LICENSE` file for details.
