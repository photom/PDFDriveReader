---
name: github-ci-android
description: Expert guidance on maintaining and extending the GitHub Actions CI pipeline for Android, including testing, linting, dependency vulnerability checks, and code metrics.
---

# GitHub CI Maintenance for Android

This skill provides expert guidance on maintaining and extending the GitHub Actions CI pipeline in this Android project. It establishes best practices for automated testing, static analysis, dependency scanning, and performance metrics.

## Core CI Principles

1. **Fast Feedback**: Ensure unit tests (`testDebugUnitTest`) and static analysis (`lintDebug`) run efficiently. Utilize gradle caching via `actions/setup-java`.
2. **Security & Vulnerability**: Maintain dependency review and submission actions to catch vulnerable dependencies automatically. The CI is configured to use GitHub's dependency graph.
3. **Static Analysis & Metrics**: Use Android Lint as the baseline for static analysis. This catches structural, performance, and security issues in code and resources.

## Regular Maintenance Tasks

- **Update Action Versions**: Periodically check for updates to `actions/checkout`, `actions/setup-java`, and `gradle/actions/dependency-submission` to use the latest versions (e.g., v4).
- **Test Matrix Expansion**: When new Android API levels are targeted, consider adding UI testing using `ReactiveCircus/android-emulator-runner` to run Android instrumentation tests on an emulator.
- **Dependency Updates**: Keep the gradle wrappers and library dependencies up to date. Monitor dependabot alerts triggered by the dependency submission graph.

## Extending the CI Pipeline

### Adding Code Coverage (JaCoCo)
If code metrics for test coverage are required:
1. Apply the `jacoco` plugin in `app/build.gradle.kts`.
2. Add a `./gradlew jacocoTestReport` step to the CI workflow.
3. Upload the generated report (`app/build/reports/jacoco/`) as a workflow artifact or submit it to a tool like Codecov.

### Adding Detekt (Advanced Static Analysis)
For Kotlin-specific code metrics and static analysis:
1. Add the Detekt Gradle plugin to `build.gradle.kts`.
2. Configure the `detekt { ... }` block and generate a `detekt.yml` config file.
3. Add a step in the CI workflow: `run: ./gradlew detekt --no-daemon`.

### Emulators & UI Tests
If Android UI instrumentation tests (`androidTest`) become a core metric:
```yaml
      - name: Run Instrumentation Tests
        uses: reactivecircus/android-emulator-runner@v2
        with:
          api-level: 33
          target: google_apis
          arch: x86_64
          script: ./gradlew connectedCheck --no-daemon
```

## Workflow Modification Rules
- ALWAYS use `--no-daemon` for Gradle commands in CI to prevent memory exhaustion and zombie processes.
- NEVER commit secrets or credentials directly. Use GitHub Secrets (`${{ secrets.MY_SECRET }}`).
- Ensure you validate YAML syntax and verify action inputs according to their documentation when making changes to `.github/workflows/ci.yml`.
