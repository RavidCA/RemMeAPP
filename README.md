# Location-Based Reminder App 📍

## Overview

This project is an Android application that reminds users not to forget important items when leaving home.
The app uses the device location to detect when the user moves away from a predefined home location and sends a reminder if selected items were not taken.

The goal of the application is to help users avoid forgetting everyday items such as keys, wallet, phone, or other personal belongings.

---

## Features

### 🏠 Main Screen

The main screen displays a table containing a list of items that the user might want to take when leaving home.

On this screen the user can:

* View all available reminder items
* Select or unselect items they want the app to remind them about
* Activate the location tracking feature

If the user leaves the defined home area while selected items are still marked, the app will trigger a reminder notification.

---

### 🧾 Item Management Page

This page allows the user to manage the list of items.

The user can:

* Add new items to the reminder list
* Customize the list of objects that appear on the main screen

This makes the application flexible and adaptable to different users.

---

### ⚙️ Settings Page

The settings screen allows configuration of the location behavior.

The user can define:

* **Home Location** – the location used as the reference point
* **Alert Distance** – the distance from home that triggers the reminder

Example:

* If the alert distance is set to **200 meters**
* And the user moves only **100 meters away from home**

➡️ The application **will NOT trigger a reminder**.

A reminder is triggered **only when the user exceeds the configured distance**.

---

## How It Works

1. The user defines a **home location** in the settings page.
2. The user selects items they want to remember from the main screen.
3. The app continuously checks the device location.
4. When the user moves farther than the defined alert distance:

   * The app checks the selected items.
   * If items are still marked, a **reminder notification** is triggered.

---

## Technologies Used

* **Android Studio**
* **Java / Kotlin**
* **Android Location Services**
* **RecyclerView / Table UI**
* **Android Notifications**

--

## Possible Future Improvements

* Background location monitoring
* Push notifications
* Integration with wearable devices
* Smart suggestions based on user habits
* Cloud synchronization

