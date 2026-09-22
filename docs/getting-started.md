# Getting started

## Choose where the test runs

A test using Mockatcha runs on a JVM or through TeaVM. The library API is the
same either way, so the choice is about what the code under test needs, not
about how you write the test.

Run it on a JVM for portable domain logic, algorithms, and state machines. A JVM
test starts without compiling the program or launching a browser.

Run it through TeaVM when:

- the code uses timers, the DOM, canvas, storage, or another browser API;
- a collaborator depends on WebSocket, IndexedDB, or another browser-only API;
  or
- the assertion concerns JavaScript produced by TeaVM, such as emulated `long`
  arithmetic, collection iteration order, or browser date and number formats.

A TeaVM test usually runs in a browser, but it runs wherever that compiled
output runs. Tests for Cloudflare Workers run under a Miniflare runner, and
Mockatcha needs nothing from the host to work there, because it is compiled into
the program.

## How a mock is made

TeaVM compiles the complete program ahead of time, so Mockatcha generates each
mock while TeaVM compiles the test. This leads to two rules there:

- Pass a class literal, such as `mock(ProfileRepository.class)`, so Mockatcha can
  identify the type during compilation.
- An invalid request, such as a final class, fails during compilation.

On a JVM the mock is made when the test asks for it. An interface becomes a
`Proxy`, and a class gets a subclass generated at that moment. This leads to two
rules there:

- An invalid request fails when the test runs, not before, and says which rule
  the type broke.
- Mocking a class needs its package open to Mockatcha. That holds on the class
  path. Inside a strong module the request is refused, and mocking an interface
  the class implements is the way through.

## Add the dependencies

Add Mockatcha Core and JUnit 4 to the test classpath. Mockatcha uses JUnit 4 and
does not supply it:

```xml
<dependency>
  <groupId>io.instanto</groupId>
  <artifactId>mockatcha-core</artifactId>
  <version>0.1.0-SNAPSHOT</version>
  <scope>test</scope>
</dependency>

<dependency>
  <groupId>junit</groupId>
  <artifactId>junit</artifactId>
  <version>4.13.2</version>
  <scope>test</scope>
</dependency>
```

That is everything a JVM test needs. Mockatcha brings what it uses to build a
mock, and a JVM test loads no TeaVM class.

A test that runs through TeaVM needs TeaVM's class library and its JUnit runner
as well:

```xml
<dependency>
  <groupId>org.teavm</groupId>
  <artifactId>teavm-classlib</artifactId>
  <version>0.15.0</version>
  <scope>test</scope>
</dependency>

<dependency>
  <groupId>org.teavm</groupId>
  <artifactId>teavm-junit</artifactId>
  <version>0.15.0</version>
  <scope>test</scope>
</dependency>
```

To use `@Rule` in a TeaVM test, add `io.instanto:teavm-rule-support` and declare
it before `teavm-junit`. Without it the runner still runs `@Before`, `@Test` and
`@After`, but ignores rules. See
[Strictness and lifecycle](strictness-and-lifecycle.md).

Configure Surefire to send those tests to a browser:

```xml
<systemPropertyVariables>
  <teavm.junit.target>${project.build.directory}/js-tests</teavm.junit.target>
  <teavm.junit.js.runner>browser-chrome</teavm.junit.js.runner>
</systemPropertyVariables>
```

### Choose and install the browser runner

These elements define JVM system properties read by `TeaVMTestRunner`:

- `teavm.junit.target` is the directory where TeaVM writes the generated
  JavaScript test files and runner pages.
- `teavm.junit.js.runner` selects how TeaVM executes those files.

The value `browser-chrome` tells TeaVM to start an external process using the
command `chrome`. It is a runner selector, not a Maven dependency or a bundled
browser. Install Chrome or Chromium and ensure that `chrome` resolves to its
executable on the `PATH` inherited by Maven. If the Chromium executable has a
different name, provide a `chrome` executable or wrapper on that `PATH`.

The browser must be installed on developer machines and CI agents. This
repository maps its `mockatcha.test.browser` property to
`teavm.junit.js.runner` and defaults it to `browser-chrome`. Run its tests with
TeaVM's Firefox runner like this:

```bash
mvn test -Dmockatcha.test.browser=browser-firefox
```

That setting expects the external command `firefox` to be available instead.

Downstream projects can use the snapshots published at `packages.instanto.io`.
The registry serves all Instanto artifacts by Maven coordinate from one URL.

When working from a clone, install the artifacts locally first:

```bash
./mvnw install
```

## Write the first test

Suppose a component gets its text from a service:

```java
public interface GreetingService {
    String hello(String name);
}
```

The test replaces the service, arranges its result, and runs the real component:

```java
import static io.instanto.mockatcha.Mockatcha.mock;
import static io.instanto.mockatcha.Mockatcha.verify;
import static io.instanto.mockatcha.Mockatcha.when;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

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

`mock(...)` creates a stand-in. Unstubbed methods return Java's empty value:
`null`, `0`, or `false`. `when(...)` arranges a result for one call.
`verify(...)` checks that a call occurred.

That test runs on a JVM, and needs no TeaVM configuration.

## Run the same test through TeaVM

Add TeaVM's runner. Nothing in the test body changes:

```java
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.SkipJVM;
import org.teavm.junit.TeaVMTestRunner;

@RunWith(TeaVMTestRunner.class)
@SkipJVM
public class GreetingComponentTest {

    @Test
    public void showsTheGreetingFromTheService() {
        // As above.
    }
}
```

`@RunWith(TeaVMTestRunner.class)` compiles the test and runs it in a browser.
`@SkipJVM` keeps it off the JVM. Leave `@SkipJVM` out and the runner runs the
same test in both places, which is worth doing when nothing in the test needs a
browser. Add it when something does.

Continue with [stubbing](stubbing.md).

[Documentation index](README.md) · [Project README](../README.md)
