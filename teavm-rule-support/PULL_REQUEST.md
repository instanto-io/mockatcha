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

`org.junit.runner.Description` holds its children in a `ConcurrentLinkedQueue`.
With no emulation the real JDK class was compiled, and its static initialiser
reaches `MethodHandles.lookup()`:

```
Method java.lang.invoke.MethodHandles.lookup()Ljava/lang/invoke/MethodHandles$Lookup; was not found
    at java.util.concurrent.ConcurrentLinkedQueue.<clinit>(ConcurrentLinkedQueue.java:1064)
    at org.junit.runner.Description.<init>(Description.java:155)
    at org.junit.runner.Description.createTestDescription(Description.java:73)
```

Any code constructing a `Description` hits this today, rule support or not. The
class added here is a queue backed by an `ArrayDeque`, which is all a
single-threaded runtime needs, and sits alongside the existing
`TConcurrentHashMap` and `TCopyOnWriteArrayList`.

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

Happy to adjust the approach, the naming, or the scope.
