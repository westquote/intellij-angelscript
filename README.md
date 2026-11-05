# AngelScript Support

<!-- Plugin description -->
AngelScript Support adds the [AngelScript](https://www.angelcode.com/angelscript/) scripting language to JetBrains
Rider and other IDEs built on the IntelliJ Platform.

- Syntax highlighting, brace matching, commenting and indentation for `.as` files and `as.predefined` declaration
  files.
- Code completion, diagnostics, hover, navigation, find usages, rename, inlay hints, quick fixes, formatting,
  parameter info and the structure view, provided by the bundled
  [angel-lsp](https://github.com/sashi0034/angel-lsp) language server through
  [LSP4IJ](https://plugins.jetbrains.com/plugin/23257-lsp4ij).
- Semantic colors for types, functions, variables and other names, which follow the IDE's C++ colors in Rider.
- Project settings for the engine's string and array types, language features, include paths and defined symbols,
  shared with the team through an `.angelscript.json` file.
<!-- Plugin description end -->

## Requirements

- Rider 2025.1 or later, or another IntelliJ Platform IDE of build 251 or later.
- The [LSP4IJ](https://plugins.jetbrains.com/plugin/23257-lsp4ij) plugin, version 0.21.0 or later. The IDE offers to
  install it with this plugin.
- Windows x64, macOS (Apple silicon or Intel) or Linux x64 for the bundled language server. On other platforms, set
  a server executable in the settings (see below).

## Installation

Install AngelScript Support from **Settings | Plugins | Marketplace**. To install a downloaded ZIP, use
**Settings | Plugins | Install Plugin from Disk**. No restart is needed.

The plugin ZIP contains the language server for all four supported platforms. The download is about 135 MB and the
installed size is about 365 MB.

## Files

Files with the extension `.as` open as AngelScript. Files named `as.predefined` or `*.as.predefined` hold the
declarations that the host application registers with the engine, so that the language server knows them.

To open other extensions as AngelScript, add a pattern such as `*.angelscript` to the AngelScript file type under
**Settings | Editor | File Types**.

In IDEs with the JavaScript or Flex plugins, ActionScript also claims `*.as`. To use AngelScript for those files,
remove `*.as` from the ActionScript file type and add it to the AngelScript file type under
**Settings | Editor | File Types**.

## Settings

**Settings | Languages & Frameworks | AngelScript** holds the language server settings of the project:

- **Language**: the string type that string literals construct (for example `FString` in an engine that registers
  it), the array type that `T[]` denotes, whether virtual property accessors need the `property` keyword, and
  switches for `foreach`, typed enumerations, digit separators, character literals and non-ASCII identifiers.
- **Preprocessor and includes**: symbols defined for `#if`, include directories, and predefined files included in
  every script.
- **Analysis**: whether analyzer errors are reported as warnings, whether all project files see each other without
  `#include`, enum scope hoisting for completion, and glob patterns of files the server ignores.
- **Completion** and **Formatting**: keyword and snippet completion, blank lines and indentation.
- **Server**: the path of a language server executable to use instead of the bundled one. This setting applies to
  this machine only and is not stored in the project.

The defaults are those of stock AngelScript. The settings are stored in the project's `.idea` directory.

### `.angelscript.json`

A file named `.angelscript.json` in the project root sets the same keys for everyone who opens the project. Its
values take precedence over the settings page. When the file exists, the settings page shows its values and saves
changes into it, rewriting the file with two-space indentation. Keys that are not in the file keep their page
values.

```json
{
  "builtinStringType": "FString",
  "explicitPropertyAccessor": true,
  "definedSymbols": ["EDITOR"],
  "includePath": ["Script/Shared"],
  "formatter": { "indentSpaces": 4 }
}
```

The keys are those of the settings page: `builtinStringType`, `builtinArrayType`, `explicitPropertyAccessor`,
`supportsForEach`, `supportsTypedEnumerations`, `supportsDigitSeparators`, `characterLiterals`,
`allowUnicodeIdentifiers`, `definedSymbols`, `includePath`, `forceIncludePredefined`, `implicitMutualInclusion`,
`hoistEnumParentScope`, `suppressAnalyzerErrors`, `completion.builtinKeywords`, `completion.snippets`,
`files.exclude`, `files.angelScript` (extra file patterns for the server), `formatter.maxBlankLines`,
`formatter.indentSpaces` and `formatter.useTabIndent`. Keys with a dot are written as nested objects.

Relative paths in `includePath` and `forceIncludePredefined` are relative to the file's directory. The file cannot
name a server executable, its arguments or its environment. The IDE reads it again when it changes, and does not
read it in a project that is not trusted.

If the file is not valid JSON, it is ignored and a notification shows the parse error. Unknown keys and values of
the wrong type are skipped and reported in a notification.

## Colors

**Settings | Editor | Color Scheme | AngelScript** lists every color the plugin uses, under **Lexical** for colors
from the editor's own highlighting and **Semantic** for colors from the language server.

In Rider, names fall back to the colors of the matching C++ names (class, enum, enumerator, template parameter,
local variable, field, global function and macro), so AngelScript looks like C++ in the same scheme. Other names,
and all names in IDEs without C++ colors, fall back to the IDE's general language colors. Set a color on the
AngelScript page to override the fallback.

## Troubleshooting

### Language server not found

The plugin shows a notification such as "No bundled AngelScript language server for linux-arm64" when it cannot
start the server. Common causes:

- The platform has no bundled server. Build or download angel-lsp for the platform and set its path under
  **Settings | Languages & Frameworks | AngelScript | Server executable**.
- The bundled server is missing or cannot be made executable. Reinstall the plugin. A plugin directory on a
  read-only file system also causes this.
- The server executable setting does not point to an executable file. Correct the path; the server restarts with
  it as soon as the setting is applied.

Highlighting, brace matching and the other editor features work without the server.

### Colors not showing

- Open **View | Tool Windows | Language Servers** and check that "AngelScript Language Server" is running for the
  project. If it is not running, check its log in that tool window.
- Check **Settings | Editor | Color Scheme | AngelScript**: a color set to the default text color looks unpainted.
- Check that the file opens as AngelScript (the file type in the status bar). If another plugin owns `*.as`, see
  [Files](#files).

## Development

See [CONTRIBUTING.md](CONTRIBUTING.md) for building and testing the plugin, and [RELEASING.md](RELEASING.md) for
releases.

## License

MIT. See [LICENSE](LICENSE). The bundled language server and its components are covered by the licenses in
[THIRD-PARTY-NOTICES.txt](THIRD-PARTY-NOTICES.txt).
