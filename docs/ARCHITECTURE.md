# Weather Station - Android Architecture

This document describes the high-level architecture of the Weather Station Android application, illustrating how components interact to provide real-time data monitoring and thermal analysis.

##  Architectural Overview

The application follows a modern **MVVM (Model-View-ViewModel)** pattern with a strict separation between the **Control Plane** and the **Data Plane**, and a **Delegated Activity** structure in the UI layer.

###  Plane Separation
1.  **Data Plane (`WeatherRepository`)**: Handles sensor data processing, protocol parsing, and analytical trend calculation using the **Single Heartbeat** pattern.
2.  **Control Plane (`WeatherConnectionController`)**: Manages hardware lifecycles, Bluetooth adapter state, device discovery, and connection orchestration.

###  Delegated Activity Architecture
To prevent `WSActivity` from becoming a "God Object", its responsibilities are delegated to specialized components:
- **`PermissionDelegate`**: Manages runtime permission requests and results.
- **`NavigationDelegate`**: Orchestrates Toolbar, Drawer, and Jetpack Navigation.
- **`UIEventDelegate`**: Handles dialogs and state-driven Snackbars.

```mermaid
graph TD
    %% 1. UI LAYER (Top)
    subgraph UI_Layer [UI: Activity & Fragments]
        WSA[WSActivity]
        
        subgraph Activity_Delegates [Activity Helpers]
            PD[PermissionDelegate]
            ND[NavigationDelegate]
            UED[UIEventDelegate]
        end
        
        subgraph Fragments [Views]
            DF[Dashboard]
            GVF[Graphs]
            BDLF[BT List]
        end
    end

    %% 2. VIEWMODEL LAYER
    subgraph VM_Layer [ViewModel: Single Source of Truth]
        WVM[WeatherViewModel]
        WUS[WeatherUiState Snapshot]
    end

    %% 3. DOMAIN LAYER (Business Logic)
    subgraph Domain_Layer [Domain: UseCases]
        direction TB
        GWUS[GetWeatherUiState]
        CTD[ConnectToDevice]
        GPD[GetPairedDevices]
        MD[ManageDiscovery]
        PDU[PairDevice]
    end

    %% 4. PLANES (Implementation)
    subgraph Planes [Core Logic]
        direction LR
        
        subgraph Control_Plane [Control Plane: How]
            WCC[WeatherConnectionController]
            CM[ConnectionManager]
            CC[Composite Connection]
            BC[Classic BT Conn]
            BLE[BLE Conn]
            SC[Sim Conn]
        end

        subgraph Data_Plane [Data Plane: What]
            WRI[WeatherRepository]
            TA[ThermalAnalyzer]
            WMP[Parser]
        end
    end

    %% FLOW CONNECTIONS
    WSA -- Delegating --> Activity_Delegates
    Fragments & WSA -- Observe --> WVM
    WVM -- State --> WUS
    WVM -- Execute --> Domain_Layer

    %% UseCase to Plane Mapping
    GWUS -- Pulls --> WRI & WCC
    CTD & GPD & MD & PDU -- Triggers --> WCC

    %% Internal Plane Flow
    WCC -- "Raw Data" --> WRI
    WRI -- "Single Heartbeat" --> GWUS
    
    %% Hardware linkage
    WCC --> CM
    CM --> CC
    CC --> BC & BLE & SC
    WRI --> TA & WMP
```

##  Key Components

### 1. **UI Layer (Delegates)**
- **`PermissionDelegate`**: Centralizes the logic for Location, Bluetooth, and Notification permissions required across different Android versions.
- **`NavigationDelegate`**: Manages the integration between the Activity's Toolbar and the Fragments' navigation lifecycle.
- **`UIEventDelegate`**: Decouples the presentation of dialogs (like Reconnect) from the core Activity lifecycle.

### 2. **Domain Layer (Specific UseCases)**
- **`GetWeatherUiStateUseCase`**: Aggregates the atomic heartbeat from the Data Plane and hardware state from the Control Plane.
- **`ConnectToDeviceUseCase`**: Executes connection requests via the Control Plane.
- **`GetPairedDevicesUseCase`**: Retrieves filtered device lists from the Control Plane.
- **`PairDeviceUseCase`**: Initiates hardware bonding for new stations.

### 3. Control Plane (`WeatherConnectionController`)
The hardware orchestrator. Encapsulates all Bluetooth management and connection lifecycle logic. Uses a **CompositeConnection** pattern to automatically route to the appropriate driver (`BluetoothConnection`, `BleConnection`, or `SimulatorConnection`) based on the device's hardware profile.

### 4. **Data Plane (`WeatherRepository`)**
The analytical engine. Implements `HardwareEventListener` to process data from the Control Plane and produce the **Single Heartbeat**. Implements the "What" of the system.

##  Quality Assurance & Testability

The application follows a **Test-Driven Development (TDD)** approach for all business and hardware logic.

### **Testing Strategy**
*   **Domain Layer**: ~100% coverage on all `UseCases`. Verified via JUnit and Mockito.
*   **Data Plane**: Intensive coverage on `ThermalAnalyzer` and `WeatherMessageParser` to ensure mathematical accuracy of thermal detection.
*   **Control Plane**: Hardware delegates and frame-sync logic are verified through unit tests simulating hardware state transitions.
*   **UDF Verification**: `WeatherUiState` and `ViewModel` interactions are tested to ensure state immutability and atomic updates.

### **Coverage Reporting (JaCoCo)**
The project uses JaCoCo to track test coverage. To generate a report:
```bash
./gradlew jacocoTestReport
```
Report Location: `app/build/reports/jacoco/jacocoTestReport/html/index.html`

##  Data Flow Path (The Heartbeat)
1. **Control Plane** syncs raw bytes into a string frame and passes it to the **Data Plane**.
2. **Data Plane** parsed and calculates trends synchronously.
3. **Data Plane** bundles everything into **`ProcessedWeatherData`** and posts it.
4. **UI Layer** observes the unified state and updates charts and text atomically.
