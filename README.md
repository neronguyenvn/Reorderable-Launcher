# Reorderable Launcher App

**Reorderable Launcher** is a streamlined and intuitive launcher application crafted using
**[Kotlin](https://github.com/JetBrains/kotlin)** and
**[Jetpack Compose](https://developer.android.com/jetpack/compose)** Designed for ease of use, it
intelligently arranges new apps to fill the available spaces on each page, ensuring a clean and
organized home screen.

## 🔥 Motivation

I started this project at my company's request to integrate user apps and other launcher features,
as the WebView team already handles the home feature for both Android and iOS. I decided to continue
the project on my own, focusing on Jetpack Compose, Pager, and PointerInput.

Overcoming the challenge of positioning new and moved apps, the launcher now dynamically reflects
changes from RoomDB, updates app positions in UI layer and in Data layer asynchronously. The goal is
to preload the pager to preload the main page as well as the adjacent pages.

## 🛠️ Technologies

- **UI**: Built with **[Jetpack Compose](https://developer.android.com/jetpack/compose)** and
  **[Coil](https://github.com/coil-kt/coil)** for image loading. Memory cache images so don't have
  laggy when switch pages.

- **Architecture**: MVVM Pattern, apply Clean Architecture principles, Reactive pattern.

- **Dependency Injection**: Powered by
  **[Hilt](https://developer.android.com/training/dependency-injection/hilt-android)**.

- **Storage**: Implements **[Room](https://github.com/realm)** for store app item info and its
  position.

## 📋 Features

- **Dynamic Grid Layout**: Automatically arranges apps in a grid layout, allowing for easy
  drag-and-drop reordering within and across pages.

- **App Management**: Efficiently handles the addition and removal of apps, as well as
  notifications.

- **Persistent Storage**: Stores app position indexes and page indexes in Room DB, enabling smooth
  transitions and movements of apps between pages.

- **Reactive Updates**: Reflects real-time changes in app positions and updates both the data and
  presentation layers asynchronously.

## 🎥 Demo
