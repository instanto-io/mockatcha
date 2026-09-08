# Verification

## Verify a call

`verify` checks that a call occurred:

```java
verify(repository).save(report);
```

It works well for observable effects such as saving a record or sending a
notification. Matchers work in verification calls:

```java
verify(notifications).send(anyString());
```

## Check invocation counts

By default, `verify` expects exactly one call. Supply a verification mode for a
different count:

```java
verify(repository, times(2)).save(report);
verify(notifications, never()).send(anyString());
verify(feed, atLeastOnce()).refresh();
verify(feed, atLeast(2)).refresh();
verify(feed, atMost(5)).refresh();
```

`only()` requires one matching call and no other calls on that mock:

```java
verify(store, only()).load("A-17");
```

## Verify call order

Group mocks in an `InOrder` and verify through it:

```java
InOrder order = inOrder(repository, notifications);

order.verify(repository).save(report);
order.verify(notifications).send("saved");
```

Each verification consumes the calls it matches. The next verification looks
only at later calls. Unverified calls may occur between the checked calls.

`times`, `never`, `atLeast`, and `atLeastOnce` also work with `InOrder`. To
reject any unverified calls after the sequence, finish with:

```java
order.verifyNoMoreInteractions();
```

## Reject extra calls

`verifyNoMoreInteractions` requires all calls on a mock to have been verified:

```java
verify(repository).save(report);
verify(repository).audit("saved");
verifyNoMoreInteractions(repository);
```

`verifyNoInteractions(mock)` requires a collaborator to remain unused.

Use these checks only when extra work would violate the behaviour under test.
Applying them to incidental calls makes tests brittle.

## Read verification failures

A failed verification lists the recorded calls:

```text
Wanted 1 invocation(s) of save([something else]) but observed 0.
Calls recorded on this mock:
  save([report])
  audit([done])
```

Matchers remain queued until a mock call consumes them. Passing a matcher to a
real method can otherwise affect the next mock call, so Mockatcha reports the
unused matcher:

```text
verify() found 1 argument matcher(s) left over: [anyString()].
A matcher belongs inside a call on a mock, as in verify(mock).save(any()).
```

This validation runs when a mock is created and when `when` or `verify` begins.
Call `Mockatcha.validateUsage()` to run it explicitly, for example from an
`@After` method.

## Capture an argument

Use an `ArgumentCaptor` when the class under test constructs the value passed to
a collaborator:

```java
ArgumentCaptor<Timesheet> saved = ArgumentCaptor.forClass(Timesheet.class);

service.submit("A-17", 38);

verify(repository).save(saved.capture());
assertEquals("A-17", saved.getValue().employeeId());
assertEquals(38, saved.getValue().hours());
```

`capture()` is a matcher, so all other arguments in the same call must also use
matchers. After several calls, `getAllValues()` returns every captured argument
in order and `getValue()` returns the last one.

Continue with [mocks and spies](mocks-and-spies.md).

[Documentation index](README.md) · [Project README](../README.md)
