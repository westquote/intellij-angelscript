# Contributing

## Building

You need JDK 21. Gradle and all plugins are pinned; the wrapper downloads Gradle and checks its SHA-256.

```
./gradlew test                  # unit and platform tests; works on a fresh clone
./gradlew runIde                # IntelliJ IDEA with the plugin in a sandbox
./gradlew build                 # compile, test, ktlint, detekt and the plugin ZIP
./gradlew verifyPlugin          # Plugin Verifier against Rider 2026.1, IDEA 2026.1 and IDEA 2025.1
```

The plugin ZIP bundles the [angel-lsp](https://github.com/westquote/angel-lsp) language server as a standalone
executable per platform. CI builds all four from the commit pinned as `angelLspCommit` in
`gradle/libs.versions.toml`. To build the one for your own platform, use the Node.js version pinned there as
`nodeRuntime` (the build embeds the Node.js that runs it, and the plugin build checks the version in the server's
notices):

```
git clone https://github.com/westquote/angel-lsp.git
cd angel-lsp
git checkout <angelLspCommit>
npm ci
npm ci --prefix server
npm run standalone --prefix server
```

The executable and its `THIRD-PARTY-NOTICES.txt` are written to `server/dist`. Pass that folder to the build:

```
./gradlew build -PangelLspServerDir=/path/to/angel-lsp/server/dist
```

A local build packages the executables it finds and warns about the others. CI passes `-PangelLspAllPlatforms=true`,
which fails the build if one is missing.

The plugin is compiled and tested against IntelliJ IDEA 2026.1 rather than Rider. Rider's distribution is several
times larger and its test framework adds nothing that this plugin uses, because the plugin depends only on the
platform and LSP4IJ. The Plugin Verifier checks the result against Rider 2026.1, IntelliJ IDEA 2026.1 and IntelliJ
IDEA 2025.1, the lowest supported build.

## Code

- Kotlin, following the [Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html). ktlint
  (style `intellij_idea`, configured in `.editorconfig`) and detekt (`config/detekt.yml`) run in CI.
- User-visible strings go in `messages/AngelScriptBundle.properties`.
- Nothing runs at IDE or project startup: use services, listeners declared in `plugin.xml`, and the coroutine scope
  the platform injects into a service.
- The plugin bundles no libraries. A new dependency needs a reason in its pull request.

## Accepted API uses

The Plugin Verifier fails the build on compatibility problems and internal API use. It also reports the experimental
API uses below, which are accepted for the reasons given. Any other experimental API needs a reason here.

| API | Reason |
|---|---|
| LSP4IJ `LSPClientFeatures`, `LSPSemanticTokensFeature` | The way the LSP4IJ developer guide gives to configure features. The plugin uses it to turn off LSP4IJ's own semantic token requests, since it paints the tokens itself. |
| Platform `TrustedProjectsListener` | The only notification that a project became trusted, which the plugin needs to read `.angelscript.json` and start the server then. |
| Platform `com.intellij.ide.impl.isTrusted` (deprecated in 2026.1) | Its replacement, `TrustedProjects.isProjectTrusted`, is internal API in 2025.1, the lowest supported build. |

## Tests

Unit tests and platform tests (`BasePlatformTestCase`) use JUnit 4, which the platform test framework requires. Test
data lives under `src/test/testData`. `PerformanceTest` checks time budgets for the lexer and the semantic token
decoder.

## Commits and pull requests

Commit messages follow [Conventional Commits](https://www.conventionalcommits.org/): `feat:`, `fix:`, `docs:`,
`build:`, `test:`, `refactor:` or `chore:`, then a short imperative summary. Keep one logical change per commit.
Add user-visible changes to the `Unreleased` section of `CHANGELOG.md`.
