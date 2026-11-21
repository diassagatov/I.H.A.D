# 🌙 Dream Journal Android App

A clean, modern **Dream Journal** Android application built using **Jetpack Compose**, **Navigation Compose**, **ViewModel**, **StateFlow**, and **Room Database**, strictly adhering to the **MVVM architecture**.

---

## 🏗️ Architecture & Project Structure

All core components are located in the `com.example.dreamjournal` package:

```text
app/src/main/java/com/example/dreamjournal/
├── DreamModel.kt       # Data entities (DreamEntity) & UI state definitions (DreamUiState)
├── DreamDatabase.kt    # Room DAO, Database instance, and Repository layer
├── DreamViewModel.kt   # ViewModel handling business logic, form validation, & StateFlow updates
├── Screens.kt          # UI Screens (List, Detail, Form) & reusable components
└── MainActivity.kt     # App entry point, Navigation Host, & state collection
```

---

## 🔑 Key Components Explained

### 1. Data & Storage Layer (`DreamModel.kt` & `DreamDatabase.kt`)
* **`DreamEntity`**: Represents a recorded dream in Room DB with fields:
  * `id`: String (generated using `UUID.randomUUID().toString()`)
  * `title`: String
  * `description`: String
  * `interpretation`: String
  * `status`: String (`"Draft"`, `"Analyzed"`, or `"Lucid"`)
  * `createdAt`: Long timestamp
* **`DreamDao`**: Room Data Access Object providing reactive `Flow<List<DreamEntity>>` and `Flow<DreamEntity?>` streams alongside full CRUD operations.
* **`DreamRepository`**: Wraps DAO calls to decouple data sources from the UI logic.

### 2. State & Business Logic (`DreamViewModel.kt`)
* **State Flow**: Exposes an immutable `StateFlow<DreamUiState>` using `_uiState.update { it.copy(...) }`.
* **Business Rules**:
  * **Status Progression Rule**: Moving a dream status to `"Analyzed"` requires a non-blank `interpretation`. If empty, the status change is blocked and an error message is emitted.
  * **Input Validation**: Ensures `title` is at least 3 characters and `description` is at least 10 characters long, displaying inline error messages in the form.
  * **Undo Delete**: Deleting a dream stores it temporarily in `uiState.lastDeletedDream`. Triggering `undoDelete()` re-inserts the dream into Room.

### 3. UI Layer (`Screens.kt` & `MainActivity.kt`)
* **`DreamListScreen`**:
  * Displays a top summary bar with counts for Total, Analyzed, and Lucid dreams.
  * Shows an empty state graphic/message when no dreams are recorded.
  * Renders a `LazyColumn` with stable keys (`key = { it.id }`) containing dream cards with status badges.
* **`DreamDetailScreen`**:
  * Shows complete dream details, timestamps, and interpretation notes.
  * Features quick-action status chips and edit/delete buttons.
  * Triggers an `AlertDialog` for delete confirmation.
* **`DreamFormScreen`**:
  * Handles both creating new dreams and editing existing ones.
  * Provides state-hoisted `OutlinedTextField` inputs with validation feedback.
* **`MainActivity.kt` & Navigation**:
  * Defines type-safe routing via `sealed class Screen(val route: String)`.
  * Collects state safely using `collectAsStateWithLifecycle()`.

---

## ⚡ Features & Workflows

1. **Full CRUD Operations**: Create, view, edit, and delete dreams with full Room database persistence across app restarts and orientation changes.
2. **Status Workflow**: Filter and update statuses across `"Draft"`, `"Analyzed"`, and `"Lucid"`.
3. **Delete & Undo**: Confirmation dialog prevents accidental deletion, with an interactive Snackbar allowing immediate "Undo".
4. **Reactive Sync**: Room `Flow` updates propagate directly through `StateFlow` to ensure screens are automatically synchronized.

---

## 🛠️ Build & Run

1. Open the project in **Android Studio**.
2. Sync Gradle files (`Gradle Sync`).
3. Build and run on an Android device or emulator (Android 7.0 / API 24+).
