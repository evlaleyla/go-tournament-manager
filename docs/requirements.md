# Requirements

This document summarizes the main requirements addressed by the prototype of the Go Tournament Manager.  
The requirements are derived from the empirical survey and the prioritization described in the thesis.

## Scope

The prototype focuses on the administrative support of Go tournaments. It is not intended to replace existing pairing software. Instead, it complements established tools such as MacMahon by providing web-based functions for registration, participant management, start lists, publication of tournament information, and file-based data exchange.

## Must-have requirements

The following requirements form the core scope of the prototype:

- Public online registration for tournaments
- Central management of participants
- Administrative management of tournament registrations
- Round-specific selection of participation
- Display of pairings
- Display of results
- Display of standings
- Import and export of data for external pairing software
- Clear and intuitive user interface
- Structured presentation of tournament information
- Controlled publication of pairings and standings

## Should-have requirements

The following requirements are important for practical tournament organization, but are secondary to the core workflow:

- Automatic generation of round-specific start lists
- Print-friendly start lists
- Print-friendly pairing lists
- Search and filter functions in administrative views
- Professional and consistent visual appearance

## Could-have requirements

The following requirements are relevant extensions, but are not part of the first prototype scope:

- Automatic confirmation messages after registration
- Result entry by participants
- Multilingual user interface
- Extended e-mail communication
- Reporting support for associations or rating databases
- Consent documentation
- Waiting list management

## Won't-have requirements for the prototype

The following requirements are considered relevant for future development, but are deliberately excluded from the prototype:

- Fully integrated proprietary pairing algorithm
- Accommodation management
- Catering management
- Venue and room management
- Volunteer coordination
- Transport and logistics management
- Advanced backup mechanisms
- Offline functionality
- Freely configurable data fields
- Full authentication and role-based access control

## Implemented functional areas

The prototype implements the following functional areas:

- Tournament management
- Participant management
- Registration management
- Public online registration
- Round-specific start lists
- MacMahon-compatible start list export
- MacMahon pairing import
- Pairing result management
- Round-based publication and unpublication of pairings
- MacMahon wall-list import
- Publication and unpublication of standings
- Public tournament overview
- Public tournament detail pages
- Public display of start lists, pairings, results, and standings

## Limitations

The prototype does not implement its own pairing system. Pairings are created externally in MacMahon and imported into the application.

The prototype also does not include participant accounts, role-based access control, participant-based result entry, payment handling, full multilingual support, or extended event management functions.

These limitations reflect the deliberately focused scope of the first prototype version described in the thesis.
