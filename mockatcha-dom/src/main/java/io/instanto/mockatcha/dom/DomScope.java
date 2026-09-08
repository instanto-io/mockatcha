/*
 * Copyright 2026 Carl Stainton
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.instanto.mockatcha.dom;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import org.teavm.jso.dom.html.HTMLElement;
import org.teavm.jso.dom.xml.Element;
import org.teavm.jso.dom.xml.NodeList;

/** Queries whose search is restricted to the descendants of one element. */
public final class DomScope {

  private final HTMLElement root;

  DomScope(HTMLElement root) {
    this.root = Objects.requireNonNull(root, "root");
  }

  /** The element that bounds this scope. */
  public HTMLElement root() {
    return root;
  }

  /** Moves focus within this scope, using its own document's active element. */
  public HTMLElement tab() {
    return Browser.tab(root, false);
  }

  /** Moves focus backwards within this scope. */
  public HTMLElement shiftTab() {
    return Browser.tab(root, true);
  }

  /** Creates a narrower query scope inside this one. */
  public DomScope within(HTMLElement element) {
    Objects.requireNonNull(element, "element");
    if (element != root && !Browser.contains(root, element)) {
      throw new IllegalArgumentException("The scoped element is outside this DOM scope");
    }
    return new DomScope(element);
  }

  /** Finds exactly one CSS match. */
  public HTMLElement find(String selector) {
    return one("CSS selector \"" + selector + "\"", findAll(selector), false);
  }

  /** Finds zero or one CSS match, returning null when there is none. */
  public HTMLElement findOrNull(String selector) {
    return one("CSS selector \"" + selector + "\"", findAll(selector), true);
  }

  /** Finds every CSS match in document order. */
  public List<HTMLElement> findAll(String selector) {
    Objects.requireNonNull(selector, "selector");
    NodeList<? extends Element> found = root.querySelectorAll(selector);
    List<HTMLElement> elements = new ArrayList<>(found.getLength());
    for (int index = 0; index < found.getLength(); index++) {
      elements.add((HTMLElement) found.get(index));
    }
    return elements;
  }

  public HTMLElement findByText(String text) {
    return findByText(TextMatch.exact(text));
  }

  public HTMLElement findByText(TextMatch text) {
    return one("text " + text.description(), byText(text), false);
  }

  public HTMLElement findByTextOrNull(String text) {
    return findByTextOrNull(TextMatch.exact(text));
  }

  public HTMLElement findByTextOrNull(TextMatch text) {
    return one("text " + text.description(), byText(text), true);
  }

  public List<HTMLElement> findAllByText(String text) {
    return findAllByText(TextMatch.exact(text));
  }

  public List<HTMLElement> findAllByText(TextMatch text) {
    return byText(Objects.requireNonNull(text, "text"));
  }

  public HTMLElement awaitByText(String text) {
    return awaitByText(TextMatch.exact(text));
  }

  public HTMLElement awaitByText(TextMatch text) {
    return Dom.waitFor(() -> findByText(text));
  }

  public HTMLElement findByRole(String role) {
    return findByRole(RoleQuery.role(role));
  }

  public HTMLElement findByRole(String role, String accessibleName) {
    return findByRole(RoleQuery.role(role).named(accessibleName));
  }

  public HTMLElement findByRole(RoleQuery query) {
    return one(query.description(), matching(query::matches), false);
  }

  public HTMLElement findByRoleOrNull(String role) {
    return findByRoleOrNull(RoleQuery.role(role));
  }

  public HTMLElement findByRoleOrNull(String role, String accessibleName) {
    return findByRoleOrNull(RoleQuery.role(role).named(accessibleName));
  }

  public HTMLElement findByRoleOrNull(RoleQuery query) {
    return one(query.description(), matching(query::matches), true);
  }

  public List<HTMLElement> findAllByRole(String role) {
    return findAllByRole(RoleQuery.role(role));
  }

  public List<HTMLElement> findAllByRole(String role, String accessibleName) {
    return findAllByRole(RoleQuery.role(role).named(accessibleName));
  }

  public List<HTMLElement> findAllByRole(RoleQuery query) {
    return matching(Objects.requireNonNull(query, "query")::matches);
  }

  public HTMLElement awaitByRole(String role) {
    return awaitByRole(RoleQuery.role(role));
  }

  public HTMLElement awaitByRole(String role, String accessibleName) {
    return awaitByRole(RoleQuery.role(role).named(accessibleName));
  }

  public HTMLElement awaitByRole(RoleQuery query) {
    return Dom.waitFor(() -> findByRole(query));
  }

  public HTMLElement findByLabelText(String text) {
    return findByLabelText(TextMatch.exact(text));
  }

  public HTMLElement findByLabelText(TextMatch text) {
    return one("label text " + text.description(), labelled(text), false);
  }

  public HTMLElement findByLabelTextOrNull(String text) {
    return findByLabelTextOrNull(TextMatch.exact(text));
  }

  public HTMLElement findByLabelTextOrNull(TextMatch text) {
    return one("label text " + text.description(), labelled(text), true);
  }

  public List<HTMLElement> findAllByLabelText(String text) {
    return findAllByLabelText(TextMatch.exact(text));
  }

  public List<HTMLElement> findAllByLabelText(TextMatch text) {
    return labelled(Objects.requireNonNull(text, "text"));
  }

  public HTMLElement awaitByLabelText(String text) {
    return awaitByLabelText(TextMatch.exact(text));
  }

  public HTMLElement awaitByLabelText(TextMatch text) {
    return Dom.waitFor(() -> findByLabelText(text));
  }

  public HTMLElement findByPlaceholderText(String text) {
    return findByPlaceholderText(TextMatch.exact(text));
  }

  public HTMLElement findByPlaceholderText(TextMatch text) {
    return one("placeholder " + text.description(), attribute("placeholder", text), false);
  }

  public HTMLElement findByPlaceholderTextOrNull(String text) {
    return findByPlaceholderTextOrNull(TextMatch.exact(text));
  }

  public HTMLElement findByPlaceholderTextOrNull(TextMatch text) {
    return one("placeholder " + text.description(), attribute("placeholder", text), true);
  }

  public List<HTMLElement> findAllByPlaceholderText(String text) {
    return findAllByPlaceholderText(TextMatch.exact(text));
  }

  public List<HTMLElement> findAllByPlaceholderText(TextMatch text) {
    return attribute("placeholder", text);
  }

  public HTMLElement awaitByPlaceholderText(String text) {
    return awaitByPlaceholderText(TextMatch.exact(text));
  }

  public HTMLElement awaitByPlaceholderText(TextMatch text) {
    return Dom.waitFor(() -> findByPlaceholderText(text));
  }

  public HTMLElement findByDisplayValue(String value) {
    return findByDisplayValue(TextMatch.exact(value));
  }

  public HTMLElement findByDisplayValue(TextMatch value) {
    return one("display value " + value.description(), byDisplayValue(value), false);
  }

  public HTMLElement findByDisplayValueOrNull(String value) {
    return findByDisplayValueOrNull(TextMatch.exact(value));
  }

  public HTMLElement findByDisplayValueOrNull(TextMatch value) {
    return one("display value " + value.description(), byDisplayValue(value), true);
  }

  public List<HTMLElement> findAllByDisplayValue(String value) {
    return findAllByDisplayValue(TextMatch.exact(value));
  }

  public List<HTMLElement> findAllByDisplayValue(TextMatch value) {
    return byDisplayValue(value);
  }

  public HTMLElement awaitByDisplayValue(String value) {
    return awaitByDisplayValue(TextMatch.exact(value));
  }

  public HTMLElement awaitByDisplayValue(TextMatch value) {
    return Dom.waitFor(() -> findByDisplayValue(value));
  }

  public HTMLElement findByAltText(String text) {
    return findByAltText(TextMatch.exact(text));
  }

  public HTMLElement findByAltText(TextMatch text) {
    return findByAttribute("alt text ", "alt", text, false);
  }

  public HTMLElement findByAltTextOrNull(String text) {
    return findByAltTextOrNull(TextMatch.exact(text));
  }

  public HTMLElement findByAltTextOrNull(TextMatch text) {
    return findByAttribute("alt text ", "alt", text, true);
  }

  public List<HTMLElement> findAllByAltText(String text) {
    return findAllByAltText(TextMatch.exact(text));
  }

  public List<HTMLElement> findAllByAltText(TextMatch text) {
    return attribute("alt", text);
  }

  public HTMLElement awaitByAltText(String text) {
    return awaitByAltText(TextMatch.exact(text));
  }

  public HTMLElement awaitByAltText(TextMatch text) {
    return Dom.waitFor(() -> findByAltText(text));
  }

  public HTMLElement findByTitle(String text) {
    return findByTitle(TextMatch.exact(text));
  }

  public HTMLElement findByTitle(TextMatch text) {
    return findByAttribute("title ", "title", text, false);
  }

  public HTMLElement findByTitleOrNull(String text) {
    return findByTitleOrNull(TextMatch.exact(text));
  }

  public HTMLElement findByTitleOrNull(TextMatch text) {
    return findByAttribute("title ", "title", text, true);
  }

  public List<HTMLElement> findAllByTitle(String text) {
    return findAllByTitle(TextMatch.exact(text));
  }

  public List<HTMLElement> findAllByTitle(TextMatch text) {
    return attribute("title", text);
  }

  public HTMLElement awaitByTitle(String text) {
    return awaitByTitle(TextMatch.exact(text));
  }

  public HTMLElement awaitByTitle(TextMatch text) {
    return Dom.waitFor(() -> findByTitle(text));
  }

  public HTMLElement findByTestId(String testId) {
    return findByAttribute("test id ", "data-testid", TextMatch.exact(testId), false);
  }

  public HTMLElement findByTestIdOrNull(String testId) {
    return findByAttribute("test id ", "data-testid", TextMatch.exact(testId), true);
  }

  public List<HTMLElement> findAllByTestId(String testId) {
    return attribute("data-testid", TextMatch.exact(testId));
  }

  public HTMLElement awaitByTestId(String testId) {
    return Dom.waitFor(() -> findByTestId(testId));
  }

  private HTMLElement findByAttribute(
      String description, String attribute, TextMatch text, boolean optional) {
    return one(description + text.description(), attribute(attribute, text), optional);
  }

  private List<HTMLElement> byDisplayValue(TextMatch text) {
    Objects.requireNonNull(text, "text");
    return matching(element -> {
      String value = Browser.value(element);
      return value != null && text.matches(value);
    });
  }

  private List<HTMLElement> labelled(TextMatch text) {
    Objects.requireNonNull(text, "text");
    return matching(element -> {
      String label = Browser.labelText(element);
      return label != null && text.matches(label);
    });
  }

  private List<HTMLElement> attribute(String name, TextMatch text) {
    Objects.requireNonNull(text, "text");
    return matching(element -> element.hasAttribute(name) && text.matches(element.getAttribute(name)));
  }

  private List<HTMLElement> byText(TextMatch text) {
    Objects.requireNonNull(text, "text");
    List<HTMLElement> matches = matching(element -> text.matches(element.getTextContent()));
    List<HTMLElement> smallest = new ArrayList<>();
    for (HTMLElement candidate : matches) {
      boolean containsMatch = false;
      for (HTMLElement other : matches) {
        if (candidate != other && Browser.contains(candidate, other)) {
          containsMatch = true;
          break;
        }
      }
      if (!containsMatch) smallest.add(candidate);
    }
    return smallest;
  }

  private List<HTMLElement> matching(Predicate<HTMLElement> predicate) {
    List<HTMLElement> result = new ArrayList<>();
    for (HTMLElement candidate : descendants()) {
      if (predicate.test(candidate)) result.add(candidate);
    }
    return result;
  }

  private List<HTMLElement> descendants() {
    NodeList<? extends Element> found = root.querySelectorAll("*");
    List<HTMLElement> elements = new ArrayList<>(found.getLength());
    for (int index = 0; index < found.getLength(); index++) {
      elements.add((HTMLElement) found.get(index));
    }
    return elements;
  }

  private HTMLElement one(String description, List<HTMLElement> matches, boolean optional) {
    if (matches.size() == 1) return matches.get(0);
    if (matches.isEmpty() && optional) return null;
    if (matches.isEmpty()) {
      throw new AssertionError("No element matching " + description + "." + markup());
    }
    StringBuilder message = new StringBuilder("Found ").append(matches.size())
        .append(" elements matching ").append(description).append(':');
    for (HTMLElement match : matches) {
      message.append("\n").append(Browser.outerHtml(match));
    }
    throw new AssertionError(message + markup());
  }

  private String markup() {
    return "\nThe query scope holds:\n" + Browser.innerHtml(root);
  }
}
