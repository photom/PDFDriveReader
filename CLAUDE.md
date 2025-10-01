# Bootstrap your project memory
claude -p "Bootstrap a CLAUDE.md for my codebase"

# Project Memory

See @README.md for an overview.

## Development Guidelines
- Follow the Diátaxis framework for documentation.
  - When creating a new document, add a link to it in one of the top four quadrants of the relevant Diátaxis document.
- Comply with SOLID principles.
- Update documentation if specifications or design change.
- During refactoring, do not change specifications or design.
- Documentation must not contain personal information:
  - Use relative paths, not absolute paths.
  - Only use file and directory names within the repository.
  - Do not use parent directory path.
- Comply with the secure coding guidelines below:
  - https://www.jssec.org/dl/android_securecoding_en/index.html
    - Read all documents, chapters 1-6.
  - https://developer.android.com/privacy-and-security/security-tips
    - Read all documents under "For app developers" and the linked resources.
