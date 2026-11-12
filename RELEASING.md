# Releasing

Releases are built and published by CI only. Pushing a version tag `vX.Y.Z` runs
`.github/workflows/release.yml`; no other approval is needed:

1. The `version` job stops the run unless the tag matches `pluginVersion`.
2. The build workflow builds the four language server executables from the pinned angel-lsp commit, then builds,
   tests and verifies the plugin ZIP with them.
3. The `sign` job signs that ZIP, without building it again, and records its SHA-256 in `SHA256SUMS`.
4. The `release` job publishes a GitHub release with the signed ZIP and `SHA256SUMS`, using the version's section of
   `CHANGELOG.md` as notes, and fails if that section is missing. It is the only job with write access to the
   repository.
5. The `publish` job runs `publish.yml`, which downloads the signed ZIP from the release, checks it against
   `SHA256SUMS`, and uploads that file to JetBrains Marketplace.

The tag is the go-ahead, so finish the steps below and the manual checks before pushing it. If the upload fails,
re-run the failed jobs of the release run, or start `publish.yml` on the tag:
`gh workflow run publish.yml --ref vX.Y.Z -f tag=vX.Y.Z`.

## Repository setup

- Environment `release` holds `CERTIFICATE_CHAIN`, `PRIVATE_KEY` and `PRIVATE_KEY_PASSWORD`, and accepts only `v*`
  tags as deployment refs. `PRIVATE_KEY` holds the decrypted key: the signer fails with "pad block corrupted" on a
  key encrypted by OpenSSL 3, even with the right password.
- Environment `marketplace` holds `PUBLISH_TOKEN`, has no required reviewer, and accepts only `v*` tags as
  deployment refs.
- The tag ruleset "release tags" covers `v*`: it restricts creating, updating, deleting and force-pushing those tags,
  with Repository admin allowed to bypass. Pushing a release tag therefore takes an admin.
- The Windows server executable is not code-signed yet; injecting the server invalidates Node.js's signature, so the
  added signature will be the only one. Signing with Plumfire LLC's Azure code-signing certificate belongs in the
  `server` job of `build.yml`, after the build step on `windows-latest`.

## Before the tag

1. To update the language server, set `angelLspCommit` in `gradle/libs.versions.toml`, and `angelLsp` and
   `nodeRuntime` to the server version and Node.js version at that commit.
2. Run `./gradlew updateNotices -PangelLspServerDir=<server/dist>` and commit `THIRD-PARTY-NOTICES.txt`.
3. If the server changed, compare its capabilities with the LSP4IJ features registered in `plugin.xml`, and the
   semantic token types it sends with `SemanticTokenKeys`.
4. Move the `Unreleased` section of `CHANGELOG.md` to the new version, update the compare links at its end, and set
   `pluginVersion` in `gradle.properties`.

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

Oldest supported IDE:

- [ ] With `./gradlew runIdeLowest`, open the sample project, open `main.as` and check semantic colors, hover and
      completion. Change a setting on the settings page, apply it, restart the IDE, and check that it was kept.

Unloading:

- [ ] Open the sample project and files, unload the plugin, and check with the platform's unload diagnostics that
      no plugin classes are retained.

## After the release

- [ ] Check that the release run finished, including the `publish` job.
- [ ] Once JetBrains approves the upload, check the Marketplace page: description and change notes.
