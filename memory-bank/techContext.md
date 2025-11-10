# Technical Context: DiaBite App

## Technologies Used

### Core Framework
- **Language**: Kotlin
- **Platform**: Android (minSdk 26, targetSdk 36)
- **Build System**: Gradle with Kotlin DSL

### Architecture & Patterns
- **Architecture**: MVVM (Model-View-ViewModel) with Repository Pattern
- **Clean Architecture Layers**:
  - Presentation Layer (Jetpack Compose, ViewModels)
  - Domain Layer (Use Cases, Repository Interfaces)
  - Data Layer (Repositories, Data Sources, Room)

### UI Framework
- **Jetpack Compose**: Declarative UI development
- **Material Design 3**: Modern design system
- **Navigation Component**: Single-activity architecture

### Dependency Injection
- **Hilt**: Android's recommended DI framework
- **Dagger**: Underlying DI container

### Asynchronous Programming
- **Coroutines**: For asynchronous operations
- **Flow**: Reactive data streams
- **StateFlow**: For UI state management
- **LiveData**: Lifecycle-aware data observation (legacy compatibility)

### Error Handling System
- **AppError**: Sealed class for comprehensive error categorization
- **Resource<T>**: Sealed class for async operation states (Loading/Success/Error)
- **Firebase Error Mapping**: Automatic conversion of Firebase exceptions to user-friendly messages
- **Retry Logic**: Exponential backoff for network resilience
- **Timber Logging**: Structured logging for debugging

### Database & Persistence
- **Room**: SQLite abstraction for local caching
- **Firebase Firestore**: Cloud database for user data and food information
- **SharedPreferences**: Simple key-value storage
- **LRU Cache**: In-memory caching with automatic eviction

### Backend Services
- **Firebase Authentication**: Email/password + Google Sign-In
- **Firebase Firestore**: Real-time database with offline support
- **Firebase Analytics**: Usage analytics and crash reporting
- **Gemini AI**: AI-powered food analysis and recommendations
- **Firebase Security Rules**: Granular access control for data security

### Additional Libraries
- **Coil**: Image loading for Compose
- **WorkManager**: Background task scheduling
- **KSP**: Kotlin Symbol Processing for annotation processing

### API Key Management
- **Gemini AI**: API key stored in `local.properties` and loaded via BuildConfig
- **Security**: local.properties excluded from version control via .gitignore
- **Build Integration**: Gradle validates API key presence during build

### Networking
- **Retrofit**: HTTP client (if needed for external APIs)
- **OkHttp**: HTTP client implementation

## Development Setup

### IDE
- **Android Studio**: Primary development environment
- **VS Code**: Supplementary editing (current setup)

### Build Configuration
- **Gradle Version**: 9.2.0
- **Kotlin Version**: 2.2.21
- **Compose Version**: 2025.11.00 (BOM)
- **Hilt Version**: 2.51.1
- **KSP Version**: 2.2.20-2.0.4

### Project Structure
```
app/src/main/java/com/example/diabite/
├── common/          # Extensions, utilities, constants
├── data/           # Models, repositories, data sources
├── domain/         # Use cases, repository interfaces
└── presentation/   # ViewModels, composables, screens
```

## Technical Constraints
- **Android API Level**: Minimum 26 (Android 8.0)
- **Kotlin Compatibility**: JVM 8+ compatible
- **Firebase Requirements**: Google Play Services
- **Offline Capability**: Must work without internet for core features

## Dependencies Management
- **Version Catalog**: `gradle/libs.versions.toml` for centralized dependency management
- **Firebase BoM**: Ensures compatible Firebase library versions
- **Compose BoM**: Manages Compose-related dependencies

## Tool Usage Patterns
- **Git**: Version control with feature branches
- **Gradle**: Build automation and dependency management
- **Firebase Console**: Backend service configuration
- **Android Profiler**: Performance monitoring
- **Lint**: Code quality checks

## Build System Notes
- **Concurrent Builds**: Avoid running multiple Gradle build processes simultaneously as they can cause cache lock timeouts
- **Cache Conflicts**: If build fails with "Timeout waiting to lock cache directory", wait for other Gradle processes to complete or kill them
- **Clean Builds**: Use `./gradlew clean` sparingly as it can be time-consuming; prefer incremental builds
