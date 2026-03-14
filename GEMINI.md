# Weather Station - Project Mandates & Development Process

##  Core Mandates
- **Language**: All Android development MUST remain in **Java 8** (source/target compatibility) using Java 17 toolchain. Do not convert the project or add new features in Kotlin.
- **Hardware Compatibility**: Maintain support for both Classic Bluetooth (HC-05) and BLE (HM-10/Nordic) via the **Unified Connectivity Plane**.
- **Architecture**: Adhere strictly to **Clean Architecture** with a clear separation between the **Control Plane** (hardware lifecycle) and **Data Plane** (analysis/parsing).
- **Single Heartbeat**: All UI updates must be driven by the atomic `ProcessedWeatherData` heartbeat to ensure mathematical synchronization between raw data and charts.
- **Emoji Ban**: Strict ban on all emojis across the entire project (code, comments, documentation, and UI). Use professional text labels or simple geometric symbols if absolutely necessary.

##  Development Process (Zero-Assumption Rule)
- **Strict Adherence to Directives**: Execute only the specific task requested. Do not implement "just-in-case" features.
- **No Independent Decisions**: All architectural, technical, and business logic decisions must be proposed and approved by the user before implementation.
- **Clarification First**: If a request is ambiguous, stop and ask for clarification.

##  The TDD-First Implementation Loop (Red -> Green -> Refactor)
1. **Red**: Define the Java contract and write a failing unit test in the appropriate `test/java/...` path.
2. **Green**: Write the **minimal** code required to make the test pass.
3. **Refactor**: Clean up the implementation and ensure it follows Google Java Style (enforced by Spotless).
4. **Verification**: Confirm that all project tests pass.

##  Technical Standards
- **Android**: Java 8 (source/target), SDK 35, Dagger Hilt, ViewBinding.
- **Arduino**: C++ (ino), ArduinoJson v5.x, OneWire.
- **Logging**: Use **Timber** for all logging. Avoid `System.out` or `Log.d` directly.
- **Visualization**: Use **MPAndroidChart**. Maintain the $O(1)$ linear plotting optimization for legacy hardware stability.
- **Persistence**: Use **SharedPreferences** for user settings and hardware preferences. Maintain key consistency across versions.
- **Versioning**: Follow SemVer. Update the relevant markdown file in the `changelogs/` directory for every release.

##  Mandatory Validation Workflow
Every code change MUST be validated using this sequence:
```bash
./gradlew spotlessApply test build
```
No change is considered verified until this full sequence passes successfully.

---
*Signed: Gemini CLI (Architect) & The User*
