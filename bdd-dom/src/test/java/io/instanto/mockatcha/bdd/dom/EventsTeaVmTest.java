package io.instanto.mockatcha.bdd.dom;

import static io.instanto.mockatcha.bdd.dom.Dom.click;
import static io.instanto.mockatcha.bdd.dom.Dom.find;
import static io.instanto.mockatcha.bdd.dom.Dom.fire;
import static io.instanto.mockatcha.bdd.dom.Dom.press;
import static io.instanto.mockatcha.bdd.dom.Dom.render;
import static io.instanto.mockatcha.bdd.dom.Dom.type;
import static io.instanto.mockatcha.bdd.dom.Expect.expect;
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

/** Driving a component the way someone using it would. */
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
  public void typingSetsTheValueAndReportsIt() {
    render("<input id='term'>");
    listen(find("#term"), "input");
    listen(find("#term"), "change");

    type(find("#term"), "A-17");

    expect(find("#term")).toHaveValue("A-17");
    assertEquals(List.of("input", "change"), seen);
  }

  @Test
  public void keysArriveWithTheirName() {
    render("<input id='term'>");
    find("#term")
        .addEventListener(
            "keydown", (EventListener<Event>) event -> seen.add("keydown:" + keyOf(event)));

    press(find("#term"), "Enter");

    assertEquals(List.of("keydown:Enter"), seen);
  }

  @Test
  public void eventsBubbleToAnAncestor() {
    render("<form id='outer'><button>Save</button></form>");
    listen(find("#outer"), "click");

    click("button");

    assertEquals(List.of("click"), seen);
  }

  @Test
  public void anyEventCanBeDispatched() {
    render("<form id='outer'></form>");
    listen(find("#outer"), "submit");

    fire(find("#outer"), "submit");

    assertEquals(List.of("submit"), seen);
  }

  private void listen(org.teavm.jso.dom.html.HTMLElement element, String type) {
    element.addEventListener(type, (EventListener<Event>) event -> seen.add(type));
  }

  private static String keyOf(Event event) {
    return KeyReader.key(event);
  }

  static final class KeyReader {
    @org.teavm.jso.JSBody(params = {"event"}, script = "return event.key;")
    static native String key(Event event);
  }
}
