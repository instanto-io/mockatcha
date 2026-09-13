# Strictness and lifecycle

## Report unused stubs

An unused stub can indicate a typo or an arrangement left behind after a
refactor. Validate one mock explicitly:

```java
validateStubbing(store);
```

The failure names each unused stub and lists calls to the same method:

```text
These stubs were arranged but never used:
  find[A-17]
    that method was called with:
      find([A-18])
Remove them, correct their arguments, or arrange them with lenient().
```

Called without arguments, `validateStubbing()` checks every mock created since
the previous check. This supports validation from an `@After` method without
including mocks from earlier tests.

## Fail on a mismatched call

`strictStubs(true)` reports a call immediately when a method has arranged
answers but none match the supplied arguments:

```java
when(store.find("A-17")).thenReturn("timesheet");

store.find("A-18");
```

```text
Mockatcha mock of Store was called with find([A-18]), which no arranged answer
matched.
This method does have arranged answers for:
  find[A-17]
Either the call or the stub has the wrong arguments. Use lenient() where a stub
is deliberately narrow.
```

An unstubbed spy call still reaches the wrapped object. A method with no stubs
also retains its default behaviour.

Mark shared setup as lenient when only some tests use it:

```java
lenient().when(clock.now()).thenReturn(FIXED_TIME);
```

## Use `MockatchaRule` with JUnit

`MockatchaRule` enables strict stubbing, validates unused stubs and unfinished
operations, and releases mock state after each test:

```java
@Rule
public MockatchaRule mockatcha = new MockatchaRule();
```

The rule preserves a failure from the test body as the primary failure and
attaches lifecycle failures as suppressed exceptions. The test class remains
free to extend another class.

TeaVM's runner needs the reusable
separate [`io.instanto:teavm-rule-support`](https://github.com/instanto-io/teavm-rule-support) library to execute JUnit
rules. Put it on the test classpath ahead of `teavm-junit`. The module is
independent of Mockatcha and supports ordinary `TestRule` use in other
TeaVM/JUnit projects.

## Use `StrictTest` as a compatibility fallback

`StrictTest` is a Mockatcha class, not a JUnit or TeaVM API. Use it when a
project cannot install `teavm-rule-support`, or when an older runner executes
inherited `@Before` and `@After` methods but ignores JUnit rules:

```java
@RunWith(TeaVMTestRunner.class)
@SkipJVM
public class PricingTest extends StrictTest {
    // tests
}
```

It performs the same session lifecycle through inherited JUnit hooks.

## Manage a session directly

For a non-JUnit harness or an explicitly managed lifecycle, use
`MockatchaSession`:

```java
try (MockatchaSession session = Mockatcha.session(true)) {
    PricingService pricing = mock(PricingService.class);
    // arrange, act, and verify
}
```

Closing the session validates unused stubs and unfinished operations, restores
the previous strictness setting, and releases its mocks. A released mock cannot
be reused by a later test. Sessions cannot be nested, including inside
`MockatchaRule` or `StrictTest`.

| Test environment | Lifecycle |
| --- | --- |
| JUnit with `teavm-rule-support` | `MockatchaRule` |
| JUnit without rule support | `StrictTest` |
| Non-JUnit or custom harness | `MockatchaSession` in try-with-resources |

## Reuse or inspect a mock

Create fresh mocks for most tests. For a deliberately shared fixture:

```java
clearInvocations(mock);   // forget calls; retain stubs
reset(mock);              // forget calls and stubs
```

Read the invocation record for diagnostics or a custom assertion:

```java
List<Invocation> calls = mockingDetails(repository).getInvocations();
```

The BDD helpers also provide a focused
[call log](../mockatcha-bdd/README.md#inspect-calls).

## Type requirements

Mockatcha works with interfaces and with classes that satisfy three conditions:

- the class can be extended;
- it has a no-argument constructor, which runs when the mock is created; and
- the methods to replace are public or protected instance methods.

Static, private, and final methods retain their real behaviour. Pass the type as
a class literal, for example `mock(ProfileRepository.class)`. Invalid requests
fail during TeaVM compilation with the type and violated rule.

[Documentation index](README.md) · [Project README](../README.md)
