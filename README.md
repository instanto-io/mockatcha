# Mockatcha

Tests are easiest to understand when they exercise real application code while
replacing only its external boundaries. A service can then be tested without
opening a database, making a network request, or starting a complete
application.

On the JVM, libraries such as Mockito can create those replacements by
generating or changing bytecode while a test is running. TeaVM prepares Java
code ahead of time for JavaScript, so that runtime machinery is not available
in a browser test.

Mockatcha provides familiar Mockito-style mocking for tests compiled with
TeaVM. It generates each mock while TeaVM compiles the test, then uses a small
Java runtime to record calls and return the configured answers.

This guide starts with one small test and introduces stubbing, matching, and
verification as they become useful. The
[`mockatcha-examples`](mockatcha-examples) module contains a complete worked
example.

## Why Mockatcha exists

A TeaVM library should be testable through the same public Java API that an
application uses in the browser. Without a TeaVM-compatible mocking library,
its tests must usually do one of the following:

- run only on the JVM and leave the generated browser behaviour untested;
- maintain handwritten replacements for every service boundary; or
- start much more of the application than the class under test needs.

Mockatcha supplies the small replacement objects. The class under test remains
ordinary Java, and the test runs in a real browser through TeaVM's test runner.

## Principles

- **Keep the Mockito shape familiar.** Java developers should recognise
  `mock`, `when`, `thenReturn`, and `verify` without learning another testing
  language.
- **Generate before the test runs.** Mock implementations are prepared by
  TeaVM rather than created through runtime bytecode changes.
- **Mock boundaries, not application logic.** A test uses real domain and
  service classes while replacing storage, transport, clocks, or similar
  collaborators.
- **Keep browser tests small.** Mockatcha does not start CDI, a web server, or
  an application framework.
- **Stay independent of Sarto and Verrai.** Any Java library tested with TeaVM
  can use Mockatcha.
- **Make the current boundary honest.** Mockatcha mocks interfaces and ordinary
  classes. It does not pretend to support static methods, constructors, or
  final classes.

## Contents

| Area | Jump to |
| --- | --- |
| Understand it | [Why it exists](#why-mockatcha-exists) · [Principles](#principles) · [Three core ideas](#the-three-core-ideas) |
| Start using it | [First browser test](#your-first-browser-test) |
| Describe behaviour | [Return values](#choose-what-a-mock-returns) · [Flexible arguments](#match-flexible-arguments) · [Answers and failures](#calculate-an-answer-or-report-a-failure) · [Changing results](#return-different-results-over-time) |
| Check behaviour | [Verify calls](#verify-important-calls) · [Call history](#inspect-or-clear-call-history) |
| Keep real behaviour | [Mock a class](#mock-an-ordinary-class) · [Spy on an object](#keep-real-behaviour-with-a-spy) · [Arrange before calling](#arrange-an-answer-before-the-call-runs) |
| See it in context | [Timesheet example](#the-timesheet-example) · [How generation works](#how-the-mock-is-created) |
| Reference | [Current coverage](#current-coverage) · [Modules](#choose-modules) · [Maven setup](#add-mockatcha-to-a-teavm-test-suite) · [Build](#build-this-repository) |

## The three core ideas

A mock is a generated stand-in for a Java interface or class. It replaces a
collaborator such as a repository or remote service.

Stubbing tells that mock what to return for a particular call. Calls without a
matching stub return Java's usual empty value: `null`, `false`, or zero.

Verification checks whether an important call happened. It is most useful for
observable effects, such as saving a record or sending a notification.

The class being tested is still real. Mockatcha replaces only the collaborators
passed to it.

## Your first browser test

Most tests start with an interface boundary. Suppose a greeting component
obtains its text through this one:

```java
public interface GreetingService {
    String hello(String name);
}
```

A TeaVM test can replace that boundary, choose its response, and exercise the
real component:

```java
import static io.instanto.mockatcha.Mockatcha.mock;
import static io.instanto.mockatcha.Mockatcha.verify;
import static io.instanto.mockatcha.Mockatcha.when;
import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.SkipJVM;
import org.teavm.junit.TeaVMTestRunner;

@RunWith(TeaVMTestRunner.class)
@SkipJVM
public class GreetingComponentTest {

    @Test
    public void displaysTheGreetingReturnedByTheService() {
        GreetingService greetings = mock(GreetingService.class);
        when(greetings.hello("Ada")).thenReturn("Hello Ada");

        GreetingComponent component = new GreetingComponent(greetings);
        String displayed = component.greet("Ada");

        assertEquals("Hello Ada", displayed);
        verify(greetings).hello("Ada");
    }
}
```

`@RunWith(TeaVMTestRunner.class)` compiles and runs the test through TeaVM.
`@SkipJVM` prevents JUnit from also trying to execute the generated mock call
directly on the JVM.

## Choose what a mock returns

Pass the call whose behaviour matters to `when`, then supply its answer:

```java
when(profiles.find("A-17")).thenReturn(new Profile("A-17", "Ada"));
```

The setup call is not kept in the mock's call history. Later calls with the
same method and arguments receive the configured profile.

Calls are matched by method, parameter types, and argument values. Overloaded
methods can therefore be configured independently.

## Match flexible arguments

Sometimes a test cares about the kind of argument rather than one exact value:

```java
when(profiles.find(anyString())).thenReturn(defaultProfile);
```

The first milestone includes `any()`, `anyString()`, `anyInt()`, and `eq(...)`.
If one argument uses a matcher, every argument in that call must use one:

```java
when(calculator.total(anyInt(), eq("EUR"))).thenReturn(42);
```

This rule keeps the intended call unambiguous. Prefer exact values when they
make the behaviour clearer; use a matcher when the exact value is genuinely
not part of the test.

## Calculate an answer or report a failure

Use `thenAnswer` when the result depends on the arguments:

```java
when(greetings.hello(anyString()))
        .thenAnswer(call -> "Hello " + call.argument(0));
```

The supplied `Invocation` contains the mock, stable method identifier, and
arguments for that call.

Use `thenThrow` to describe a boundary failure and test how the real class
responds:

```java
when(profiles.find("A-17"))
        .thenThrow(new IllegalStateException("Profile store unavailable"));
```

## Return different results over time

Several answers can be configured for the same call:

```java
when(status.current())
        .thenReturn("starting")
        .thenReturn("ready");
```

The first call returns `starting`. The second and later calls return `ready`.
Repeating the final answer makes a changing boundary deterministic without
requiring a timing loop in the test.

## Verify important calls

`verify(mock)` expects the following call to have happened once:

```java
verify(repository).save(report);
```

Use `times` when the count is itself part of the behaviour:

```java
verify(repository, times(2)).save(report);
```

Use `never` for an effect that must not happen:

```java
verify(notifications, never()).send(anyString());
```

Matchers work in verification just as they do in stubbing. Verification calls
are not added to the call history.

## Inspect or clear call history

Most tests should prefer `verify`, which expresses the intended behaviour
directly. `mockingDetails` is available when a diagnostic or specialised
assertion needs the complete call history:

```java
MockingDetails details = mockingDetails(repository);
boolean generatedMock = details.isMock();
List<Invocation> calls = details.getInvocations();
```

`clearInvocations(mock)` removes recorded calls but keeps stubbing. `reset(mock)`
removes both calls and stubbing. These operations are useful for a deliberately
shared fixture, although small tests normally create fresh mocks instead.

## Mock an ordinary class

Not every boundary is an interface. `mock` accepts a class as well:

```java
PricingService pricing = mock(PricingService.class);
when(pricing.total("A-17")).thenReturn(42);
```

Mockatcha generates a subclass while TeaVM compiles the test, so the mock really
is a `PricingService` and can be passed anywhere one is expected. Its
no-argument constructor runs; every public and protected method is replaced.

Three limits follow from generating a subclass, and Mockatcha does not hide
them:

- a `final` class, an enum, a record, or a class without a no-argument
  constructor is rejected while TeaVM compiles the test, naming the type and the
  rule it failed;
- `static`, `private`, and `final` methods keep their real behaviour, because a
  subclass cannot override them; and
- `equals`, `hashCode`, and `toString` answer by identity and with a fixed
  description. They are neither recorded nor stubbed, so logging a mock never
  runs real code.

Prefer an interface where the design allows one. Class mocking exists for
boundaries you do not control or have not yet extracted.

## Keep real behaviour with a spy

A mock replaces every method. A spy keeps a real object and replaces only the
calls a test names, which suits a working collaborator better than rebuilding
its behaviour in stubs:

```java
InMemoryTimesheetRepository real = new InMemoryTimesheetRepository();
InMemoryTimesheetRepository timesheets = spy(InMemoryTimesheetRepository.class, real);

when(timesheets.findDraft("A-17")).thenReturn(new Timesheet("A-17", 46));
```

`findDraft` now answers from the stub. Every other call reaches `real` and is
still recorded, so `verify(timesheets).markSubmitted("A-17")` works as usual.

The class is a separate argument because TeaVM must know it while it compiles
the call. An interface and one of its implementations work too:

```java
Notifier notifier = spy(Notifier.class, new EmailNotifier());
```

Two things are worth knowing before relying on a spy:

- the spy is not the object it delegates to, so `==` distinguishes them; and
- a real method that calls another method on itself is not intercepted, because
  `this` inside that method is the delegate. Stubbing `currency()` does not
  change what `summary()` sees when `summary()` calls it.

The second point is the cost of delegation. Where it matters, mock the
collaborator instead of spying on it.

## Arrange an answer before the call runs

`when(spy.currency())` has to evaluate `spy.currency()` to know which call is
being described, which runs the real method once. When that method is slow,
destructive, or unavailable in a browser, arrange the answer first:

```java
doReturn("EUR").when(pricingSpy).currency();
doThrow(new IllegalStateException("offline")).when(pricingSpy).currency();
doAnswer(call -> call.argument(0) + "!").when(pricingSpy).describe(anyString());
doNothing().when(pricingSpy).record("audited");
```

The real method never runs. These work on mocks too, where they are simply an
alternative spelling.

## The timesheet example

The example module tests a real `TimesheetService` while replacing its storage
and approval boundaries. Its tests progress through exact stubbing, positive
and negative verification, matchers, calculated answers, changing results, and
failures.

Start with
[`TimesheetServiceTest`](mockatcha-examples/src/test/java/io/instanto/mockatcha/examples/TimesheetServiceTest.java),
then
[`TimesheetSpyTest`](mockatcha-examples/src/test/java/io/instanto/mockatcha/examples/TimesheetSpyTest.java)
for class mocks and spies. The separate
[example guide](mockatcha-examples/README.md) explains the order and how to run
only that module.

## How the mock is created

`mock(SomeInterface.class)` is handled while TeaVM compiles the test. TeaVM
visits the abstract methods of that interface and Mockatcha supplies an
implementation for each one. The generated method records its arguments and
asks the small Mockatcha runtime for a configured answer.

A class cannot go through that path, because TeaVM's proxy generator supplies
bodies only for methods it finds abstract, and a concrete class has none.
Mockatcha therefore generates the whole subclass with the compiler's own ASM
and submits it to TeaVM. The generated bodies are just as thin: pack the
arguments, ask the runtime, then either call the real object or convert the
answer. The reasoning behind that choice, and the seam that was tried first, is
recorded in the [class mocking and spies design
record](CLASS_MOCKING_AND_SPIES.md).

Nothing generates bytecode in the browser. There is no Java agent, reflective
class-path scan, CDI container, or DOM dependency. This is why Mockatcha can
keep the Mockito-style test API while fitting TeaVM's ahead-of-time build.

The class passed to `mock` or `spy` must be a compile-time constant, as it is in
`mock(ProfileRepository.class)`.

## Current coverage

Mockatcha supports:

- Java interfaces and ordinary classes;
- delegation spies over either;
- object, primitive, and `void` methods;
- overloaded, inherited, protected, and generic bridge methods;
- exact arguments and the initial matcher set;
- returned values, calculated answers, exceptions, and consecutive answers;
- `doReturn`, `doThrow`, `doAnswer`, and `doNothing` for arranging an answer
  before the call runs;
- verification with exact counts and `never`; and
- call-history inspection, clearing, and reset.

Static methods, constructors, final classes, final and private methods,
package-private methods, argument captors, a single-argument `spy(T)`, and
asynchronous verification are later work. Those features should be added only
when they can preserve the same small, predictable TeaVM runtime. The
[design record](CLASS_MOCKING_AND_SPIES.md) explains what each would require.

## Choose modules

| Module | Use it for |
| --- | --- |
| `mockatcha-core` | The public mocking API, compile-time generator, and small runtime. |
| `mockatcha-examples` | Build-checked examples for learning; applications do not depend on it. |

Mockatcha has no Sarto, Verrai, CDI, Mockito, Byte Buddy, or browser DOM
dependency. The only production dependency is TeaVM's metaprogramming API.
Class mocking also uses TeaVM's introspection API and its relocated ASM while
the compiler runs; both are already on the compiler's classpath, so neither is
passed on to a consumer.

## Add Mockatcha to a TeaVM test suite

Add the core library for tests, together with TeaVM's class library and JUnit
runner:

```xml
<properties>
  <mockatcha.version>0.1.0-SNAPSHOT</mockatcha.version>
  <teavm.version>0.15.0</teavm.version>
</properties>

<dependencies>
  <dependency>
    <groupId>io.instanto</groupId>
    <artifactId>mockatcha-core</artifactId>
    <version>${mockatcha.version}</version>
    <scope>test</scope>
  </dependency>
  <dependency>
    <groupId>org.teavm</groupId>
    <artifactId>teavm-classlib</artifactId>
    <version>${teavm.version}</version>
    <scope>test</scope>
  </dependency>
  <dependency>
    <groupId>org.teavm</groupId>
    <artifactId>teavm-junit</artifactId>
    <version>${teavm.version}</version>
    <scope>test</scope>
  </dependency>
  <dependency>
    <groupId>junit</groupId>
    <artifactId>junit</artifactId>
    <version>4.13.2</version>
    <scope>test</scope>
  </dependency>
</dependencies>
```

Configure the TeaVM runner used by Maven Surefire:

```xml
<systemPropertyVariables>
  <teavm.junit.target>${project.build.directory}/js-tests</teavm.junit.target>
  <teavm.junit.js.runner>browser-chrome</teavm.junit.js.runner>
</systemPropertyVariables>
```

The current repository version is a snapshot and must be installed locally
until published artifacts are available.

## Build this repository

Use Java 21, Maven, and a locally available Chrome or Chromium browser:

```bash
mvn clean test
```

The reactor compiles Mockatcha and runs both the core contract tests and the
consumer examples in Chrome through TeaVM.

To run only the examples and anything they require:

```bash
mvn -pl mockatcha-examples -am test
```

## License

Apache License 2.0.
