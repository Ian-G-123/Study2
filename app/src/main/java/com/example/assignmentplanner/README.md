# Study² — Codebase Reference

## Project Overview
An academic productivity tool built with Kotlin and Jetpack Compose. It leverages Gemini AI for document-led analysis and integrates with Google Classroom and Google Drive to help students manage assignments, track grades, and organize study materials.

---

## Package: `com.example.assignmentplanner`

---

### `MainActivity.kt`
The application entry point. It manages theme persistence via DataStore and drives the primary navigation flow.
- **`AppRoot`** — Orchestrates the authentication gate. Shows `SignInScreen` if not authenticated.
- **`MainContainer`** — Hosts a `ModalNavigationDrawer` with condensed navigation categories:
  - **Dashboard** (`home`)
  - **Academic Planner** (`todo`, `scan`, `calendar`)
  - **Study Library** (`notes`, `study`)
  - **Grades** (`grades`)

---

### `HomeScreen.kt` (Dashboard)
The default landing page. Provides a high-level academic snapshot:
- **Greeting & GPA**: Displays user name and cumulative GPA.
- **Task Summary**: Combined count of incomplete local scanned tasks and Classroom assignments.
- **Upcoming Items**: A preview of the next three assignments due in Google Classroom.

---

### `TodoListScreen.kt` (Planner Hub)
Central hub for task management.
- **Sub-Navigation**: Direct buttons to "AI Plan from Scan" and "Calendar".
- **Task List**: Merges local AI-generated task roadmaps with live Google Classroom coursework.
- **Classroom Integration**: Fetches assignments and displays details (points, descriptions, attachments) in a full-screen dialog.

---

### `NotesScreen.kt` (Study Library Hub)
Manages personal study materials and Google Drive synchronization.
- **Sub-Navigation**: Button to launch the "AI Study Guide" generator.
- **Drive Upload**: Users specify Course, Unit, and Topic. The app creates a nested folder structure in Drive (`AssignmentPlanner/Course/Unit/Topic`) and uploads scanned JPEGs.
- **Content Summary**: Uses Gemini to generate an academic summary of the scanned notes.

---

### `GradesScreen.kt`
Academic performance tracking.
- **GPA Card**: Displays cumulative GPA calculated from active Classroom courses.
- **Course Cards**: Individual course averages and graded assignment counts.
- **What-If Calculator**: A simulator that lets users see how a hypothetical score on a new assignment would impact their total course grade.

---

### `StudyScheduleScreen.kt`
Generates structured study guides.
- **Strict Documentation**: Requires a document scan to proceed. Gemini analyzes the provided image and generates a guide based **strictly** on that content, prohibiting fabrication of outside information.

---

### `AssignmentViewModel.kt`
Handles AI analysis logic for assignment handouts.
- **`analyzeAssignment`**: Sends a bitmap to Gemini with strict instructions to generate milestones based **only** on the requirements explicitly found in the document.
- **`toggleTask`**: Manages completion states for scanned tasks.

---

### `ClassroomViewModel.kt`
Fetches and processes data from the Google Classroom API.
- **GPA Calculation**: Maps course percentages to a 4.0 scale and averages them.
- **Grade Summary**: Aggregates submissions and coursework data to compute course averages.
- **Error Handling**: Detects 401 errors specifically to prompt for a re-login when new permissions are required.

---

### `AuthManager.kt` & `AuthViewModel.kt`
Handles Google OAuth 2.0 flow via `AppAuth`.
- **Scopes**: Requests permissions for Classroom (courses, coursework, submissions) and Drive (`drive.file`).
- **Persistence**: Serializes and saves the `AuthState` to DataStore to maintain long-term sessions.

---

### `DriveApiService.kt`
Retrofit interface for Google Drive. Handles folder creation, file listing (with parent queries), and multipart image uploads.

---

### `ui/theme/`
- **`AppTheme.kt`**: Enum supporting `Light`, `Dark`, `Pastel`, `Ocean`, and `Midnight`.
- **`Color.kt`**: Tokenized colors for all themes.
- **`Theme.kt`**: Material 3 implementation for all five color schemes.

---

## Key Dependencies
 Library | Purpose |
---|---|
 `net.openid:appauth` | Standardized OAuth 2.0 and OpenID Connect (OIDC) client |
 `retrofit2` | REST API communication for Classroom and Drive |
 `com.google.ai.client.generativeai` | Gemini AI for document analysis |
 `com.google.mlkit:vision-document-scanner` | Native Android document scanning |
 `androidx.datastore:datastore-preferences` | Lightweight persistence for themes and auth state |
```