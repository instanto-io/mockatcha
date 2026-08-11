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
| **Say what a mock does** | [Return a value](#return-a-value) · [Flexible arguments](#flexible-arguments) · [Calculate or fail](#calculate-an-answer-or-fail) · [Common answers](#reuse-a-common-answer) · [Change over time](#change-the-answer-over-time) |
| **Check what happened** | [Verify a call](#verify-a-call) · [How many times](#how-many-times) · [In what order](#verify-the-order-of-calls) · [Account for every call](#account-for-every-call) · [Capture an argument](#capture-an-argument) |
| **Beyond interfaces** | [Mock a class](#mock-a-class) · [Spy on a real object](#spy-on-a-real-object) · [Set up without calling](#set-up-without-calling-the-method) |
| **Keep tests honest** | [Unused stubs](#insist-that-every-stub-is-used) · [When a check fails](#when-a-check-fails) |
| **Another spelling** | [Given, when, then](#given-when-then) |
| **Reference** | [Reuse and reset](#reuse-and-reset) · [Requirements](#requirements) · [Modules](#modules) · [Build](#build-this-repository) |

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

**`mock(...)`** creates a stand-in. Until you say otherwise, every method on it
returns an empty value: `null`, `0`, or `false`.

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
different arguments still return `null`.

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

`AdditionalMatchers` adds comparison and combination:

```java
verify(store).grade(and(gt(10), lt(100)));
verify(store).grade(or(lt(10), gt(100)));
verify(audit).log(not(eq("ignored")));
verify(audit).log(find("A-\\d+"));
verify(reader).read(aryEq(new byte[] {1, 2, 3}));
```

`gt`, `geq`, `lt`, and `leq` take `int`, `long`, `double`, or anything
`Comparable`; `cmpEq` compares by ordering where `equals` would be too strict.

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

## Reuse a common answer

Some answers come up often enough to have names:

```java
when(store.save(any())).thenAnswer(returnsFirstArg());
when(clock.next()).thenAnswer(returnsElementsOf(List.of(1, 2, 3)));
```

`returnsFirstArg`, `returnsSecondArg`, `returnsLastArg`, and `returnsArgAt(n)`
hand an argument back to the caller, which suits a collaborator that saves
something and returns it. `returnsElementsOf` walks a sequence you already have,
repeating the last element once it runs out.

`answer(...)` types the arguments for you, so the test does not cast:

```java
when(names.of(anyString())).thenAnswer(answer((String id) -> id.toUpperCase()));
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

`only()` is stronger than any of these. It says the call happened once **and it
was the only call on that mock** — which is how to show that a cache answered
from memory:

```java
verify(store, only()).load("A-17");
```

## Verify the order of calls

When the sequence matters, group the mocks into an `InOrder` and verify through
it:

```java
InOrder order = inOrder(repository, notifications);

order.verify(repository).save(report);
order.verify(notifications).send("saved");
```

Each `verify` consumes the calls it matches, so the next one looks only at what
came afterwards. Other calls in between are allowed, which keeps the test
focused on the sequence you care about.

`times`, `never`, `atLeast`, and `atLeastOnce` work here as they do elsewhere.
When you want to say that a sequence was the whole story, finish with:

```java
order.verifyNoMoreInteractions();
```

## Account for every call

`verifyNoMoreInteractions` expects everything on a mock to have been verified
already, which turns a test into a complete statement about what the class did:

```java
verify(repository).save(report);
verify(repository).audit("saved");
verifyNoMoreInteractions(repository);
```

`verifyNoInteractions(mock)` says a collaborator was left alone entirely.

Use these where the absence of extra work is the behaviour you are describing.
On a mock with many incidental calls they make a test harder to change than it
needs to be.

## When a check fails

A failed verification lists what did happen:

```
Wanted 1 invocation(s) of save([something else]) but observed 0.
Calls recorded on this mock:
  save([report])
  audit([done])
```

Matchers are held in a queue until the call they belong to consumes them, so a
matcher passed to something that is not a mock would otherwise be applied to the
next call on a real one. That is reported instead:

```
verify() found 1 argument matcher(s) left over: [anyString()].
A matcher belongs inside a call on a mock, as in verify(mock).save(any()).
```

The check runs when a mock is created and when `when` or `verify` begins.
`Mockatcha.validateUsage()` runs it on demand, which suits an `@After` method:
it reports the mistake against the test that made it.

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

- the class must be extendable and have a no-argument constructor, which is
  run. Anything else is reported while the test compiles, naming the rule that
  applied;
- `static`, `private`, and `final` methods keep their real behaviour; and
- `equals`, `hashCode`, and `toString` answer by identity and with a fixed
  description, so printing a mock is always safe.

Prefer an interface where your design allows one. Class mocking suits the
boundaries you inherit.

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

- the spy is a separate object from the one it wraps, so `==` tells them apart;
  and
- when a real method calls another method on itself, that inner call reaches the
  wrapped object directly. Stubbing `currency()` changes what the test sees, and
  `summary()` still sees the real one when it calls `currency()` itself.

Where the second point matters, mock the collaborator instead.

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

## Given, when, then

`BDDMockatcha` is the same library spelled to match the three parts of a test:

```java
// given
given(repository.findDraft("A-17")).willReturn(new Timesheet("A-17", 38));

// when
Submission result = service.submit("A-17");

// then
then(repository).should().markSubmitted("A-17");
then(notifications).shouldHaveNoInteractions();
```

`given(...).willReturn/willThrow/willAnswer` replaces `when(...).thenReturn` and
its siblings. `then(mock).should()` replaces `verify(mock)`, and takes the same
counting rules — `should(times(2))` — and an order, as `should(order)`.
`shouldHaveNoInteractions` and `shouldHaveNoMoreInteractions` replace the
`verifyNo...` calls.

`willReturn(v).given(mock).method()` is the spelling of `doReturn`, for the
cases where the call must not run during setup.

Nothing behaves differently, and the two spellings mix freely, so choose one per
test rather than per project if that reads better.

## Insist that every stub is used

A stub nothing ever calls is usually a typo, or one that outlived a refactor.
The test still passes and proves less than it looks like it does.

```java
validateStubbing(store);
```

The failure names the stub and, more usefully, the calls that were made to the
same method:

```
These stubs were arranged but never used:
  find[A-17]
    that method was called with:
      find([A-18])
Remove them, correct their arguments, or arrange them with lenient().
```

Called with no arguments, it checks every mock created since the last such
check, which scopes it to one test from an `@After` method.

Extending `StrictTest` does that for you, along with the matcher check:

```java
@RunWith(TeaVMTestRunner.class)
@SkipJVM
public class PricingTest extends StrictTest {
    ...
}
```

A base class rather than a JUnit rule because TeaVM's test runner collects
`@Before` and `@After` from superclasses, and does not run rules.

Where a stub is deliberately broad — shared setup that only some tests use —
`lenient()` exempts it:

```java
lenient().when(clock.now()).thenReturn(FIXED_TIME);
```

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

## Requirements

Mockatcha works on interfaces, and on classes that meet three conditions:

- the class can be extended;
- it has a no-argument constructor, which is run when the mock is created; and
- the methods you want to replace are instance methods that are public or
  protected. Static, private, and final methods keep their real behaviour.

The type passed to `mock` or `spy` is written out in full at the call site:
`mock(ProfileRepository.class)`. Anything outside these rules is reported while
the test compiles, naming the type and the rule that applied.

The [design record](CLASS_MOCKING_AND_SPIES.md) covers what is planned next.

## Modules

| Module | What it is |
| --- | --- |
| `mockatcha-core` | The library described above. Its one dependency is TeaVM's metaprogramming API. |
| `oolong` | A [second vocabulary](oolong/README.md): configure by method name, read the call record, control the clock. |
| `mockatcha-examples` | Worked examples you can run. |

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
