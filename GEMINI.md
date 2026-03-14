# Weather Station - Project Mandates & Development Process

## 🛡️ Core Mandates
- **Language**: All Android development MUST remain in **Java 17**. Do not convert the project or add new features in Kotlin.
- **Hardware Compatibility**: Maintain support for both Classic Bluetooth (HC-05) and BLE (HM-10/Nordic) via the **Unified Connectivity Plane**.
- **Architecture**: Adhere strictly to **Clean Architecture** with clear separation between the Control Plane (hardware) and Data Plane (analysis).

## 🚀 Development Process (Zero-Assumption Rule)
To ensure the implementation remains strictly aligned with the user's vision:
- **Strict Adherence to Directives**: Execute only the specific task requested. Do not implement "just-in-case" features or additional logic beyond the current scope.
- **No Independent Decisions**: All architectural, technical, and business logic decisions must be proposed and approved by the user before implementation.
- **Clarification First**: If a request is ambiguous, stop and ask for clarification instead of proceeding with an assumption.

## 🧪 The TDD-First Implementation Loop (Red -> Green -> Refactor)
Never implement a large feature in one go. Every task must follow a strict TDD cycle:
1. **Red**: Define the Java contract and write a failing unit test.
2. **Green**: Write the **minimal** code required to make the test pass.
3. **Refactor**: Clean up the implementation and ensure it follows idiomatic Java standards.
4. **Verification**: Confirm that all project tests pass.

## 🧱 Technical Standards
- **Android**: Java 17, SDK 35, Dagger Hilt, ViewBinding.
- **Arduino**: C++ (ino), ArduinoJson v5.x, OneWire.
- **Database (Future)**: Any schema changes MUST follow the **Expand and Contract** pattern across multiple atomic releases. Never use destructive updates.
- **Documentation**: Every public method must have Javadoc explaining the "Why," not just the "How."
- **Visuals**: Use professional text labels or reliable geometric symbols in the UI for cross-platform consistency. Avoid emojis in the core application UI.

## 🔬 Mandatory Validation Workflow
Every code change MUST be validated using this sequence:
```bash
./gradlew spotlessApply test build
```
No change is considered verified until this full sequence passes successfully.

---
*Signed: Gemini CLI (Architect) & The User*
