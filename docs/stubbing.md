# Stubbing

## Return a value

Pass a call to `when`, then provide its result:

```java
when(profiles.find("A-17")).thenReturn(new Profile("A-17", "Ada"));
```

Later calls with those exact arguments return the profile. Other arguments
still return `null`. The stub applies to this `String` overload of `find`; other
overloads retain their default behaviour.

## Match arguments

Use a matcher to cover a range of arguments:

```java
when(profiles.find(anyString())).thenReturn(defaultProfile);
```

Mockatcha provides a matcher for each primitive type, plus `any()`,
`any(Type.class)`, `nullable(Type.class)`, `eq(...)`, `same(...)`,
`startsWith(...)`, `endsWith(...)`, `isNull()`, `isNotNull()`, and
`argThat(...)`:

```java
when(store.save(argThat(timesheet -> timesheet.hours() > 40)))
        .thenReturn(true);
```

`AdditionalMatchers` adds comparisons, combinations, regular expressions, and
array equality:

```java
verify(store).grade(and(gt(10), lt(100)));
verify(store).grade(or(lt(10), gt(100)));
verify(audit).log(not(eq("ignored")));
verify(audit).log(find("A-\\d+"));
verify(reader).read(aryEq(new byte[] {1, 2, 3}));
```

`gt`, `geq`, `lt`, and `leq` accept `int`, `long`, `double`, or a
`Comparable`. `cmpEq` compares by ordering when `equals` is too strict.

If one argument uses a matcher, all arguments in that call must use matchers.
Wrap an exact argument in `eq`:

```java
when(calculator.total(anyInt(), eq("EUR"))).thenReturn(42);
```

Use exact values when they make the test clearer. Use a matcher when several
values should produce the same behaviour.

## Calculate a result or throw an exception

Use `thenAnswer` when the result depends on the arguments:

```java
when(greetings.hello(anyString()))
        .thenAnswer(call -> "Hello " + call.argument(0));
```

Use `thenThrow` to arrange a failure:

```java
when(profiles.find("A-17"))
        .thenThrow(new IllegalStateException("Profile store unavailable"));
```

## Return an argument or sequence

`AdditionalAnswers` can return a call argument or values from an existing list:

```java
when(store.save(any())).thenAnswer(returnsFirstArg());
when(clock.next()).thenAnswer(returnsElementsOf(List.of(1, 2, 3)));
```

`returnsFirstArg`, `returnsSecondArg`, `returnsLastArg`, and `returnsArgAt(n)`
return the selected argument. `returnsElementsOf` walks an existing sequence
and repeats its final element after reaching the end.

`answer(...)` supplies typed arguments to the callback:

```java
when(names.of(anyString()))
        .thenAnswer(answer((String id) -> id.toUpperCase()));
```

## Change the result between calls

Chain answers to describe successive states:

```java
when(status.current())
        .thenReturn("starting")
        .thenReturn("ready");
```

The first call returns `starting`. The second and later calls return `ready`;
the last arranged answer repeats.

Continue with [verification](verification.md).

[Documentation index](README.md) · [Project README](../README.md)
