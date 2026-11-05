# Releasing

Releases are built by CI only. A version tag `vX.Y.Z` runs `.github/workflows/release.yml`:

1. The build workflow builds the four language server executables from the pinned angel-lsp commit, then builds,
   tests and verifies the plugin ZIP with them.
2. The `sign` job signs that ZIP, without building it again, and records its SHA-256 in `SHA256SUMS`.
3. The `release` job attaches the signed ZIP and `SHA256SUMS` to a draft GitHub release. It is the only job with
   write access to the repository.

Publishing to JetBrains Marketplace is a separate, manually started workflow (`publish.yml`). It downloads the
signed ZIP from the GitHub release, checks it against `SHA256SUMS`, and uploads that file. The first releases go to
the `beta` channel.

## Repository setup

- Environment `release` holds `CERTIFICATE_CHAIN`, `PRIVATE_KEY` and `PRIVATE_KEY_PASSWORD`.
- Environment `marketplace` holds `PUBLISH_TOKEN` and requires a reviewer.
- The Windows server executable is not code-signed yet; injecting the server invalidates Node.js's signature. Signing
  through SignPath belongs in the `server` job of `build.yml`, after the build step on `windows-latest`.

## Before the tag

1. To update the language server, set `angelLspCommit` in `gradle/libs.versions.toml`, and `angelLsp` and
   `nodeRuntime` to the server version and Node.js version at that commit.
2. Run `./gradlew updateNotices -PangelLspServerDir=<server/dist>` and commit `THIRD-PARTY-NOTICES.txt`.
3. If the server changed, compare its capabilities with the LSP4IJ features registered in `plugin.xml`, and the
   semantic token types it sends with `SemanticTokenKeys`.
4. Move the `Unreleased` section of `CHANGELOG.md` to the new version, and set `pluginVersion` in
   `gradle.properties`.

## Manual checks

Use Rider 2026.1 with a clean configuration, the sample project in `src/test/testData/sample`, and the ZIP from CI.

Installation:

- [ ] Install from disk, then uninstall, without a restart prompt.
- [ ] `main.as` and `as.predefined` show their icons and file types; adding `*.angelscript` under File Types opens
      such files as AngelScript.
- [ ] Help | Diagnostic Tools | Analyze Plugin Startup Performance shows no time for the plugin.
- [ ] On macOS, `codesign -v` passes on both bundled executables in the installed plugin.

Language server, each once on `main.as`:

- [ ] Diagnostics, completion after `.` and `::`, hover, go to declaration, find usages, rename, inlay hints, quick
      fixes, Reformat Code, reformat on `;` and `}`, parameter info, structure view.
- [ ] The Language Servers tool window shows one AngelScript server for the project.
- [ ] Killing the server process from the OS leads to a restart, after which semantic colors return.
- [ ] With the bundled executable renamed: one balloon, no exception dialog, lexical colors present. Setting the
      server executable to the renamed file starts the server without reopening the project, and setting it back to
      empty restarts it.

Semantic colors:

- [ ] Class, method, parameter, local, field, enum, enum member and namespace names are colored; after typing stops,
      colors line up with the text.
- [ ] With a C++ file and an AngelScript file side by side, in the default dark and light schemes: class, member
      function, local, parameter, namespace, enum, field, global function and macro names match.
- [ ] Closing the file removes its highlighters.

Settings:

- [ ] With LSP4IJ tracing set to verbose, changing the string type shows one `workspace/didChangeConfiguration`,
      then the server's `workspace/configuration` request answered with the new value, and diagnostics update.
- [ ] Turning on digit separators re-highlights `1'000` in open editors without reopening them.
- [ ] Creating `.angelscript.json` with an invalid value shows one balloon listing the key, and the settings page
      shows "Set by .angelscript.json" for its keys.
- [ ] In an untrusted project, the file is not read and the server does not start until the project is trusted.

Unloading:

- [ ] Open the sample project and files, unload the plugin, and check with the platform's unload diagnostics that
      no plugin classes are retained.

## After the release

- [ ] Publish the draft GitHub release, then run the publish workflow for the tag.
- [ ] Check the Marketplace page: description, change notes, the beta channel.
