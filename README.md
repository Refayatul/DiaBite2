# DiaBite

DiaBite is a comprehensive, production-grade Android application designed for diabetes management and personalized nutritional guidance. Built using modern Android development practices (Kotlin, Jetpack Compose, Clean Architecture, and Hilt), DiaBite integrates a robust local SQLite cache (Room), real-time cloud storage (Firebase Firestore), secure authentication (Firebase Auth), and AI-powered food analysis (Gemini AI via Firebase Cloud Functions).

---

## 🌟 Key Features

- **Multi-Step Onboarding**: Smooth user registration flow capturing personal info, medical conditions (13+ conditions tracked), and specific diabetes configurations.
- **Advanced Authentication**: Secure Email/Password registration/login and Google Sign-In with automatic Firestore profile creation.
- **Comprehensive Food Database**: Over 1000+ foods with rich nutritional profiles, condition-specific safety ratings, and healthy alternatives.
- **AI-Powered Search & Analysis**: Integrated Gemini AI API to analyze searched foods, provide glycemic impact details, and generate alternatives with medical justifications.
- **Offline-First Capabilities**: Room Database caching with an LRU eviction policy (maximum 200 items cached) and automatic background synchronization with Firestore.
- **Medical Intelligence System**: Tailored dietary warnings and recommendations calculated based on the user's specific combination of health conditions.
- **Advanced Real-Time Search**: Debounced search queries (300ms delay) with dual-source matching (Local Cache -> Firestore -> Gemini AI fallback) and relevance-based scoring.
- **Production-Ready Error Handling**: A centralized error resolution system featuring `AppError` mappings, `Resource<T>` UI states, and responsive retry UI modules.

---

## 🏗️ Clean Architecture & Project Structure

DiaBite is built on **Clean Architecture** principles, enforcing strict separation of concerns through Domain, Data, and Presentation layers. This design ensures that the business logic is entirely decoupled from the UI framework, external APIs, and database libraries.

```
app/src/main/java/com/example/diabite/
├── common/              # Global extensions, utilities, and constants
├── data/               # Data Layer: Repositories, models, API & DB sources
│   ├── model/          # Local & network data models (User, FoodItem, etc.)
│   └── repository/     # Data source orchestrators (Firestore, Room Cache)
├── di/                 # Dependency Injection: Hilt modules
├── domain/             # Domain Layer: Use cases and repository interfaces
│   └── repository/     # Abstract definitions for data repositories
├── presentation/       # Presentation Layer: Jetpack Compose screens & ViewModels
│   ├── components/     # Reusable custom UI components (Error snackbars, custom text fields)
│   ├── screen/         # Main UI screens (Auth, Search, Details, Settings, Registration)
│   ├── theme/          # Material Design 3 styling and color palette
│   └── viewmodel/      # Architecture ViewModels maintaining StateFlows
└── util/               # Specialized utilities (AppError classification, Resource wrappers)
```

---

## 🛠️ Technology Stack

- **Language**: Kotlin 2.2.21
- **Min SDK**: 26 (Android 8.0) | **Target SDK**: 36 (Android 14)
- **UI Framework**: Jetpack Compose (Compose BOM 2025.11.00) with Material Design 3
- **Dependency Injection**: Dagger Hilt 2.51.1
- **Database / Persistence**: Room 2.6.1 + Firebase Firestore
- **Asynchronous / Reactive**: Kotlin Coroutines & Flows
- **Backend / Cloud Services**: Firebase Authentication, Cloud Firestore, Cloud Functions
- **AI Integration**: Gemini API (via Firebase Cloud Functions / direct integration)
- **Build System**: Gradle Kotlin DSL with central Version Catalogs (`libs.versions.toml`)
- **Logging**: Timber for structured, environment-aware logging

---

## ⚡ Setup & Installation

### Prerequisites
- Android Studio Ladybug (or newer)
- Java Development Kit (JDK) 17+
- Node.js (v18+) - only required for database seeding

### Step 1: Clone the Repository
```bash
git clone https://github.com/Refayatul/DiaBite.git
cd DiaBite
```

### Step 2: Configure Firebase
1. Create a Firebase project in the [Firebase Console](https://console.firebase.google.com/).
2. Enable **Authentication** (Email/Password and Google Sign-In).
3. Enable **Cloud Firestore** in Test or Production Mode.
4. Add your Android App to the Firebase project (Package name: `com.example.diabite`).
5. Download the `google-services.json` file and place it in the `app/` directory.

### Step 3: Add API Keys & Local Configuration
Create a `local.properties` file in the root directory (if not already present) and configure your Gemini API Key:
```properties
sdk.dir=/path/to/your/android/sdk
GEMINI_API_KEY=your_gemini_api_key_here
```

### Step 4: Seed the Database (Optional)
The project includes a Node.js seed script to populate Firestore with initial food and category data.
1. Navigate to the `seed/` directory:
   ```bash
   cd seed
   ```
2. Install dependencies:
   ```bash
   npm install
   ```
3. Place your Firebase Service Account private key in `seed/serviceAccountKey.json`.
4. Run the upload script:
   ```bash
   node upload-data.js
   ```

---

## 🔒 Firebase Security Rules

To protect user profile data and database items, configure the following rules in Cloud Firestore:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Allow users to read/write only their own profile data
    match /users/{userId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
    
    // Public read access for food items and categories; write access restricted to administrators
    match /foodItems/{foodId} {
      allow read: if request.auth != null;
      allow write: if false; // Admin-only updates
    }
    
    match /categories/{categoryId} {
      allow read: if request.auth != null;
      allow write: if false;
    }
  }
}
```

---

## 🧪 Quality Assurance & Error Resilience

DiaBite implements standard error resilience mechanisms:
- **Centralized Error Hierarchy**: Exception mappings from Network, Firestore, and Room are translated into `AppError` types.
- **Resource Wrapper**: Network and DB calls are wrapped inside the `Resource<T>` sealed class (exposing `Loading`, `Success`, and `Error` states).
- **Exponential Backoff**: Automatic retry logic for all remote Firestore requests during network instability.
- **Proguard Rules**: Configured production Proguard rules for Firebase, Hilt, and Room serialization to ensure lightweight, secure releases.
