# Reorderable Launcher App

**Reorderable Launcher** is a streamlined and intuitive launcher application crafted using
**[Kotlin](https://github.com/JetBrains/kotlin)** and
**[Jetpack Compose](https://developer.android.com/jetpack/compose)**. Designed for ease of use, it
intelligently arranges new apps to fill the available spaces on each page, ensuring a clean and
organized home screen.

## 🔥 Motivation

I started this project at my company's request to integrate user apps and other launcher features,
as the WebView team already handles the home feature for both Android and iOS. I decided to continue
the project on my own, focusing on Jetpack Compose, Pager, and PointerInput.

## 🌌 Progress & Next Steps

Milestone 1: Successfully tackled the dynamic positioning of new and moved apps, allowing the launcher to reflect changes from RoomDB and adjust app positions across both the UI and data layers asynchronously.

Next Goal: Streamline state and data updates in the ViewModel to make them more reactive, eliminating the need to manually stop and restart Flow collections.

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

- **Persistent Storage**: Stores app position indexes and page indexes in Room DB.

- **Reactive Updates**: Reflects real-time changes in app positions and updates both the data and
  presentation layers asynchronously.

## 🎥 Demo

[screen-20240701-234344.webm](https://github.com/neronguyenvn/custom-launcher/assets/50225603/9f62c7dd-2749-46d7-bed0-b7659cafa333)
