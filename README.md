# Mockatcha

Mockatcha is a mocking library for Java tests that run in a browser through
TeaVM. If you have used Mockito, you already know most of it.

A test usually wants to exercise one real class while replacing the things
around it — the repository, the remote service, the clock. Mockatcha builds
those replacements for you.

```java
GreetingService greetings = mock(GreetingService.class);
when(greetings.hello("Ada")).thenReturn("Hello Ada");

assertEquals("Hello Ada", new GreetingComponent(greetings).greet("Ada"));
verify(greetings).hello("Ada");
```

This guide starts with the simplest possible test and adds one idea at a time.
The companion library, [Oolong](oolong/README.md), adds a fake clock and a
second way of writing the same things.

| | |
| --- | --- |
| **Get started** | [Setup](#setup) · [Your first test](#your-first-test) |
| **Say what a mock does** | [Return a value](#return-a-value) · [Flexible arguments](#flexible-arguments) · [Calculate or fail](#calculate-an-answer-or-fail) · [Change over time](#change-the-answer-over-time) |
| **Check what happened** | [Verify a call](#verify-a-call) · [How many times](#how-many-times) · [Capture an argument](#capture-an-argument) |
| **Beyond interfaces** | [Mock a class](#mock-a-class) · [Spy on a real object](#spy-on-a-real-object) · [Set up without calling](#set-up-without-calling-the-method) |
| **Reference** | [Reuse and reset](#reuse-and-reset) · [What it cannot do](#what-it-cannot-do) · [Modules](#modules) · [Build](#build-this-repository) |

## Setup

Add Mockatcha to your test dependencies:

```xml
<dependency>
  <groupId>io.instanto</groupId>
  <artifactId>mockatcha-core</artifactId>
  <version>0.1.0-SNAPSHOT</version>
  <scope>test</scope>
</dependency>
```

You will also need `teavm-classlib`, `teavm-junit`, and `junit` on the test
classpath, and Surefire told to run the tests in a browser:

```xml
<systemPropertyVariables>
  <teavm.junit.target>${project.build.directory}/js-tests</teavm.junit.target>
  <teavm.junit.js.runner>browser-chrome</teavm.junit.js.runner>
</systemPropertyVariables>
```

Until Mockatcha is published, build this repository and install it locally.

## Your first test

Say a component gets its text from a service:

```java
public interface GreetingService {
    String hello(String name);
}
```

A test can replace that service, decide what it returns, and run the real
component against it:

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
    public void showsTheGreetingFromTheService() {
        GreetingService greetings = mock(GreetingService.class);
        when(greetings.hello("Ada")).thenReturn("Hello Ada");

        String shown = new GreetingComponent(greetings).greet("Ada");

        assertEquals("Hello Ada", shown);
        verify(greetings).hello("Ada");
    }
}
```

Three things are happening.

**`mock(...)`** creates a stand-in. Every method on it does nothing and returns
an empty value — `null`, `0`, or `false` — until you say otherwise.

**`when(...)`** says what one particular call should return.

**`verify(...)`** checks that a call happened.

The two annotations are TeaVM's. `@RunWith(TeaVMTestRunner.class)` compiles and
runs the test in a browser; `@SkipJVM` stops JUnit from also trying to run it on
the JVM, where the mock does not exist.

## Return a value

Pass the call you care about to `when`, then say what it should give back:

```java
when(profiles.find("A-17")).thenReturn(new Profile("A-17", "Ada"));
```

Every later call with those exact arguments gets that profile. Calls with
different arguments are unaffected and still return `null`.

Overloads are separate: `total(String)` and `total(int)` are configured
independently.

## Flexible arguments

Sometimes the exact argument is not the point:

```java
when(profiles.find(anyString())).thenReturn(defaultProfile);
```

There is a matcher for each primitive type, plus `any()`, `eq(...)`,
`isNull()`, `isNotNull()`, and `argThat(...)` for a condition of your own:

```java
when(store.save(argThat(timesheet -> timesheet.hours() > 40))).thenReturn(true);
```

One rule to remember: **if any argument uses a matcher, they all must.** Mixing
a matcher with a plain value in the same call is ambiguous, so wrap the plain
one in `eq`:

```java
when(calculator.total(anyInt(), eq("EUR"))).thenReturn(42);
```

Prefer exact values when they make the test clearer. Reach for a matcher when
the value genuinely does not matter.

## Calculate an answer or fail

When the result depends on the arguments, use `thenAnswer`:

```java
when(greetings.hello(anyString()))
        .thenAnswer(call -> "Hello " + call.argument(0));
```

When you want to see how your class copes with a broken collaborator, use
`thenThrow`:

```java
when(profiles.find("A-17"))
        .thenThrow(new IllegalStateException("Profile store unavailable"));
```

## Change the answer over time

Chain answers to describe something that changes:

```java
when(status.current())
        .thenReturn("starting")
        .thenReturn("ready");
```

The first call gets `starting`; every call after that gets `ready`. The last
answer repeats, so you never have to predict exactly how many times your code
will ask.

## Verify a call

`verify` checks that something happened:

```java
verify(repository).save(report);
```

It reads as an assertion and fails like one. It is most useful for effects you
cannot see in a return value — saving a record, sending a notification.

Matchers work here too:

```java
verify(notifications).send(anyString());
```

## How many times

By default `verify` expects exactly one call. When the count matters, say so:

```java
verify(repository, times(2)).save(report);
verify(notifications, never()).send(anyString());
verify(feed, atLeastOnce()).refresh();
verify(feed, atLeast(2)).refresh();
verify(feed, atMost(5)).refresh();
```

`only()` is stronger than any of these. It says the call happened once **and
nothing else was called on that mock** — which is how to show that a cache did
not go back to its store:

```java
verify(store, only()).load("A-17");
```

## Capture an argument

Matchers ask "was it called with something like this". Sometimes you need to
ask "what was it called with" — because the argument was built inside the class
you are testing and the test could not have predicted it.

```java
ArgumentCaptor<Timesheet> saved = ArgumentCaptor.forClass(Timesheet.class);

service.submit("A-17", 38);

verify(repository).save(saved.capture());
assertEquals("A-17", saved.getValue().employeeId());
assertEquals(38, saved.getValue().hours());
```

`capture()` counts as a matcher, so the all-or-nothing rule applies to the other
arguments of that call.

If the call happened more than once, `getAllValues()` gives you every argument
in order and `getValue()` gives you the last.

## Mock a class

Not every boundary is an interface. `mock` takes a class too:

```java
PricingService pricing = mock(PricingService.class);
when(pricing.total("A-17")).thenReturn(42);
```

The result really is a `PricingService` and can be passed anywhere one is
expected. Its no-argument constructor runs, and every public and protected
method is replaced.

A few things to know:

- the class must not be `final`, and must have a no-argument constructor. If it
  does not, the test fails to compile with a message naming the rule it broke;
- `static`, `private`, and `final` methods keep their real behaviour, because a
  subclass cannot replace them; and
- `equals`, `hashCode`, and `toString` answer by identity and with a fixed
  description, so printing a mock never runs real code.

Prefer an interface where your design allows one. Class mocking is for
boundaries you do not control.

## Spy on a real object

A mock replaces every method. A spy keeps a real object and replaces only the
calls you name:

```java
InMemoryTimesheetRepository real = new InMemoryTimesheetRepository();
InMemoryTimesheetRepository timesheets = spy(InMemoryTimesheetRepository.class, real);

when(timesheets.findDraft("A-17")).thenReturn(new Timesheet("A-17", 46));
```

`findDraft` now answers from the stub. Everything else runs for real and is
still recorded, so `verify(timesheets).markSubmitted("A-17")` works as usual.

This suits a working collaborator you would rather keep than rebuild in stubs.
An interface and one of its implementations work too:

```java
Notifier notifier = spy(Notifier.class, new EmailNotifier());
```

Two things to know before relying on a spy:

- the spy is a different object from the one it wraps, so `==` tells them
  apart; and
- if a real method calls another method on itself, that inner call is not
  intercepted. Stubbing `currency()` does not change what `summary()` sees when
  `summary()` calls it.

Where the second point matters, mock the collaborator instead of spying on it.

## Set up without calling the method

`when(spy.currency())` has to run `spy.currency()` to know which call you mean.
On a spy that means the real method runs once during setup. When it is slow,
destructive, or simply unavailable in a browser, arrange the answer first:

```java
doReturn("EUR").when(pricingSpy).currency();
doThrow(new IllegalStateException("offline")).when(pricingSpy).currency();
doAnswer(call -> call.argument(0) + "!").when(pricingSpy).describe(anyString());
doNothing().when(pricingSpy).record("audited");
```

The real method never runs. These work on plain mocks too, where they are just
another way of writing `when`.

## Reuse and reset

Most tests should create fresh mocks. When a fixture is deliberately shared:

```java
clearInvocations(mock);   // forget the calls, keep the stubs
reset(mock);              // forget both
```

For a diagnostic, or an assertion the verification modes cannot express, read
the record directly:

```java
List<Invocation> calls = mockingDetails(repository).getInvocations();
```

Oolong offers a [more readable way](oolong/README.md#read-the-call-record) to do
the same thing.

## What it cannot do

Mockatcha would rather be clear about its limits than fail confusingly.

- **Static methods and constructors** cannot be mocked.
- **Final classes** cannot be mocked, and **final and private methods** keep
  their real behaviour.
- **A mocked class must have a no-argument constructor**, which is run.
- **The class passed to `mock` or `spy` must be written out in full** at the
  call site: `mock(ProfileRepository.class)`, not `mock(someVariable)`.

Unsupported cases are reported while the test compiles, naming the type and the
rule it failed.

Not yet supported, but possible: `InOrder` verification,
`verifyNoMoreInteractions`, and annotation-driven mocks. The
[design record](CLASS_MOCKING_AND_SPIES.md) has the full picture.

## Modules

| Module | What it is |
| --- | --- |
| `mockatcha-core` | The library described above. |
| `oolong` | A [second vocabulary](oolong/README.md): configure by method name, read the call record, control the clock. |
| `mockatcha-examples` | Worked examples you can run. Applications do not depend on it. |

Mockatcha has no dependency on Mockito, Byte Buddy, CDI, a browser DOM, Sarto,
or Verrai.

## Examples

The examples module tests a real `TimesheetService` with mocked storage and
approval boundaries. Read them in this order:

1. [`TimesheetServiceTest`](mockatcha-examples/src/test/java/io/instanto/mockatcha/examples/TimesheetServiceTest.java)
   — stubbing, verification, matchers, answers, failures;
2. [`TimesheetSpyTest`](mockatcha-examples/src/test/java/io/instanto/mockatcha/examples/TimesheetSpyTest.java)
   — class mocks and spies.

## Build this repository

Java 21, Maven, and a local Chrome or Chromium:

```bash
mvn clean test
```

To run only the examples and what they need:

```bash
mvn -pl mockatcha-examples -am test
```

## License

Apache License 2.0.
