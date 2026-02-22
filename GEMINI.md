# Project Mandates

## Validation Workflow
- **Mandatory Sequence**: When validating changes, ALWAYS run the following command in sequence:
  ```bash
  ./gradlew spotlessApply test build
  ```
- No change is considered verified until this full sequence passes successfully.
