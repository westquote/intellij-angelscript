# Changelog

All notable changes to this project are documented in this file. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and the project uses
[Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Fixed

- Rider no longer reports a "Class initialization must not depend on services" error from the plugin. Rider's C++
  colors now apply whenever its C++ support is enabled, whichever color scheme is active.
- The file type for `as.predefined` files is listed as "AngelScript Predefined" instead of a second "AngelScript".

## [0.1.1] - 2026-11-10

### Fixed

- A call to a method that a subclass overrides, or that a class also gets from an interface, no longer shows a false
  "Multiple matching signatures" warning.
- The plugin no longer uses IntelliJ Platform API that is internal or deprecated in IntelliJ IDEA 2026.3.

## [0.1.0] - 2026-11-05

### Added

- AngelScript file types for `.as` and `as.predefined` files.
- Syntax highlighting, brace matching, commenting, quote pairing and indentation on Enter.
- The angel-lsp language server, bundled for Windows x64, macOS and Linux x64, started through LSP4IJ.
- Semantic colors from the language server, with C++ color fallbacks in Rider.
- Project settings for the language server, and an `.angelscript.json` file to share them.
- A machine-level setting for a custom language server executable.

[Unreleased]: https://github.com/westquote/intellij-angelscript/compare/v0.1.1...HEAD
[0.1.1]: https://github.com/westquote/intellij-angelscript/compare/v0.1.0...v0.1.1
[0.1.0]: https://github.com/westquote/intellij-angelscript/commits/v0.1.0
