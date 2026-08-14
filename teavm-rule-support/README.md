# JUnit rules for TeaVM's test runner

> This module replaces two classes that TeaVM publishes, by shadowing them on the
> classpath. Add it to a project deliberately, never as a transitive dependency:
> arriving that way it would change how the project's tests run without saying
> so.

`TeaVMTestRunner` runs `@Before` and `@After` methods but not `@Rule` fields, so
a rule silently does nothing in a browser test. This module is a working fix,
running in Chrome, and [`teavm-junit-rules.patch`](teavm-junit-rules.patch) is
the same change against `teavm` at tag 0.15.0.

Mockatcha ships and uses this module so that `MockatchaRule` can be its preferred
test lifecycle. Any TeaVM/JUnit project can put it ahead of `teavm-junit` and
use ordinary JUnit `TestRule` fields and no-argument rule methods.

`StrictTest` is Mockatcha's inheritance-based compatibility fallback for a
consumer that cannot install this rule support; it is not part of TeaVM or
JUnit. `MockatchaSession` is the framework-neutral lifecycle underneath both
Mockatcha adapters and is intended for custom or non-JUnit harnesses.

The implementation does not depend on Mockatcha. The classes under
`org/teavm/junit` and `org/teavm/classlib` shadow the ones in the TeaVM jars,
because test classes come before dependencies on the classpath. This keeps the
support independently reusable and testable without building TeaVM.

## What the change does

`TestEntryPointTransformer` already generates the bodies of
`TestEntryPoint.before()` and `after()`, emitting a direct call to each
annotated method it finds in the test class hierarchy. It now also generates a
body for a new `applyRules` method:

```java
static Statement applyRules(Statement base, String name) {
    Statement statement = base;
    Description description = describe("com.example.MyTest", name);
    statement = ((TestRule) testCase.firstRule).apply(statement, description);
    statement = ((TestRule) testCase.secondRule).apply(statement, description);
    return statement;
}
```

Rule fields are read and rule methods are called directly, so runtime reflection
is not needed. When a test declares no rules, the generated body returns its
argument unchanged.

`TestEntryPoint.run` then wraps each launcher in a `Statement` and hands it to
the rules:

```java
for (Launcher launcher : launchers) {
    applyRules(new LaunchStatement(launcher), name).evaluate();
}
```

`LaunchStatement.evaluate` runs `before()`, the test, and `after()`, placing the
rules outside the `@Before` and `@After` methods as JUnit does. Teardown is
always attempted. If it fails, its exception is thrown when the test body
passed, or suppressed by the existing test failure when the body failed too.

## The classlib addition

`org.junit.runner.Description` holds its children in a
`ConcurrentLinkedQueue`, which TeaVM had no emulation for, so the real JDK class
was compiled and its static initialiser reached `MethodHandles.lookup()`:

```
Method java.lang.invoke.MethodHandles.lookup()Ljava/lang/invoke/MethodHandles$Lookup; was not found
    at java.util.concurrent.ConcurrentLinkedQueue.<clinit>
    at org.junit.runner.Description.<init>
```

`TConcurrentLinkedQueue` fixes that. It is a queue backed by an `ArrayDeque`,
which is all a single-threaded runtime needs, and it is useful beyond this
change: any code constructing a `Description`, or using the queue directly, hits
the same wall today.

## Ordering

Following JUnit's `RuleContainer`, rules with a higher `order` are applied first
and therefore end up inner. Where the order is equal, rule methods are applied
before rule fields. Within either group, declaration order is preserved with
superclasses first.

## What is left for a MethodRule

A `MethodRule` receives a `FrameworkMethod`, which wraps
`java.lang.reflect.Method`, and JUnit rejects a null one in its constructor.
TeaVM can produce a `Method` only for a method annotated `@Reflectable`.

Supporting it would require the transformer to add `@Reflectable` to each test
method and construct a `FrameworkMethod` from
`testClass.getDeclaredMethod(name)`. This patch supports the newer `TestRule`
interface and reports a clear error for `MethodRule` instead:

```
Field com.example.MyTest.rule of type com.example.SomeMethodRule is a MethodRule,
which TeaVM cannot run because it needs java.lang.reflect.Method. Use a TestRule.
```

## Using it in any TeaVM/JUnit module

Add the dependency, above `teavm-junit` so that its copies of the runner classes
come first on the classpath:

```xml
<dependency>
  <groupId>io.instanto</groupId>
  <artifactId>teavm-rule-support</artifactId>
  <version>0.1.0-SNAPSHOT</version>
  <scope>test</scope>
</dependency>
```

No registration or explicit call is required. The support is applied while
TeaVM compiles the test, after which `@Rule` fields and no-argument rule methods
are honoured.

With this dependency present, any JUnit `TestRule` is honoured. A Mockatcha test
should normally use:

```java
@Rule
public MockatchaRule mockatcha = new MockatchaRule();
```

There is no need for that test to extend `StrictTest` or open a
`MockatchaSession` itself.

[`TimesheetRuleTest`](../mockatcha-examples/src/test/java/io/instanto/mockatcha/examples/TimesheetRuleTest.java)
uses this setup and proves that the rule runs.

## Running it

```bash
mvn -pl teavm-rule-support test
```

`RuleSupportTeaVmTest` declares two rule fields and one rule method, and checks
that all three wrap the test, nest and unwind in the right order, and receive a
`Description` naming the test class and method. `RuleOrderTeaVmTest` covers
`@Rule(order = ...)`, `AfterFailureTeaVmTest` proves an isolated teardown
failure is propagated, and `MockatchaRuleTeaVmTest` exercises a real consumer
without coupling the implementation to it.
