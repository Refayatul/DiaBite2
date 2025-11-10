# System Patterns: Diabetes Demo App

## System Architecture

### Clean Architecture Implementation
```
┌─────────────────────────────────────┐
│          Presentation Layer         │
│  ┌─────────────────────────────────┐ │
│  │        ViewModels               │ │
│  │        Composables              │ │
│  │        Screens                  │ │
│  └─────────────────────────────────┘ │
└─────────────────────────────────────┘
                │
                ▼
┌─────────────────────────────────────┐
│          Domain Layer               │
│  ┌─────────────────────────────────┐ │
│  │        Use Cases                │ │
│  │        Repository Interfaces    │ │
│  └─────────────────────────────────┘ │
└─────────────────────────────────────┘
                │
                ▼
┌─────────────────────────────────────┐
│          Data Layer                 │
│  ┌─────────────────────────────────┐ │
│  │        Repositories             │ │
│  │        Data Sources             │ │
│  │        Models                   │ │
│  └─────────────────────────────────┘ │
└─────────────────────────────────────┘
```

## Key Technical Decisions

### 1. MVVM with Repository Pattern
- **ViewModels** handle UI logic and state management
- **Repositories** abstract data access and provide clean API
- **Separation of Concerns** between UI, business logic, and data

### 2. Single Activity Architecture
- **MainActivity** as single entry point
- **Navigation Component** manages fragment/compose navigation
- **Back stack management** handled by NavController

### 3. Dependency Injection with Hilt
- **Application-level** component for app-wide dependencies
- **Activity/Fragment-scoped** components for UI dependencies
- **ViewModel injection** for clean ViewModel creation

### 4. Reactive Data Flow
- **Flow** for asynchronous data streams
- **LiveData** for lifecycle-aware UI updates
- **StateFlow** for state management in ViewModels

## Design Patterns

### Repository Pattern
```kotlin
interface AuthRepository {
    fun login(email: String, password: String): Flow<Result<User>>
    fun register(user: User): Flow<Result<User>>
    fun logout(): Flow<Result<Unit>>
}
```

### Use Case Pattern
```kotlin
class LoginUseCase(
    private val authRepository: AuthRepository
) {
    operator fun invoke(email: String, password: String): Flow<Result<User>> {
        return authRepository.login(email, password)
    }
}
```

### Factory Pattern for ViewModels
- Hilt generates ViewModel factories automatically
- Clean injection of dependencies into ViewModels

## Component Relationships

### Data Flow
1. **UI Events** → ViewModel
2. **ViewModel** → Use Case execution
3. **Use Case** → Repository call
4. **Repository** → Data Source (Local/Remote)
5. **Data Source** → Repository → Use Case → ViewModel → UI

### Dependency Injection Graph
- **Application** provides core services (Firebase, Database)
- **Activity** provides navigation and UI contexts
- **ViewModels** receive repositories and use cases
- **Repositories** receive data sources

## Critical Implementation Paths

### Authentication Flow
1. User enters credentials in LoginScreen
2. LoginViewModel calls LoginUseCase
3. AuthRepository delegates to FirebaseAuthDataSource
4. Success: Navigate to MainScreen, store user session
5. Failure: Display error message

### Data Synchronization
1. Local Room database for offline access
2. Firebase Firestore for cloud synchronization
3. Repository decides data source based on connectivity
4. Conflict resolution strategies for data merging

### Navigation Flow
- Authentication screens (Login/Register)
- Main app navigation (Bottom navigation or drawer)
- Deep linking support for external navigation
- State preservation during configuration changes

## Error Handling Patterns
- **Resource<T>** sealed class for async operation states (Loading/Success/Error)
- **AppError** sealed class for comprehensive error categorization
- **Firebase error mapping** with automatic conversion to user-friendly messages
- **Retry mechanisms** with exponential backoff for network operations
- **Input validation** at ViewModel level with immediate feedback
- **Timber logging** for structured debugging information
- **UI error components** (ErrorHandler, ErrorCard, ErrorSnackbar) for consistent error display

## Firebase Integration Patterns
- **Firebase-first architecture** with offline capabilities
- **Repository pattern** with Resource<T> return types
- **Security rules** for granular access control
- **Batch operations** for efficient data uploads
- **Real-time listeners** for reactive data updates
- **Offline persistence** with automatic sync

## Testing Patterns
- **Unit tests** for Use Cases and ViewModels
- **Integration tests** for Repository implementations
- **UI tests** with Compose testing framework
- **Mock data sources** for isolated testing
