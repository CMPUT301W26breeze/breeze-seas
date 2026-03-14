# Breeze-Seas

![Android](https://img.shields.io/badge/platform-Android-green)
![Language](https://img.shields.io/badge/language-Java-blue)
![Backend](https://img.shields.io/badge/backend-Firebase-orange)
![Course](https://img.shields.io/badge/course-CMPUT%20301-lightgrey)
![Status](https://img.shields.io/badge/status-Active%20Development-brightgreen)

**Breeze-Seas** is an event management Android application that allows users to discover events, join waiting lists, and participate in a fair lottery-based event registration system.

Developed for **CMPUT 301 – Introduction to Software Engineering** at the **University of Alberta**.

---

## Features

### Entrant
- Browse available events
- Join or leave waiting lists
- Accept or decline invitations
- View ticket history

### Organizer
- Create events with QR codes
- Manage event waiting lists
- Run lottery selections
- Draw replacement entrants

### Administrator
- Browse all events
- Monitor system logs
- Review notifications

---

## 👥 Team Members

| Name | CCID | GitHub |
|-----|------|--------|
| **Abhinav Bhattarai** | bhattar5 | [abhinav-bhattarai8](https://github.com/abhinav-bhattarai8) |
| **Ahmed Jama** | aajama | [ajama475](https://github.com/ajama475) |
| **Alan Yu** | ayu8 | [itsraindi](https://github.com/itsraindi) |
| **Bryant Liu** | byliu | [Vaiom](https://github.com/Vaiom) |
| **Guang Hua Liang** | guanghua | [ualbertagit123](https://github.com/ualbertagit123) |
| **Osman Akman** | oakman | [MoonTzu76](https://github.com/MoonTzu76) |

---
## Project Structure

```text
code/
├── app/
│   ├── src/main/java/com/example/breeze_seas/
│   │   ├── activities/
│   │   │   └── MainActivity.java
│   │   ├── fragments/
│   │   │   ├── entrant/
│   │   │   │   ├── WelcomeScreenFragment.java
│   │   │   │   ├── SignUpFragment.java
│   │   │   │   ├── ExploreFragment.java
│   │   │   │   ├── EventDetailsFragment.java
│   │   │   │   ├── TicketsFragment.java
│   │   │   │   ├── ActiveTicketsFragment.java
│   │   │   │   ├── AttendingTicketsFragment.java
│   │   │   │   ├── PastTicketsFragment.java
│   │   │   │   ├── NotificationFragment.java
│   │   │   │   └── ProfileFragment.java
│   │   │   ├── organizer/
│   │   │   │   ├── OrganizeFragment.java
│   │   │   │   ├── CreateEventFragment.java
│   │   │   │   ├── FilterFragment.java
│   │   │   │   ├── OrganizerEventPreviewFragment.java
│   │   │   │   ├── OrganizerListHostFragment.java
│   │   │   │   ├── WaitingListFragment.java
│   │   │   │   ├── PendingListFragment.java
│   │   │   │   ├── AcceptedListFragment.java
│   │   │   │   ├── DeclinedListFragment.java
│   │   │   │   └── ViewQrCodeFragment.java
│   │   │   └── admin/
│   │   │       ├── AdminDashboardFragment.java
│   │   │       ├── AdminBrowseEventsFragment.java
│   │   │       ├── AdminBrowseProfilesFragment.java
│   │   │       ├── AdminBrowseImagesFragment.java
│   │   │       └── AdminBrowseLogsFragment.java
│   │   ├── adapters/
│   │   │   ├── ExploreEventViewAdapter.java
│   │   │   ├── ActiveTicketsAdapter.java
│   │   │   ├── AttendingTicketsAdapter.java
│   │   │   ├── PastTicketsAdapter.java
│   │   │   ├── OrganizerListAdapter.java
│   │   │   ├── OrganizerPagerAdapter.java
│   │   │   └── NotificationEntryAdapter.java
│   │   ├── data/
│   │   │   ├── DBConnector.java
│   │   │   ├── FirebaseSession.java
│   │   │   ├── UserDB.java
│   │   │   ├── EventDB.java
│   │   │   ├── TicketDB.java
│   │   │   ├── WaitingList.java
│   │   │   ├── PendingList.java
│   │   │   ├── AcceptedList.java
│   │   │   ├── DeclinedList.java
│   │   │   ├── Lottery.java
│   │   │   └── NotificationService.java
│   │   ├── models/
│   │   │   ├── User.java
│   │   │   ├── Event.java
│   │   │   ├── Notification.java
│   │   │   ├── TicketUIModel.java
│   │   │   ├── AttendingTicketUIModel.java
│   │   │   └── PastEventUIModel.java
│   │   └── shared/
│   │       ├── SessionViewModel.java
│   │       ├── TicketTabMapper.java
│   │       ├── EventMutationCallback.java
│   │       └── NonNull.java
│   └── src/main/res/
│       ├── layout/
│       ├── drawable/
│       ├── color/
│       ├── values/
│       ├── menu/
│       ├── font/
│       ├── mipmap-*/
│       └── xml/
```
