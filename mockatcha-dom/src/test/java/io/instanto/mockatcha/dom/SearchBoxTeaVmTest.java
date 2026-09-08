/*
 * Copyright 2026 Carl Stainton
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.instanto.mockatcha.dom;

import static io.instanto.mockatcha.Mockatcha.mock;
import static io.instanto.mockatcha.Mockatcha.never;
import static io.instanto.mockatcha.Mockatcha.verify;
import static io.instanto.mockatcha.Mockatcha.when;
import static io.instanto.mockatcha.bdd.Bdd.clock;
import static io.instanto.mockatcha.dom.Dom.find;
import static io.instanto.mockatcha.dom.Dom.findAll;
import static io.instanto.mockatcha.dom.Dom.render;
import static io.instanto.mockatcha.dom.Dom.type;
import static io.instanto.mockatcha.dom.Expect.expect;
import static org.junit.Assert.assertEquals;

import io.instanto.mockatcha.ArgumentMatchers;
import java.util.List;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.jso.browser.Window;
import org.teavm.jso.dom.events.Event;
import org.teavm.jso.dom.events.EventListener;
import org.teavm.jso.dom.html.HTMLElement;
import org.teavm.junit.SkipJVM;
import org.teavm.junit.TeaVMTestRunner;

/**
 * A component with a mocked collaborator, a controlled clock, and assertions on what it rendered.
 *
 * <p>The three libraries on the same test: Mockatcha replaces the search service, BDD stops the
 * debounce from taking real time, and this module drives the input and reads the result.
 */
@RunWith(TeaVMTestRunner.class)
@SkipJVM
public class SearchBoxTeaVmTest {

  @Rule
  public DomRule dom = new DomRule();

  @After
  public void restoreTheClock() {
    clock().uninstall();
  }

  @Test
  public void searchesOnceTheTypingStops() {
    clock().install();
    SearchService searches = mock(SearchService.class);
    when(searches.find("A-17")).thenReturn(List.of("A-17 draft", "A-17 submitted"));
    SearchBox box = renderSearchBox(searches);

    type(find("#term"), "A-17");

    // Still within the debounce, so nothing has been asked for yet.
    clock().tick(299);
    verify(searches, never()).find(ArgumentMatchers.anyString());

    clock().tick(1);

    verify(searches).find("A-17");
    assertEquals(2, findAll("#results li").size());
    expect(findAll("#results li").get(0)).toHaveText("A-17 draft");
  }

  @Test
  public void onlyTheLastThingTypedIsSearchedFor() {
    clock().install();
    SearchService searches = mock(SearchService.class);
    when(searches.find("A-18")).thenReturn(List.of("A-18 draft"));
    renderSearchBox(searches);

    type(find("#term"), "A-1");
    clock().tick(100);
    type(find("#term"), "A-18");
    clock().tick(300);

    verify(searches, never()).find("A-1");
    verify(searches).find("A-18");
    expect(find("#results li")).toHaveText("A-18 draft");
  }

  private SearchBox renderSearchBox(SearchService searches) {
    render("<input id='term'><ul id='results'></ul>");
    return new SearchBox(find("#term"), find("#results"), searches);
  }

  public interface SearchService {
    List<String> find(String term);
  }

  /** Waits for typing to stop, then asks the service and lists what comes back. */
  static final class SearchBox {

    private static final int DEBOUNCE_MILLISECONDS = 300;

    private final HTMLElement results;
    private final SearchService searches;
    private int pending;

    SearchBox(HTMLElement term, HTMLElement results, SearchService searches) {
      this.results = results;
      this.searches = searches;
      term.addEventListener("input", (EventListener<Event>) event -> restart(valueOf(term)));
    }

    private void restart(String term) {
      Window.clearTimeout(pending);
      pending = Window.setTimeout(() -> show(searches.find(term)), DEBOUNCE_MILLISECONDS);
    }

    private void show(List<String> found) {
      StringBuilder markup = new StringBuilder();
      for (String entry : found) {
        markup.append("<li>").append(entry).append("</li>");
      }
      Rendering.setInnerHtml(results, markup.toString());
    }

    @org.teavm.jso.JSBody(params = {"element"}, script = "return element.value;")
    private static native String valueOf(HTMLElement element);
  }

  static final class Rendering {
    @org.teavm.jso.JSBody(params = {"element", "html"}, script = "element.innerHTML = html;")
    static native void setInnerHtml(HTMLElement element, String html);
  }
}
