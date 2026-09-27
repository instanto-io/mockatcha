# Mockatcha

Mockatcha provides Mockito-style mocking through one API that runs in two
places: a JVM test, and a test that TeaVM compiles. The API is the same in both.
`mock`, `spy`, `when`, `verify`, the argument matchers, sessions and the JUnit
rule behave alike, because both platforms call the same runtime.

They differ only in how a mock is made. TeaVM compiles the whole program ahead
of time, so Mockatcha generates each mock while TeaVM compiles the test. On a
JVM it makes one when the test asks: an interface becomes a `Proxy`, and a class
gets a subclass generated at that moment.

## Write a first test

Use Mockatcha Core with JUnit 4. A TeaVM browser test also needs TeaVM's
JUnit runner. The [getting-started guide](docs/getting-started.md) shows the
current setup for each runtime.

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

## Where a test can run

- **A JVM**, for code that needs no browser.
- **A browser**, through TeaVM and its JUnit runner, for code that uses timers,
  the DOM, canvas, storage, or another browser API.
- **Other JavaScript runtimes**, including edge runtimes. TeaVM code and its tests
  can run in any JavaScript runtime that supports the APIs they use. Sarto Edge's
  `MiniflareTestRunner` uses this to test edge services in Miniflare.

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

## Explore the modules

| Module | Use it for |
| --- | --- |
| `mockatcha-core` | Mocks, spies, verification and lifecycle support. |
| `mockatcha-bdd` | [Call logs, value matchers and method-name stubbing](mockatcha-bdd/README.md). |
| `mockatcha-examples` | [Executable examples](mockatcha-examples/README.md). |

[Webapp Testkit](https://github.com/instanto-io/webapp-testkit) provides DOM
queries and browser interactions; [TeaVM rule support](https://github.com/instanto-io/teavm-rule-support)
applies JUnit rules in TeaVM tests. Both are separate libraries.

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

## Examples

The examples module tests a `TimesheetService` with mocked storage and approval
boundaries, a concrete-class spy, and a browser canvas. Its
[reading order and run command](mockatcha-examples/README.md) cover the public
API from basic stubbing through browser-specific tests.

## Credits

Thank you to the people behind the projects whose ideas inform Mockatcha:

- [Mockito](https://site.mockito.org/) for the mocking, stubbing, spies, argument
  matching, and verification APIs that Mockatcha Core follows.
- [Jasmine](https://jasmine.github.io/) for the browser-testing style behind the
  BDD helpers, call inspection, and value matchers.

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
