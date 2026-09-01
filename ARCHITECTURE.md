/**
 * ROUTECJ ADMIN - PRODUCTION-READY ANDROID APPLICATION
 * 
 * ═══════════════════════════════════════════════════════════════════════════════
 * ARCHITECTURE OVERVIEW
 * ═══════════════════════════════════════════════════════════════════════════════
 * 
 * This application follows a Clean Architecture pattern with MVVM design.
 * 
 * Layer Structure:
 * ├── Presentation Layer (UI)
 * │   ├── Screens (Composables)
 * │   ├── ViewModels
 * │   ├── Components
 * │   └── Navigation
 * │
 * ├── Domain Layer (Business Logic)
 * │   ├── Models
 * │   ├── Repositories (Interfaces)
 * │   └── Use Cases
 * │
 * └── Data Layer (Data Access)
 *     ├── Remote (API)
 *     ├── Local (Database)
 *     ├── Models (DTOs)
 *     └── Repositories (Implementations)
 * 
 * ═══════════════════════════════════════════════════════════════════════════════
 * PACKAGE STRUCTURE
 * ═══════════════════════════════════════════════════════════════════════════════
 * 
 * com.routecj.admin
 * │
 * ├── core/                          (Core utilities and base classes)
 * │   ├── ui/                        (UI utilities and base composables)
 * │   │   └── CommonComposables.kt   (Loading, Error, Empty states)
 * │   ├── util/                      (Utility functions and constants)
 * │   │   ├── Constants.kt           (App-wide constants)
 * │   │   ├── Extensions.kt          (Extension functions)
 * │   │   └── Result.kt              (Result wrapper for async operations)
 * │   ├── network/                   (Network configuration)
 * │   │   └── RetrofitClient.kt      (Retrofit setup)
 * │   └── presentation/              (Base classes for presentation)
 * │       └── BaseViewModel.kt       (Base ViewModel with utilities)
 * │
 * ├── data/                          (Data layer - Repository implementation)
 * │   ├── model/                     (DTOs - Data Transfer Objects)
 * │   │   └── OrderDTO.kt            (API response models)
 * │   ├── local/                     (Database entities - will be added)
 * │   │   └── (Database Daos)
 * │   ├── remote/                    (API services)
 * │   │   └── OrderApiService.kt     (Retrofit API endpoints)
 * │   └── repository/                (Repository implementations)
 * │       ├── BaseRepository.kt      (Base repository with common patterns)
 * │       └── OrderRepositoryImpl.kt  (Order repository implementation)
 * │
 * ├── domain/                        (Domain layer - Business logic)
 * │   ├── model/                     (Domain models - independent of data sources)
 * │   │   ├── Order.kt
 * │   │   ├── Driver.kt
 * │   │   ├── Vehicle.kt
 * │   │   └── (Other domain models)
 * │   ├── repository/                (Repository interfaces)
 * │   │   └── OrderRepository.kt     (Abstract contracts)
 * │   └── usecase/                   (Business use cases)
 * │       └── OrderUseCases.kt       (Order-related operations)
 * │
 * ├── presentation/                  (Presentation layer - UI)
 * │   ├── splash/                    (Splash screen)
 * │   │   └── SplashScreen.kt
 * │   ├── login/                     (Authentication)
 * │   │   ├── LoginScreen.kt
 * │   │   └── LoginViewModel.kt
 * │   ├── dashboard/                 (Main dashboard)
 * │   │   ├── DashboardScreen.kt
 * │   │   └── DashboardViewModel.kt
 * │   ├── orders/                    (Orders management)
 * │   │   ├── OrdersScreen.kt
 * │   │   └── OrdersViewModel.kt
 * │   ├── drivers/                   (Driver management)
 * │   ├── vehicles/                  (Vehicle management)
 * │   ├── godowns/                   (Warehouse management)
 * │   ├── dispatch/                  (Dispatch management)
 * │   ├── tracking/                  (Real-time tracking)
 * │   ├── reports/                   (Analytics & Reports)
 * │   ├── components/                (Reusable UI components)
 * │   │   └── CommonComponents.kt    (Buttons, Cards, Badges, etc.)
 * │   └── navigation/                (Navigation setup)
 * │       └── NavGraph.kt            (Navigation routes and graph)
 * │
 * ├── di/                            (Dependency Injection - Hilt modules)
 * │   ├── AppModule.kt               (App-level dependencies)
 * │   ├── DataModule.kt              (Data layer dependencies)
 * │   └── DomainModule.kt            (Domain layer dependencies)
 * │
 * ├── ui/                            (UI theme and styling)
 * │   └── theme/
 * │       ├── Color.kt               (Material Design 3 colors)
 * │       ├── Type.kt                (Typography styles)
 * │       └── Theme.kt               (Theme definition)
 * │
 * ├── MainActivity.kt                (Entry point activity)
 * ├── RouteCJAdminApp.kt             (Application class)
 * └── AndroidManifest.xml            (App manifest)
 * 
 * ═══════════════════════════════════════════════════════════════════════════════
 * DATA FLOW ARCHITECTURE
 * ═══════════════════════════════════════════════════════════════════════════════
 * 
 * User Interaction (UI)
 *    ↓
 * ViewModel (StateFlow)
 *    ↓
 * Use Case (Business Logic)
 *    ↓
 * Repository Interface (Abstraction)
 *    ↓
 * Repository Implementation (Data Orchestration)
 *    ↓
 * Data Sources (Remote API / Local DB)
 *    ↓
 * API Service / Database
 * 
 * Response Flow (Reverse):
 * Data Sources
 *    ↓
 * DTO Conversion
 *    ↓
 * Domain Model Conversion
 *    ↓
 * Result Wrapper
 *    ↓
 * ViewModel StateFlow
 *    ↓
 * UI Recomposition
 * 
 * ═══════════════════════════════════════════════════════════════════════════════
 * KEY COMPONENTS & PATTERNS
 * ═══════════════════════════════════════════════════════════════════════════════
 * 
 * 1. MVVM Pattern
 *    - Model: Domain and Data Models
 *    - View: Composable UI functions
 *    - ViewModel: State management and business logic orchestration
 * 
 * 2. Repository Pattern
 *    - Single source of truth
 *    - Abstracts data sources
 *    - Handles offline scenarios
 *    - Implements caching strategies
 * 
 * 3. Use Case Pattern (Interactors)
 *    - Encapsulates business logic
 *    - Single Responsibility Principle
 *    - Reusable across features
 *    - Easy to test
 * 
 * 4. Dependency Injection (Hilt)
 *    - Automatic injection
 *    - Testable architecture
 *    - Scope management
 *    - Module-based configuration
 * 
 * 5. StateFlow & Flow
 *    - Reactive programming
 *    - Lifecycle-aware
 *    - Memory safe
 *    - Coroutine support
 * 
 * 6. Result Wrapper
 *    - Type-safe error handling
 *    - Loading states
 *    - Success/Failure distinction
 *    - Extension functions for transformation
 * 
 * ═══════════════════════════════════════════════════════════════════════════════
 * TECH STACK
 * ═══════════════════════════════════════════════════════════════════════════════
 * 
 * UI Framework:
 * - Jetpack Compose (Modern declarative UI)
 * - Material Design 3 (Design system)
 * 
 * Navigation:
 * - Jetpack Navigation Compose (Type-safe navigation)
 * 
 * Architecture:
 * - Hilt (Dependency injection)
 * - LiveData/StateFlow (State management)
 * - ViewModel (UI logic holder)
 * 
 * Networking:
 * - Retrofit 2 (REST client)
 * - OkHttp 3 (HTTP client with interceptors)
 * - Gson (JSON serialization)
 * 
 * Database:
 * - Room (Local database)
 * 
 * Data Storage:
 * - DataStore (Preferences)
 * 
 * Asynchronous Programming:
 * - Coroutines (Lightweight threads)
 * 
 * Logging:
 * - Timber (Logging library)
 * 
 * Image Loading:
 * - Coil (Image loading and caching)
 * 
 * ═══════════════════════════════════════════════════════════════════════════════
 * SOLID PRINCIPLES IMPLEMENTATION
 * ═══════════════════════════════════════════════════════════════════════════════
 * 
 * 1. Single Responsibility Principle (SRP)
 *    - Each class has one reason to change
 *    - ViewModels don't fetch data directly
 *    - Repositories don't contain UI logic
 *    - Use cases handle single operations
 * 
 * 2. Open/Closed Principle (OCP)
 *    - Open for extension, closed for modification
 *    - Repository interfaces allow new implementations
 *    - Base classes provide common functionality
 *    - Can add new use cases without modifying existing code
 * 
 * 3. Liskov Substitution Principle (LSP)
 *    - Repository implementations can replace interface
 *    - ViewModels can work with any repository implementation
 *    - Ensures type safety and predictability
 * 
 * 4. Interface Segregation Principle (ISP)
 *    - Focused repository interfaces
 *    - Clients don't depend on interfaces they don't use
 *    - Specific API services for different endpoints
 * 
 * 5. Dependency Inversion Principle (DIP)
 *    - Depend on abstractions, not concretions
 *    - ViewModels depend on repository interfaces
 *    - Hilt manages dependency creation
 *    - Loose coupling between layers
 * 
 * ═══════════════════════════════════════════════════════════════════════════════
 * FEATURES
 * ═══════════════════════════════════════════════════════════════════════════════
 * 
 * Core Modules:
 * ✓ Splash Screen (App entry point)
 * ✓ Login Screen (Authentication)
 * ✓ Dashboard (Main hub)
 * ✓ Orders Management (CRUD operations)
 * ✓ Driver Management
 * ✓ Vehicle Management
 * ✓ Godowns/Warehouse Management
 * ✓ Dispatch Management
 * ✓ Real-time Tracking
 * ✓ Reports & Analytics
 * 
 * Architecture Features:
 * ✓ Type-safe navigation
 * ✓ Dependency injection
 * ✓ Reactive UI updates
 * ✓ Error handling
 * ✓ Loading states
 * ✓ Offline support ready
 * ✓ Modular structure
 * ✓ Easy to test
 * 
 * ═══════════════════════════════════════════════════════════════════════════════
 * DEVELOPMENT GUIDELINES
 * ═══════════════════════════════════════════════════════════════════════════════
 * 
 * Creating New Features:
 * 1. Create domain model in domain/model
 * 2. Create domain repository interface in domain/repository
 * 3. Create API service in data/remote (if API needed)
 * 4. Create DTO in data/model
 * 5. Create repository implementation in data/repository
 * 6. Create use cases in domain/usecase
 * 7. Create ViewModel in presentation/<feature>
 * 8. Create UI screens in presentation/<feature>
 * 9. Add navigation route
 * 10. Register DI bindings in appropriate module
 * 
 * Code Quality Standards:
 * - Use meaningful variable names
 * - Add documentation comments
 * - Handle all Result states (Loading, Success, Error)
 * - Implement proper error handling
 * - Use sealed classes for type safety
 * - Follow naming conventions
 * - Keep functions small and focused
 * - Use extension functions for readability
 * 
 * ═══════════════════════════════════════════════════════════════════════════════
 * TESTING STRATEGY
 * ═══════════════════════════════════════════════════════════════════════════════
 * 
 * Unit Tests:
 * - ViewModels (with mock repositories)
 * - Use cases (with mock repositories)
 * - Extension functions
 * - Utility functions
 * 
 * Integration Tests:
 * - Repository implementations
 * - API service calls
 * - Database operations
 * 
 * UI Tests:
 * - Screen rendering
 * - User interactions
 * - State changes
 * 
 * ═══════════════════════════════════════════════════════════════════════════════
 * DEPLOYMENT
 * ═══════════════════════════════════════════════════════════════════════════════
 * 
 * Build Variants:
 * - Debug: Full logging, development API
 * - Release: Optimized, production API
 * 
 * Configuration:
 * - Build.gradle.kts: Dependencies and build settings
 * - AndroidManifest.xml: App configuration
 * - gradle.properties: Build properties
 * 
 * ═══════════════════════════════════════════════════════════════════════════════
 */

