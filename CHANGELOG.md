# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project adheres to
[Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Fixed

- A service that registers no interceptor logged `registered 0 interceptor(s): ` with a colon and nothing after it. It logs
  `registered no interceptors`.

## [1.1.0] - 2026-10-06

### Added

- `borba.interceptors.component/build`, which builds every registered interceptor into a map, so a service or a test can do it
  without starting a system.
- A check of every interceptor when it is built: it must be a map with at least one of `:enter`, `:leave` and `:error`, each a
  function or a var. A map that is not valid fails the start naming its keyword and the reason, instead of failing on a request.
- The component logs the interceptors it registered.
- A test suite with 100% coverage.

### Changed

- A `:default` method of the registry is no longer built as an interceptor.
- The component no longer defines a `halt-key!`, which did nothing.
- Moves to Integrant 1.0, where a reference must be a qualified keyword.
- The published library is named `io.github.af2b/borba-interceptors-component`.

## [1.0.0] - 2026-03-30

First release: the `borba.interceptors.registry/interceptor` multimethod and the `:service/interceptors` Integrant component.

[Unreleased]: https://github.com/AF2B/borba-interceptors-component/compare/v1.1.0...HEAD
[1.1.0]: https://github.com/AF2B/borba-interceptors-component/compare/v1.0.0...v1.1.0
[1.0.0]: https://github.com/AF2B/borba-interceptors-component/releases/tag/v1.0.0
