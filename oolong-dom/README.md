# Oolong DOM

Tests for code that renders. A container per test, queries and events scoped to
it, and assertions that say which element was wrong.

```java
@Rule
public DomRule dom = new DomRule();

@Test
public void showsTheGreeting() {
    render("<h1 class='title'>Hello Ada</h1>");

    assertThat(find("h1")).hasText("Hello Ada").hasClass("title");
}
```

It works on its own, and beside [Mockatcha](../README.md) and
[Oolong](../oolong/README.md) when a test needs a mocked collaborator or a
controlled clock as well.

| | |
| --- | --- |
| **Get started** | [Setup](#setup) · [A container per test](#a-container-per-test) |
| **Drive it** | [Find things](#find-things) · [Events](#events) |
| **Check it** | [Assertions](#assertions) |
| **Together** | [All three libraries](#all-three-libraries) |

## Setup

```xml
<dependency>
  <groupId>io.instanto</groupId>
  <artifactId>oolong-dom</artifactId>
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
click(find("button"));
type(find("#term"), "A-17");        // sets the value, then fires input and change
press(find("#term"), "Enter");      // fires keydown and keyup with that key
fire(find("form"), "submit");
```

`type` reports the change the way a browser does while someone types, so a
component listening for `input` sees it. Events bubble, so a listener on an
ancestor is reached.

## Assertions

```java
assertThat(find("h1")).hasText("Hello Ada");
assertThat(find(".row")).hasClass("selected").doesNotHaveClass("muted");
assertThat(find("#name")).hasValue("A-17").hasAttribute("id", "name");
assertThat(find("#panel")).isVisible();
assertThat(find("#submit")).isDisabled();
```

Also `containsText`, `isHidden`, `isEnabled`, and `isChecked`. They chain, and a
failure prints the element:

```
Wanted text "Goodbye" but found "Hello Ada".
The element is:
<h1 class="title">Hello Ada</h1>
```

## All three libraries

A component that waits for typing to stop, then asks a service and lists what
comes back. Mockatcha replaces the service, Oolong stops the debounce from
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
assertThat(findAll("#results li").get(0)).hasText("A-17 draft");
```

The whole test runs in a few milliseconds and never waits for a real timer. See
[`SearchBoxTeaVmTest`](src/test/java/io/instanto/oolong/dom/SearchBoxTeaVmTest.java).
