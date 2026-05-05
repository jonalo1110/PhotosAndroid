# Photos Android

Android port of the JavaFX Photos assignment. The app is written in Java with Android XML layouts and uses local device storage for album/photo metadata.

## Features

- Home screen loads saved albums from the previous session.
- Create, open, rename, and delete albums.
- Add photos from Android storage using the system picker.
- Display album photos with thumbnails.
- Remove photos from albums.
- Display a photo full screen with manual previous/next slideshow controls.
- Add and delete `person` and `location` tags.
- Move a photo from one album to another.
- Search photos across all albums by case-insensitive tag-value prefixes.
- Supports one or two search predicates with AND/OR.
- Provides autocomplete suggestions from existing tag values.


## GenAI Use

I used OpenAI as a coding assistant while working through the Android version of the Photos project. I provided screenshots of the assignment requirements so the GenAI tool could help me organize the required features into smaller implementation steps. I then used it in stages to help scaffold the Android Studio project and refine separate functional pieces such as album management, photo display, tagging, moving photos, searching, persistence, and build verification.

Prompts/refinements used:

- Organized the assignment requirements into a checklist of Android features.
- Created the base Android Studio project structure with Kotlin Gradle DSL, Java source folders, XML layout folders, and app resources.
- Built the home screen XML and Java logic for loading, creating, renaming, opening, and deleting albums.
- Built the album screen XML and Java logic for listing photo thumbnails, adding photos from device storage, removing photos, and moving photos to another album.
- Built the photo display screen XML and Java logic for showing the selected photo, navigating previous/next photos manually, and showing tags.
- Added Java model classes for albums, photos, and tags.
- Added local persistence using JSON saved through `SharedPreferences`, then refined the code so saved albums/photos reload when the app starts.
- Added tag editing for only the required tag types: `person` and `location`.
- Added search across all albums, including case-insensitive matching, AND/OR search, and autocomplete suggestions from existing tag values.
- Ran Gradle builds, fixed build setup issues such as the SDK path in `local.properties`, and verified that `./gradlew assembleDebug` succeeds.

Reference materials provided:

- Screenshots of the assignment requirements were provided for reference while breaking the project into smaller implementation tasks.

AI-generated components:

- Portions of the Android project scaffold using Kotlin Gradle DSL.
- Draft XML layouts for the home, album, photo display, search, album row, photo row, and tag row screens.
- Draft Java model classes for albums, photos, and tags.
- Draft JSON persistence code using `SharedPreferences`.
- Draft Java activity logic for navigation and feature workflows.

Student-written/manual components:

- Reviewed the assignment requirements and checked that the implementation matched the required feature list.
- Chose the Java/XML Android approach required by the assignment.
- Reviewed and adjusted generated code so the different screens and components worked together as one app.
- Ran the Gradle build and verified that the debug APK builds successfully.
- Performed or should perform final manual emulator testing before submission.
