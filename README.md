# Mockatcha

Mockatcha provides Mockito-like mocking for TeaVM and Java-based browser testing
for web applications built with any stack. Mockatcha Core generates mocks during
TeaVM compilation. Companion modules cover browser-focused BDD, DOM testing,
and complete application tests.

The Webapp Testkit lets you write tests in Java for an application built with
JavaScript, TypeScript, Java, or any other web stack. TeaVM compiles the tests
to run in a browser; the application itself does not need to use Java or TeaVM.

Use Mockatcha for tests that depend on browser APIs, browser-only collaborators,
or JavaScript behaviour produced by TeaVM. Keep portable domain logic on the JVM
when Mockito can test it faster and with its full API.

## Documentation

Start with the [documentation index](docs/README.md), or go directly to a
chapter:

1. [Getting started](docs/getting-started.md) — choose where to run a test,
   configure TeaVM, and write a first Mockatcha test.
2. [Stubbing](docs/stubbing.md) — return values, argument matchers, calculated
   answers, exceptions, and consecutive results.
3. [Verification](docs/verification.md) — calls, counts, order, argument
   capture, and failure diagnostics.
4. [Mocks and spies](docs/mocks-and-spies.md) — class constraints, partial
   doubles, safe spy setup, and BDD aliases.
5. [Strictness and lifecycle](docs/strictness-and-lifecycle.md) — unused stubs,
   strict calls, JUnit rules, sessions, and reset operations.

## Minimal setup

Add Mockatcha Core to the test classpath:

```xml
<dependency>
  <groupId>io.instanto</groupId>
  <artifactId>mockatcha-core</artifactId>
  <version>0.1.0-SNAPSHOT</version>
  <scope>test</scope>
</dependency>
```

A browser test uses TeaVM's runner and skips the JVM. Its body follows the
familiar arrange, act, assert pattern:

```java
@RunWith(TeaVMTestRunner.class)
@SkipJVM
public class GreetingComponentTest {
    @Test
    public void showsTheGreetingFromTheService() {
        GreetingService greetings = mock(GreetingService.class);
        when(greetings.hello("Ada")).thenReturn("Hello Ada");

        String shown = new GreetingComponent(greetings).greet("Ada");

        assertEquals("Hello Ada", shown);
        verify(greetings).hello("Ada");
    }
}
```

The [getting-started chapter](docs/getting-started.md) contains the complete
example, imports, TeaVM dependencies, and Surefire configuration.

## Modules

| Module | Documentation |
| --- | --- |
| `mockatcha-core` | Mockito-style mocks, spies, verification, and lifecycle support. Start with the [core guide](docs/README.md). |
| `mockatcha-bdd` | [Browser clock, call logs, value matchers, and method-name stubbing](mockatcha-bdd/README.md). |
| `mockatcha-dom` | [Accessible DOM queries, interactions, waiting, and assertions](mockatcha-dom/README.md). It can be used without `mockatcha-core`. |
| `mockatcha-webapp-testkit` | [Testing any built web application from a TeaVM Java test](mockatcha-webapp-testkit/README.md). The application itself does not need to use TeaVM. |
| `mockatcha-examples` | [Executable examples](mockatcha-examples/README.md). |
| `teavm-rule-support` | [JUnit `TestRule` support for TeaVM](teavm-rule-support/README.md). It can be used outside Mockatcha. |

## Examples

The examples module tests a `TimesheetService` with mocked storage and approval
boundaries, a concrete-class spy, and a browser canvas. Its
[reading order and run command](mockatcha-examples/README.md) cover the public
API from basic stubbing through browser-specific tests.

## Build this repository

Maintainers preparing a Maven Central release should follow the
[release guide](docs/releasing.md). Ordinary builds do not sign or publish files.

Requirements: Java 21, Maven, and Chrome or Chromium. The build passes
`teavm.junit.js.runner=browser-chrome` to `TeaVMTestRunner`. This tells TeaVM to
compile each browser test to JavaScript and launch it with the external command
`chrome`; it does not download or embed a browser. That command must therefore
resolve to an installed Chrome or Chromium executable on `PATH` for both local
and CI builds. See [Getting started](docs/getting-started.md#choose-and-install-the-browser-runner)
for the runner settings and alternatives.

```bash
mvn clean test
```

Run the DOM suite in Firefox:

```bash
mvn -pl mockatcha-dom -am -Dmockatcha.test.browser=browser-firefox test
```

Run only the examples and their dependencies:

```bash
mvn -pl mockatcha-examples -am test
```

## License

Mockatcha is licensed under the [Apache License 2.0](LICENSE).

## Support TeaVM and Sarto

Mockatcha is part of the Sarto Java ecosystem and is built on
[TeaVM](https://github.com/konsoletyper/teavm). If Mockatcha supports your work,
please consider funding the projects behind it:

- [Sponsor TeaVM](https://github.com/sponsors/konsoletyper) to support the
  compiler that brings Java to the browser.
- [Sponsor Sarto](https://github.com/sponsors/instanto-io) to support Mockatcha
  and the other Sarto libraries.
