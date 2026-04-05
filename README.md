# Breeze Seas

<div align="center">
<img src="https://img.shields.io/badge/platform-Android-2ea44f" alt="Platform">
<img src="https://img.shields.io/badge/language-Java%2011-0b5fff" alt="Language">
<img src="https://img.shields.io/badge/backend-Firebase-ff8c00" alt="Backend">
<img src="https://img.shields.io/badge/minSdk-24-5c6370" alt="Min SDK">
<img src="https://img.shields.io/badge/license-GPL--3.0--or--later-7a1fa2" alt="License">

<strong>Android event discovery and lottery-based registration platform</strong>

Built for CMPUT 301, Introduction to Software Engineering, at the University of Alberta


<a href="https://cmput301w26breeze.github.io/breeze-seas/">Live Documentation Site</a>
·
<a href="https://cmput301w26breeze.github.io/breeze-seas/javadoc/">API Reference</a>
·
<a href="https://github.com/CMPUT301W26breeze/breeze-seas/wiki">Project Wiki</a>
·
<a href="LICENSE">License</a>

</div>

<br>
Breeze Seas is an Android application for managing event discovery, registrations, and high-demand admissions through a fair lottery workflow. The project supports three distinct user experiences: entrants discovering and joining events, organizers managing event operations, and administrators overseeing platform-wide activity.


## Documentation

| Resource | Link |
| --- | --- |
| Documentation portal | [cmput301w26breeze.github.io/breeze-seas](https://cmput301w26breeze.github.io/breeze-seas/) |
| Javadocs | [cmput301w26breeze.github.io/breeze-seas/javadoc](https://cmput301w26breeze.github.io/breeze-seas/javadoc/) |
| Project wiki | [github.com/CMPUT301W26breeze/breeze-seas/wiki](https://github.com/CMPUT301W26breeze/breeze-seas/wiki) |
| Source repository | [github.com/CMPUT301W26breeze/breeze-seas](https://github.com/CMPUT301W26breeze/breeze-seas) |

## Product Overview

### Entrant Experience

- Browse and discover available events
- Join or leave waitlists
- Accept or decline invitations
- View active, attending, and past tickets

### Organizer Experience

- Create and manage events
- Generate and share QR codes
- Manage waitlists and invitation states
- Run lottery selections and replacement draws

### Administrator Experience

- Browse platform events and profiles
- Review uploaded images
- Monitor activity logs and notifications

## Technology Stack

- Android SDK 35 with `minSdk 24`
- Java 11
- Gradle Kotlin DSL
- Firebase Authentication and Firestore
- CameraX, ML Kit barcode scanning, and ZXing
- osmdroid and OpenCSV

## Getting Started

### Prerequisites

- Android Studio with JDK 11 support
- Android SDK 35
- A device or emulator running Android 7.0 or later
- `google-services.json` inside `code/app/` for Firebase-backed features

### Local Setup

1. Clone the repository.
2. Open `code/` in Android Studio.
3. Sync the Gradle project.
4. Add `google-services.json` to `code/app/` if you want authentication and Firestore features enabled.
5. Run the app on an emulator or physical device.

### Build and Test

Run from `code/`:

```bash
./gradlew assembleDebug
./gradlew test
./gradlew connectedAndroidTest
```

## Repository Layout

```text
breeze-seas/
├── code/
│   ├── app/
│   │   ├── src/main/java/com/example/breeze_seas/
│   │   │   ├── fragments/
│   │   │   │   ├── entrant/
│   │   │   │   ├── organizer/
│   │   │   │   └── admin/
│   │   │   ├── adapters/
│   │   │   ├── data/
│   │   │   ├── models/
│   │   │   └── shared/
│   │   └── src/main/res/
│   ├── gradlew
│   └── settings.gradle.kts
├── docs/
│   ├── index.html
│   └── javadoc/
└── README.md
```

## Team

| Name | CCID | GitHub |
| --- | --- | --- |
| Abhinav Bhattarai | `bhattar5` | [abhinav-bhattarai8](https://github.com/abhinav-bhattarai8) |
| Ahmed Jama | `aajama` | [ajama475](https://github.com/ajama475) |
| Alan Yu | `ayu8` | [itsraindi](https://github.com/itsraindi) |
| Bryant Liu | `byliu` | [Vaiom](https://github.com/Vaiom) |
| Guang Hua Liang | `guanghua` | [ualbertagit123](https://github.com/ualbertagit123) |
| Osman Akman | `oakman` | [MoonTzu76](https://github.com/MoonTzu76) |

## License

Breeze Seas is released under the GNU General Public License, version 3 or any later version (`GPL-3.0-or-later`).

See [LICENSE](LICENSE) for the repository notice and the official GNU GPL text at [gnu.org/licenses/gpl-3.0.txt](https://www.gnu.org/licenses/gpl-3.0.txt).
