# Mockatcha documentation

This index separates the core guide from the module-specific documentation.

## Core guide

Read the chapters in order for a first project. Each chapter also works as a
standalone reference.

| Chapter | Covers |
| --- | --- |
| [1. Getting started](getting-started.md) | Test placement, TeaVM configuration, compile-time mock generation, and a first test. |
| [2. Stubbing](stubbing.md) | Exact values, matchers, answers, exceptions, and consecutive results. |
| [3. Verification](verification.md) | Calls, invocation counts, order, exhaustive checks, diagnostics, and argument capture. |
| [4. Mocks and spies](mocks-and-spies.md) | Interface and class mocks, partial doubles, spy setup, and BDD aliases. |
| [5. Strictness and lifecycle](strictness-and-lifecycle.md) | Unused stubs, strict calls, rules, compatibility support, sessions, and reset operations. |

## Browser testing modules

| Guide | Use it for |
| --- | --- |
| [BDD helpers](../mockatcha-bdd/README.md) | Method-name stubbing, call logs, additional matchers, and controlled browser time. |
| [DOM testing](https://github.com/instanto-io/webapp-testkit) | Accessible queries, interactions, asynchronous rendering, assertions, and frames. |
| [Webapp testkit](https://github.com/instanto-io/webapp-testkit) | Extending Mockatcha DOM and TeaVM's test runner to applications built with any web stack. |
| JUnit rules for TeaVM (`io.instanto:teavm-rule-support`) | Running ordinary JUnit `TestRule` fields and methods through TeaVM. |

## Examples

The [core examples guide](../mockatcha-examples/README.md) links to executable
tests for stubbing, verification, spies, strict lifecycle support, and canvas
code. The
plain JavaScript webapp
is exercised by the [webapp testkit](https://github.com/instanto-io/webapp-testkit)'s
own tests and has no TeaVM dependency of its own.

[Back to the project README](../README.md)

## Maintaining a release

The [release guide](releasing.md) covers Maven Central requirements, signing,
local bundle checks and publication approval.
