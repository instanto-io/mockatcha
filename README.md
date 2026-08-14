# Mockatcha

Mockatcha is a toolkit for Java tests that TeaVM runs in a browser. Mockatcha
Core provides Mockito-style mocks. Companion modules add browser-focused BDD
helpers and DOM testing.

Use Mockatcha Core when a test needs to work against browser or other APIs,
while other parts can be mocked.

## When to use it

Keep portable domain logic, algorithms, and state machines in JVM tests with
Mockito. Those tests have all of Mockito available and finish in milliseconds
where a browser test takes seconds.

Use Mockatcha when:

**The code uses the browser.** Timers, the DOM, canvas, storage, anything
reached through TeaVM's JavaScript interop. A JVM test cannot exercise those
APIs directly, so the test and its mocks run through TeaVM.

**A dependency is browser-specific.** A transport over WebSocket, for example,
or a store over IndexedDB. The class under test may be portable while the thing
it talks to is not.

**The behaviour only exists after compilation.** Java translated to JavaScript
differs in places: `long` arithmetic is emulated, `HashMap` iteration order
changes, date and number formatting follow the browser. A test on the JVM cannot
see any of it.

## How it works

Mockito creates mocks at runtime through reflection and JVM class generation.
TeaVM compiles the whole program ahead of time, so Mockatcha generates each mock
while TeaVM compiles the test.

Mock generation has these constraints:

- Pass a class literal, such as `mock(ProfileRepository.class)`, so Mockatcha
  knows what to generate.
- Invalid requests, such as mocking a final class, fail during compilation.
- A test that creates Mockatcha mocks runs through TeaVM, not as a JVM test.

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

This stubs the `String` overload of `find`; another `find` overload keeps its
default behaviour.

## Flexible arguments

Use a matcher when `find` should return the same profile for every string ID:

```java
when(profiles.find(anyString())).thenReturn(defaultProfile);
```

There is a matcher for each primitive type, plus `any()`, `any(Type.class)`,
`nullable(Type.class)`, `eq(...)`, `same(...)`, `startsWith(...)`,
`endsWith(...)`, `isNull()`, `isNotNull()`, and `argThat(...)` for a condition
of your own:

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

If any argument uses a matcher, every argument must use one. Mixing a matcher
with a plain value in the same call is ambiguous, so wrap the plain one in
`eq`:

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

## Return an argument or sequence

`AdditionalAnswers` can return one of the call's arguments or values from an
existing list:

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
cannot see in a return value, such as saving a record or sending a notification.

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
was the only call on that mock**, which is how to show that a cache answered
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
ask "what was it called with" because the argument was built inside the class
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

Give an important collaborator a name when several mocks of the same type would
otherwise make a failure ambiguous:

```java
PricingService pricing = mock(PricingService.class, "regional pricing");
```

The name appears in verification, strict-stubbing, and unused-stub diagnostics.

The result really is a `PricingService` and can be passed anywhere one is
expected. Its no-argument constructor runs, and every public and protected
method is replaced.

Class mocks have these constraints:

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

A spy has two limitations:

- the spy is a separate object from the one it wraps, so `==` tells them apart;
  and
- when a real method calls another method on itself, that inner call reaches the
  wrapped object directly. Stubbing `currency()` changes what the test sees, and
  `summary()` still sees the real one when it calls `currency()` itself.

Where the second point matters, mock the collaborator instead.

## Set up without calling the method

`when(spy.currency())` has to run `spy.currency()` to know which call you mean.
On a spy that means the real method runs once during setup. When it is slow,
destructive, or unavailable in a browser, arrange the answer first:

```java
doReturn("EUR").when(pricingSpy).currency();
doThrow(new IllegalStateException("offline")).when(pricingSpy).currency();
doAnswer(call -> call.argument(0) + "!").when(pricingSpy).describe(anyString());
doNothing().when(pricingSpy).record("audited");
```

The real method never runs. These work on plain mocks too, where they are just
another way of writing `when`.

## Given and then aliases

`mockatcha-core` includes `BDDMockatcha`, which provides `given(...)` and
`then(...)` aliases for stubbing and verification:

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
counting rules, such as `should(times(2))`, or an order with `should(order)`.
`shouldHaveNoInteractions` and `shouldHaveNoMoreInteractions` replace the
`verifyNo...` calls.

`willReturn(v).given(mock).method()` is the spelling of `doReturn`, for the
cases where the call must not run during setup.

The aliases delegate to the core API and can be mixed with `when(...)` and
`verify(...)`. They do not require the separate
[`mockatcha-bdd` module](mockatcha-bdd/README.md).
That module provides a browser clock, call logs, and additional test helpers.

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

### Fail at the call instead

`strictStubs(true)` moves the report forward: a call that finds no arranged
answer, on a method that has one, fails there and then.

```java
when(store.find("A-17")).thenReturn("timesheet");

store.find("A-18");
```

```
Mockatcha mock of Store was called with find([A-18]), which no arranged answer
matched.
This method does have arranged answers for:
  find[A-17]
Either the call or the stub has the wrong arguments. Use lenient() where a stub
is deliberately narrow.
```

A spy is left alone, because an unstubbed call there is meant to reach the real
object, and so is a method with no stubs of its own.

Where a stub is deliberately broad, such as shared setup that only some tests
use, `lenient()` exempts it:

```java
lenient().when(clock.now()).thenReturn(FIXED_TIME);
```

### Automatic lifecycle: prefer the rule

For a JUnit test, `MockatchaRule` is the normal way to enable strict stubbing,
validate unused stubs and unfinished operations, and release mock state after
each test:

```java
@Rule
public MockatchaRule mockatcha = new MockatchaRule();
```

The rule opens a `MockatchaSession`, preserves a test-body failure as the primary
failure, and attaches any lifecycle failure as suppressed. It also leaves the
test class free to extend another class.

Mockatcha ships the reusable
[`teavm-rule-support` module](teavm-rule-support/README.md), which adds JUnit
rule execution to TeaVM's runner. Add it to the test classpath ahead of
`teavm-junit`; `MockatchaRule` then works as the default lifecycle. The support
module does not depend on Mockatcha and can be used by any TeaVM/JUnit project
that needs ordinary `TestRule` support.

### Compatibility fallback: `StrictTest`

`StrictTest` is a Mockatcha class, not a JUnit or TeaVM API. It exists for a
consumer that cannot install `teavm-rule-support`, or that must run against an
older or otherwise unpatched TeaVM runner which executes inherited `@Before`
and `@After` methods but ignores JUnit rules:

```java
@RunWith(TeaVMTestRunner.class)
@SkipJVM
public class PricingTest extends StrictTest {
    ...
}
```

It performs the same session lifecycle through inherited JUnit hooks. Prefer
`MockatchaRule` whenever rule support is available; use `StrictTest` only when
the runner cannot execute the rule.

### Framework-neutral lifecycle: `MockatchaSession`

For a non-JUnit harness or deliberately managed lifecycle, use the underlying
session directly:

```java
try (MockatchaSession session = Mockatcha.session(true)) {
    PricingService pricing = mock(PricingService.class);
    // arrange, act, and verify
}
```

Closing validates unused stubs and unfinished operations, restores the previous
strictness setting, and releases every mock owned by the session. A released
mock cannot be used accidentally by a later test. Sessions cannot be nested,
so do not open one inside `MockatchaRule` or `StrictTest`.

| Test environment | Lifecycle to use |
| --- | --- |
| JUnit with `teavm-rule-support` installed | `MockatchaRule` (preferred) |
| Consumer unable to install rule support | `StrictTest` compatibility fallback |
| Non-JUnit or custom harness | `MockatchaSession` with try-with-resources |

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

The BDD helpers provide a focused
[call log](mockatcha-bdd/README.md#inspect-calls).

## Requirements

Mockatcha works on interfaces, and on classes that meet three conditions:

- the class can be extended;
- it has a no-argument constructor, which is run when the mock is created; and
- the methods you want to replace are instance methods that are public or
  protected. Static, private, and final methods keep their real behaviour.

The type passed to `mock` or `spy` is written out in full at the call site:
`mock(ProfileRepository.class)`. Anything outside these rules is reported while
the test compiles, naming the type and the rule that applied.

## Modules

| Module | What it is |
| --- | --- |
| `mockatcha-core` | The mocking API, TeaVM code generation, and JUnit lifecycle adapters described above. |
| `mockatcha-bdd` | Optional [browser-test helpers](mockatcha-bdd/README.md): stub by method name, inspect calls, match values, and control time. |
| `mockatcha-dom` | [Browser UI testing](mockatcha-dom/README.md): accessible queries, interactions, waiting, and assertions. It does not require `mockatcha-core`. |
| `mockatcha-examples` | Worked examples you can run. |
| `teavm-rule-support` | Reusable [JUnit `TestRule` support for TeaVM](teavm-rule-support/README.md), used by Mockatcha but independently useful to any TeaVM/JUnit project. |

## Examples

The examples module tests a real `TimesheetService` with mocked storage and
approval boundaries. Read them in this order:

1. [`TimesheetServiceTest`](mockatcha-examples/src/test/java/io/instanto/mockatcha/examples/TimesheetServiceTest.java):
   stubbing, verification, matchers, answers, and failures.
2. [`TimesheetSpyTest`](mockatcha-examples/src/test/java/io/instanto/mockatcha/examples/TimesheetSpyTest.java):
   class mocks and spies.
3. [`SparklineTest`](mockatcha-examples/src/test/java/io/instanto/mockatcha/examples/SparklineTest.java):
   a real browser canvas with a mocked source of readings.

## Build this repository

Java 21, Maven, and a local Chrome or Chromium:

```bash
mvn clean test
```

The browser is configurable. With Firefox installed, the same clock and DOM
coverage used by CI runs with:

```bash
mvn -pl mockatcha-dom -am -Dmockatcha.test.browser=browser-firefox test
```

To run only the examples and what they need:

```bash
mvn -pl mockatcha-examples -am test
```

## License

Apache License 2.0.
