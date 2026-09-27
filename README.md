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

## A few things to know

**Interfaces are straightforward to mock.** For a class, Mockatcha creates a
subclass and calls its no-argument constructor. The class must not be `final`,
and static, private and final methods keep their real behaviour. For example,
`mock(PriceService.class)` needs a non-final `PriceService` with a
no-argument constructor. See [mocks and spies](docs/mocks-and-spies.md) for
the full class rules.

**TeaVM needs the type written directly in the test.** Use
`mock(PriceService.class)` or `spy(PriceService.class, realService)`.
Mockatcha generates the mock while TeaVM compiles the test, so it cannot choose
the type from a `Class` variable at runtime. If a class cannot be mocked, TeaVM
reports it during compilation; a JVM reports it when the test runs.

**Keep each test's mocks separate.** In a JUnit test, `MockatchaRule` checks
and clears mock state after the test:

```java
@Rule
public MockatchaRule mockatcha = new MockatchaRule();
```

TeaVM tests need [TeaVM rule support](https://github.com/instanto-io/teavm-rule-support)
to run JUnit rules. If that is unavailable, extend `StrictTest` instead.
For other test harnesses, use `MockatchaSession`. On a JVM using named Java
modules, open the mocked class's package to Mockatcha or mock an interface;
ordinary classpath tests need no extra setting. See
[strictness and lifecycle](docs/strictness-and-lifecycle.md) for cleanup
choices, including `releaseMocks()`.

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
