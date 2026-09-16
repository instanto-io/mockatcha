# Mockatcha

Mockatcha provides Mockito-style mocking through one API that runs in two
places: a JVM test, and a test that TeaVM compiles. The API is the same in both.
`mock`, `spy`, `when`, `verify`, the argument matchers, sessions and the JUnit
rule behave alike, because both platforms call the same runtime.

They differ only in how a mock is made. TeaVM compiles the whole program ahead
of time, so Mockatcha generates each mock while TeaVM compiles the test. On a
JVM it makes one when the test asks: an interface becomes a `Proxy`, and a class
gets a subclass generated at that moment.

## Where a test can run

- **A JVM**, for code that needs no browser.
- **A browser**, through TeaVM and its JUnit runner, for code that uses timers,
  the DOM, canvas, storage, or another browser API.
- **Another runtime that runs TeaVM output.** Mockatcha is part of the compiled
  program rather than something the host provides, so it runs where that program
  runs. Tests for Cloudflare Workers run this way today, under a Miniflare
  runner. Whether any other host works is a question about running TeaVM output
  there, not about Mockatcha.

## Limitations

On a JVM:

- Mocking a class needs its package open to Mockatcha, which is the case on the
  class path and not inside a strong module.
- A type that cannot be subclassed, such as a final class or one without a
  no-argument constructor, is rejected when the test runs.
- Mocking state is held per thread. Mocks a test creates outside a session stay
  on that thread until something checks or releases them, because one process
  runs the whole suite. See `releaseMocks` in
  [Strictness and lifecycle](docs/strictness-and-lifecycle.md).

Under TeaVM:

- The class passed to `mock` or `spy` must be a class literal, since the mock is
  generated while the program compiles.
- A type that cannot be subclassed is reported while TeaVM compiles, rather than
  when the test runs.
- JUnit rules need the `teavm-rule-support` module. `StrictTest` is the fallback
  where that cannot be installed.

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

Add Mockatcha Core to the test classpath, alongside JUnit 4, which Mockatcha
uses and does not supply:

```xml
<dependency>
  <groupId>io.instanto</groupId>
  <artifactId>mockatcha-core</artifactId>
  <version>0.1.0-SNAPSHOT</version>
  <scope>test</scope>
</dependency>
```

A test that runs through TeaVM also needs `org.teavm:teavm-classlib` and
`org.teavm:teavm-junit`. The
[getting-started chapter](docs/getting-started.md) lists both classpaths in full.

On a JVM the test is an ordinary JUnit test:

```java
public class GreetingServiceTest {
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

The same body runs in a browser through TeaVM's runner. Only the runner and the
`@SkipJVM` annotation differ, and `@SkipJVM` is what keeps a browser-only test
off the JVM:

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
| `mockatcha-bdd` | [Call logs, value matchers, and method-name stubbing](mockatcha-bdd/README.md). |
| Webapp Testkit | [Testing web applications in a real browser through TeaVM](https://github.com/instanto-io/webapp-testkit). A separate library; it does not depend on Mockatcha. |
| `mockatcha-examples` | [Executable examples](mockatcha-examples/README.md). |
| `teavm-rule-support` | JUnit `TestRule` support for TeaVM, now a separate library. It can be used outside Mockatcha. |

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

## Credits

Thank you to the people behind the projects whose ideas inform Mockatcha:

- [Mockito](https://site.mockito.org/) for the mocking, stubbing, spies, argument
  matching, and verification APIs that Mockatcha Core follows.
- [Jasmine](https://jasmine.github.io/) for the browser-testing style behind the
  BDD helpers, call inspection, and value matchers.
- [Testing Library](https://testing-library.com/) for finding and testing page
  elements through the roles, labels, and text available to users.
- [jasmine-jquery](https://github.com/velesin/jasmine-jquery) and
  [jest-dom](https://github.com/testing-library/jest-dom) for the DOM assertion
  vocabulary used in Mockatcha DOM.

Mockatcha also builds on [TeaVM](https://teavm.org/), which compiles the Java
tests and provides the browser runner, and [JUnit](https://junit.org/junit4/),
which supplies the Java test structure, assertions, and lifecycle. Thank you to
their maintainers and contributors for making this work possible.

## License

Mockatcha is licensed under the [Apache License 2.0](LICENSE).

## Support Mockatcha and TeaVM

If Mockatcha helps your work, please consider supporting its development and
the compiler it builds on:

- [Support Mockatcha](https://github.com/sponsors/instanto-io).
- [Support TeaVM](https://github.com/sponsors/konsoletyper), the compiler that
  brings Java to the browser.
