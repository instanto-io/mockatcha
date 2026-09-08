/*
 * Copyright 2026 Carl Stainton
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.instanto.mockatcha.dom;

import static io.instanto.mockatcha.dom.Dom.blur;
import static io.instanto.mockatcha.dom.Dom.click;
import static io.instanto.mockatcha.dom.Dom.find;
import static io.instanto.mockatcha.dom.Dom.fire;
import static io.instanto.mockatcha.dom.Dom.focus;
import static io.instanto.mockatcha.dom.Dom.hover;
import static io.instanto.mockatcha.dom.Dom.render;
import static io.instanto.mockatcha.dom.Dom.tab;
import static io.instanto.mockatcha.dom.Dom.tabBack;
import static io.instanto.mockatcha.dom.Dom.unhover;
import static io.instanto.mockatcha.dom.Expect.expect;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.jso.dom.events.Event;
import org.teavm.jso.dom.events.EventListener;
import org.teavm.junit.SkipJVM;
import org.teavm.junit.TeaVMTestRunner;

/** Driving pointer, focus, and general browser events the way someone using the page would. */
@RunWith(TeaVMTestRunner.class)
@SkipJVM
public class EventsTeaVmTest {

  @Rule
  public DomRule dom = new DomRule();

  private final List<String> seen = new ArrayList<>();

  @Test
  public void clickReachesAListener() {
    render("<button>Save</button>");
    listen(find("button"), "click");

    click("button");

    assertEquals(List.of("click"), seen);
  }

  @Test
  public void eventsBubbleToAnAncestor() {
    render("<div id='outer'><button type='button'>Save</button></div>");
    listen(find("#outer"), "click");

    click("button");

    assertEquals(List.of("click"), seen);
  }

  @Test
  public void anyEventCanBeDispatched() {
    render("<div id='outer'></div>");
    listen(find("#outer"), "inventory-refreshed");

    fire(find("#outer"), "inventory-refreshed");

    assertEquals(List.of("inventory-refreshed"), seen);
  }

  @Test
  public void focusAndBlurUseTheBrowsersActiveElement() {
    render("<input id='term'>");

    focus("#term");
    expect(find("#term")).toHaveFocus();

    blur("#term");
    expect(find("#term")).not().toHaveFocus();
  }

  @Test
  public void tabsThroughFocusableElementsInDocumentOrder() {
    render("<button id='first'>First</button><input id='second'><a id='third' href='#'>Third</a>");

    tab();
    expect(find("#first")).toHaveFocus();
    tab();
    expect(find("#second")).toHaveFocus();
    tabBack();
    expect(find("#first")).toHaveFocus();
  }

  @Test
  public void hoverReportsPointerEntryAndExit() {
    render("<button id='details'>Details</button>");
    listen(find("#details"), "mouseover");
    listen(find("#details"), "mouseenter");
    listen(find("#details"), "mouseout");
    listen(find("#details"), "mouseleave");

    hover("#details");
    unhover("#details");

    assertEquals(List.of("mouseover", "mouseenter", "mouseout", "mouseleave"), seen);
  }

  private void listen(org.teavm.jso.dom.html.HTMLElement element, String type) {
    element.addEventListener(type, (EventListener<Event>) event -> seen.add(type));
  }
}
