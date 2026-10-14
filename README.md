# Quizzy

Quizzy is a fully offline Android app for creating and practicing your own quizzes. Questions and answer notes are stored on the device in a local SQLite database. The app does not request network access.

## Features

- Add, edit, and delete questions with four answer choices and a note for each choice.
- The first answer entered is the correct answer; the app shuffles choices only during practice.
- Practice shuffles question order, marks the correct answer green and an incorrect selection red, and reveals notes for all four choices.
- The home screen shows each complete question and its correct answer.

## Open and run

Open this folder in Android Studio with Android SDK Platform 35 installed, then run the `app` configuration on an emulator or Android device. The project uses Android Gradle Plugin 8.9.2 and supports Android 6.0 (API 23) and newer. Runtime features work offline.
