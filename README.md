# Balungpisah Android

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Kotlin](https://img.shields.io/badge/kotlin-2.1.0-purple.svg)](https://kotlinlang.org/)

Balungpisah is an open civic platform designed to surface real public issues and make government accountability visible and traceable. This repository contains the **Android client**, built with pure Kotlin.

## About the App
The Android app provides a mobile-first experience for:
* **Reporting:** Instant submission of public issues with photos and location.
* **Tracking:** A timeline of government responses and community context.
* **Resolution:** A mechanism for the affected public to vote on whether a problem is truly "Solved."

## Tech Stack
* **Language:** 100% Kotlin
* **UI:** Jetpack Compose
* **Architecture:** MVVM + Clean Architecture
* **Networking:** Retrofit / Ktor
* **Dependency Injection:** Hilt / Koin

## Getting Started
1. **Clone the repository:**
   `git clone https://github.com/balungpisah/balungpisah-android.git`
2. **Open in Android Studio:** Use the latest stable version.
3. **SDK Configuration:** Copy `local.properties.sample` to `local.properties` and add set your sdk path.
4. **App Configuration:** Copy `util/AppConfig.example` to `util/AppConfig.kt` and add your environments.
4. **Backend:** This app connects to the [Balungpisah Core](https://github.com/balungpisah/balungpisah-core).

## Contributing
We welcome software engineers, UI/UX designers, and accessibility advocates. 
Please read our [Contribution Guidelines](CONTRIBUTING.md) before submitting a Pull Request.

## Licensing
This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---
*Government reports progress. Citizens decide resolution.*