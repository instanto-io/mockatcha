# BDD DOM

Tests for code that renders. A container per test, queries and events scoped to
it, and assertions that say which element was wrong.

```java
@Rule
public DomRule dom = new DomRule();

@Test
public void showsTheGreeting() {
    render("<h1 class='title'>Hello Ada</h1>");

    expect(find("h1")).toHaveText("Hello Ada").toHaveClass("title");
}
```

It works on its own, and beside [Mockatcha](../README.md) and
[BDD](../bdd/README.md) when a test needs a mocked collaborator or a
controlled clock as well.

| | |
| --- | --- |
| **Decide** | [Why it exists](#why-it-exists) |
| **Get started** | [Setup](#setup) · [A container per test](#a-container-per-test) |
| **Drive it** | [Find things](#find-things) · [Events](#events) |
| **Check it** | [Expectations](#expectations) |
| **Together** | [All three libraries](#all-three-libraries) |

## Why it exists

Every test in a run shares one document. Elements left behind by one test are
found by the next, listeners attached earlier keep firing, and a failure surfaces
in a test that did nothing wrong. Giving each test a container of its own,
created before it and removed after, and scoping queries and events to that
container, keeps one test out of the next.

The second half is what a failure tells you. This:

```java
assertEquals("Hello Ada", find("h1").getTextContent());
```

reports two strings and leaves you to work out which element it read, whether
the selector matched what you thought, and whether anything matched at all.
`expect(find("h1"))` still holds the selector and the element, so it can print
them:

```
Wanted text "Hello Ada" but found "Goodbye".
The element is:
<h1 class="title">Goodbye</h1>
```

## Setup

```xml
<dependency>
  <groupId>io.instanto</groupId>
  <artifactId>bdd-dom</artifactId>
  <version>0.1.0-SNAPSHOT</version>
  <scope>test</scope>
</dependency>
```

## A container per test

`render(markup)` puts markup into a container attached to the document, and
every query runs inside that container. Markup from one test is therefore
invisible to the next, provided the container is removed between them.

`DomRule` removes it:

```java
@Rule
public DomRule dom = new DomRule();
```

Rules need a runner that runs them, which TeaVM's does once the
[patch in this repository](../teavm-rule-support/README.md) is on the test
classpath. Where that is not available, extend `DomTest` instead, which does the
same cleanup from an `@After` method. Calling `Dom.reset()` yourself works too.

Without any of those, containers accumulate in the document and a query can find
an element left behind by an earlier test.

## Find things

```java
HTMLElement heading = find("h1");           // fails if there is no match
List<HTMLElement> rows = findAll(".row");   // empty list if there are none
HTMLElement save = findByText("Save");
HTMLElement maybe = Dom.findOrNull(".x");   // null if there is no match
```

When `find` matches nothing it fails with the container's current markup, which
is usually enough to see why:

```
No element matching ".missing".
The container holds:
<p>only a paragraph</p>
```

## Events

```java
click("button");
type(find("#term"), "A-17");        // sets the value, then fires input and change
press(find("#term"), "Enter");      // fires keydown and keyup with that key
fire(find("form"), "submit");
```

`type` reports the change the way a browser does while someone types, so a
component listening for `input` sees it. Events bubble, so a listener on an
ancestor is reached.

Each of them returns the element it acted on, so an expectation about that same
element can follow in one statement:

```java
expect(click("#agree")).toBeChecked();
expect(type(find("#term"), "A-17")).toHaveValue("A-17");
```

## Expectations

```java
expect(find("h1")).toHaveText("Hello Ada");
expect(find(".row")).toHaveClass("selected").not().toHaveClass("muted");
expect(find("#name")).toHaveValue("A-17").toHaveAttribute("id", "name");
expect(find("#panel")).toBeVisible();
expect(find("#submit")).toBeDisabled();
```

Also `toContainText`, `toBeHidden`, `toBeEnabled`, `toBeChecked`,
`toBeInTheDocument`, `toHaveFocus`, `toBeEmpty`, `toContainElement`,
`toHaveStyle`, and `toContainHtml`. The last six are the ones both
jasmine-jquery and jest-dom settled on.

`not()` inverts the expectation that follows it, and only that one, as it does in
Jasmine. They chain, and a failure prints the element:

```
Wanted text "Goodbye" but found "Hello Ada".
The element is:
<h1 class="title">Hello Ada</h1>
```

## Work that finishes later

`clock().tick(...)` runs anything scheduled through a timer, which covers polling,
debouncing, retries and animation.

Promise callbacks are a different matter. The browser runs them once the call
stack empties, which is after the test method returns, so a test cannot observe
them however long it waits.
[`PromiseTimingTeaVmTest`](src/test/java/io/instanto/bdd/dom/PromiseTimingTeaVmTest.java)
shows that directly.

Code written against a result type that completes on the spot is testable
without waiting. [Sarto Async](https://github.com/cstainton/sarto-async)'s
`PortableResult` calls its subscribers on the thread that completes it, so a
test can complete the result itself and assert immediately.

## All three libraries

A component that waits for typing to stop, then asks a service and lists what
comes back. Mockatcha replaces the service, BDD stops the debounce from
taking real time, and this module drives the input and reads the result:

```java
clock().install();
SearchService searches = mock(SearchService.class);
when(searches.find("A-17")).thenReturn(List.of("A-17 draft", "A-17 submitted"));

render("<input id='term'><ul id='results'></ul>");
new SearchBox(find("#term"), find("#results"), searches);

type(find("#term"), "A-17");

clock().tick(299);
verify(searches, never()).find(anyString());

clock().tick(1);

verify(searches).find("A-17");
expect(findAll("#results li").get(0)).toHaveText("A-17 draft");
```

The whole test runs in a few milliseconds and never waits for a real timer. See
[`SearchBoxTeaVmTest`](src/test/java/io/instanto/bdd/dom/SearchBoxTeaVmTest.java).
