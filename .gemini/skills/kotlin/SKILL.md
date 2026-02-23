---
name: kotlin
description: Expert guidance on Kotlin programming, following official coding conventions and best practices. Use for all Kotlin development, specifically for test method naming and idiomatic implementation policies.
---

# Kotlin Coding Standards & Conventions

This skill ensures that all code follows the official [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html) and project-specific implementation policies.

## 1. Test Method Naming
In accordance with the official conventions, test methods in this project must use backticks to provide descriptive, sentence-like names. This is permitted and encouraged for unit tests to improve readability.

### Standard
- Use backticks for method names.
- Describe the condition and the expected result.
- Use spaces between words.

### Example
```kotlin
@Test
fun `PagePosition should throw exception when pageIndex is negative`() {
    // ...
}
```

## 2. General Coding Style
- **Naming**: Use `PascalCase` for classes and `camelCase` for methods and variables.
- **Immutability**: Always prefer `val` over `var`. Use `immutable` collections by default.
- **Null Safety**: Avoid non-null assertions (`!!`). Use safe calls (`?.`), the Elvis operator (`?:`), or `let` blocks.
- **Scope Functions**: Use `apply`, `also`, `let`, `run`, and `with` judiciously to improve code flow without nesting too deeply.

## 3. Idiomatic Kotlin Features
- **Data Classes**: Use for simple data holders (Value Objects and DTOs).
- **Sealed Classes**: Use for restricted class hierarchies (e.g., Result patterns, UI states).
- **Extension Functions**: Use to add functionality to external or framework classes without inheritance.
- **Coroutines**: Use for all asynchronous work. Prefer `Flow` for reactive data streams.

## 4. Documentation (KDoc)
Follow the [android-implementation-standards](references/../android-implementation-standards/SKILL.md) for mandatory documentation of interfaces, classes, and public methods.

## References
- [Official Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html)
- [Android Implementation Standards](../android-implementation-standards/SKILL.md)
