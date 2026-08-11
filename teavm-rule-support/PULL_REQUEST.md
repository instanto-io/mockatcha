# Support JUnit `@Rule` in `TeaVMTestRunner`

## The problem

A `@Rule` field in a test run by `TeaVMTestRunner` is never used. The test
compiles, runs, and passes — the rule simply does not execute.

```java
@RunWith(TeaVMTestRunner.class)
public class ExampleTest {
    static final StringBuilder events = new StringBuilder();

    @Rule
    public TestRule recorder = (base, description) -> new Statement() {
        @Override
        public void evaluate() throws Throwable {
            events.append("before;");
            base.evaluate();
        }
    };

    @Test
    public void ruleRan() {
        assertEquals("before;", events.toString());   // fails: events is empty
    }
}
```

Silence is the awkward part. A rule that sets up and tears down global state —
a temporary folder, a stubbed clock, an installed mock framework — leaves the
test passing for the wrong reason, and a rule whose whole job is to assert
something never gets the chance. Rules are common enough in library test suites
that this tends to be discovered late.

`TestEntryPointTransformer` generates the bodies of `TestEntryPoint.before()`
and `after()` by emitting a direct call to each annotated method it finds,
walking the superclass chain. `@Rule` fields are not looked at anywhere.

## The change

Three parts.

**1. `TestEntryPoint` gains an `applyRules` method** and calls it around each
launcher:

```java
for (Launcher launcher : launchers) {
    applyRules(new LaunchStatement(launcher), name).evaluate();
}
```

`LaunchStatement.evaluate` runs `before()`, the test, then `after()`, so rules
sit outside the `@Before` and `@After` methods, as they do in JUnit.

**2. `TestEntryPointTransformer` generates the body of `applyRules`**, in the
same style as the existing `before`/`after` generation. For a test declaring two
rules it emits the equivalent of:

```java
static Statement applyRules(Statement base, String name) {
    Statement statement = base;
    Description description = describe("com.example.ExampleTest", name);
    statement = ((TestRule) testCase.first).apply(statement, description);
    statement = ((TestRule) testCase.second).apply(statement, description);
    return statement;
}
```

The fields are read directly, so nothing needs reflection at runtime. When a
test declares no rules the generated body returns its argument unchanged, which
is what every existing test compiles to.

**3. `TConcurrentLinkedQueue` is added to the classlib.** This one is not really
about rules, and may be worth taking on its own merits.

`TestRule.apply(Statement, Description)` takes a `Description` — JUnit's
description of the test being run — so calling a rule means constructing one.
`org.junit.runner.Description` holds its children in a `ConcurrentLinkedQueue`,
as a field initialiser, so every constructor reaches it:

```java
private final Collection<Description> fChildren = new ConcurrentLinkedQueue<Description>();
```

With no emulation for that class, the real JDK one is compiled, and its static
initialiser sets up `VarHandle`s:

```
Method java.lang.invoke.MethodHandles.lookup()Ljava/lang/invoke/MethodHandles$Lookup; was not found
    at java.util.concurrent.ConcurrentLinkedQueue.<clinit>(ConcurrentLinkedQueue.java:1064)
    at org.junit.runner.Description.<init>(Description.java:155)
    at org.junit.runner.Description.createTestDescription(Description.java:73)
    at org.teavm.junit.TestEntryPoint.describe
```

So the reachable surface is wider than JUnit: any code that constructs a
`Description`, and any code using `ConcurrentLinkedQueue` at all, fails the same
way today. It is one of the JDK classes whose static initialiser reaches for
method handles, which is a class of problem worth closing off one entry at a
time.

The implementation is a queue backed by an `ArrayDeque`, which is all a
single-threaded runtime needs, sitting alongside the existing
`TConcurrentHashMap` and `TCopyOnWriteArrayList`. It keeps the null rejection
`ConcurrentLinkedQueue` specifies, and inherits the rest of the API from
`AbstractQueue` and `AbstractCollection`.

One style question for you. The neighbouring emulations extend the `T`-prefixed
classlib types — `TConcurrentHashMap extends TAbstractMap` — while this one
extends `java.util.AbstractQueue`, which the renamer maps to `TAbstractQueue`
anyway. `TCopyOnWriteArrayList` already mixes the two, importing
`java.util.Collection` and `java.util.List`, but nothing in the classlib
currently *extends* a plain `java.util` type, so this would be the first.

I wrote it this way because the `T` types are not on the published
`teavm-classlib` jar's classpath, and that jar is what I had to compile against
to test the change outside your build. Say the word and I will switch it to
`TAbstractQueue`/`TArrayDeque`/`TQueue`, which is the version I would expect you
to prefer.

## Ordering

Rules are applied in declaration order, each wrapping the previous, so the last
field declared is the outermost, and superclass rules are applied before
subclass rules. This matches what `RunRules` does with the list it is handed.

## Not included

- **`MethodRule`.** It receives a `FrameworkMethod`, which wraps
  `java.lang.reflect.Method` and is rejected by JUnit if null. TeaVM produces a
  `Method` only for a method annotated `@Reflectable`, so this would mean the
  transformer adding that annotation to test methods and emitting
  `getDeclaredMethod`. Reachable, but more machinery for the older of the two
  interfaces. A `MethodRule` field currently fails compilation with a message
  naming the field and suggesting `TestRule`, rather than being ignored.
- **`@ClassRule`.** Would need a hook around the whole class rather than around
  each test.
- **`@Rule(order = ...)`**, added in JUnit 4.13. Supporting it means sorting the
  collected fields before emitting the chain, and is a small follow-up if this
  approach looks right.

## Testing

`tests/src/test/java/org/teavm/tests/JUnitRuleTest.java` declares two rules and
checks that both wrap the test, that they nest and unwind in the expected order,
and that the `Description` a rule receives names the test class and method.

I should be straightforward about how this was verified: I have not run TeaVM's
own Gradle build. I developed and ran the change by putting the three classes on
the test classpath of a separate Maven project ahead of the TeaVM jars, where
they shadow the published ones, and ran it in Chrome through
`TeaVMTestRunner`. That covers the generated code and the classlib addition
under a real browser, but not `checkstyleMain` or the wider suite, so those are
worth a look before merging.

## In the meantime

If this needs time, or lands in a later release, the change works as a small jar
that a project puts on its test classpath ahead of `teavm-junit`, where its
copies of `TestEntryPoint` and `TestEntryPointTransformer` shadow the published
ones. `TConcurrentLinkedQueue` needs no shadowing, since it fills a gap rather
than replacing anything.

I am happy to publish that jar so people who need rules today are not blocked,
and to point it at whatever you decide here so the two do not drift. It is
plainly a stopgap — classpath ordering is a poor thing to depend on — and I
would rather it became unnecessary.

Happy to adjust the approach, the naming, or the scope.
