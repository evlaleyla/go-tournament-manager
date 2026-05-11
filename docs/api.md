# Web Endpoints

The prototype is implemented as a server-side rendered Spring Boot MVC application using Thymeleaf.  
It does not provide a public REST API. Instead, the following web endpoints are used to support the administrative and public tournament workflow.

## Public pages

| Method | Path | Purpose |
|---|---|---|
| GET | `/` | Public landing page |
| GET | `/public/tournaments` | Public tournament overview |
| GET | `/public/tournaments/{id}` | Public tournament detail page |
| GET | `/public/tournaments/{id}/register` | Public self-registration form |
| POST | `/public/tournaments/{id}/register` | Submit public registration |
| GET | `/public/tournaments/{id}/startlist` | Public start list by round |
| GET | `/public/tournaments/{id}/pairings` | Published pairings and results |
| GET | `/public/tournaments/{id}/walllist` | Published wall list / standings |

## Administrative pages

| Method | Path | Purpose |
|---|---|---|
| GET | `/admin` | Administrative dashboard |
| GET | `/tournaments` | Tournament overview and filters |
| GET | `/tournaments/new` | Create tournament form |
| POST | `/tournaments` | Create tournament |
| GET | `/tournaments/{id}` | Tournament dashboard/detail page |
| GET | `/tournaments/{id}/edit` | Edit tournament form |
| POST | `/tournaments/{id}` | Update tournament |
| POST | `/tournaments/{id}/delete` | Delete tournament and related data |

## Participants and registrations

| Method | Path | Purpose |
|---|---|---|
| GET | `/participants` | Participant overview and filters |
| GET | `/participants/new` | Create participant form |
| POST | `/participants` | Create participant |
| GET | `/participants/{id}` | Participant detail page |
| GET | `/participants/{id}/edit` | Edit participant form |
| POST | `/participants/{id}` | Update participant |
| POST | `/participants/{id}/delete` | Delete participant |
| GET | `/registrations` | Registration overview and tournament filter |
| GET | `/registrations/new` | Create registration form |
| POST | `/registrations` | Create registration |
| GET | `/registrations/{id}` | Registration detail page |
| GET | `/registrations/{id}/edit` | Edit registration form |
| POST | `/registrations/{id}` | Update registration |
| POST | `/registrations/{id}/delete` | Delete registration |

## MacMahon import and export

| Method | Path | Purpose |
|---|---|---|
| GET | `/tournaments/{id}/startlist` | Show round-specific start list |
| GET | `/tournaments/{id}/startlist/export-macmahon` | Export tournament registrations for MacMahon |
| GET | `/tournaments/{id}/pairings` | Internal pairing overview |
| POST | `/tournaments/{id}/pairings/import-macmahon` | Import pairings from MacMahon for one round |
| POST | `/tournaments/{tournamentId}/pairings/{roundNumber}/publish` | Publish pairings of a round |
| POST | `/tournaments/{tournamentId}/pairings/{roundNumber}/unpublish` | Unpublish pairings of a round |
| POST | `/pairings/{id}/result` | Store or clear the result of one pairing |
| GET | `/tournaments/{id}/walllist` | Internal wall list administration |
| POST | `/tournaments/{id}/walllist/import-macmahon` | Import MacMahon wall list |
| POST | `/tournaments/{id}/walllist/publish` | Publish imported wall list |
| POST | `/tournaments/{id}/walllist/unpublish` | Unpublish wall list |
