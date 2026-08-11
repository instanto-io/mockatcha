package io.instanto.oolong.dom;

import static io.instanto.oolong.dom.Dom.click;
import static io.instanto.oolong.dom.Dom.find;
import static io.instanto.oolong.dom.Dom.render;
import static io.instanto.oolong.dom.Expect.expect;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.jso.dom.html.HTMLElement;
import org.teavm.junit.SkipJVM;
import org.teavm.junit.TeaVMTestRunner;

/** The expectations, including the ones both jasmine-jquery and jest-dom settled on. */
@RunWith(TeaVMTestRunner.class)
@SkipJVM
public class ExpectTeaVmTest {

  @Rule
  public DomRule dom = new DomRule();

  @Test
  public void invertsTheExpectationThatFollows() {
    render("<p class='shown'>here</p>");

    expect(find("p")).not().toHaveClass("hidden");
    expect(find("p")).not().toHaveText("elsewhere");

    assertThrows(AssertionError.class, () -> expect(find("p")).not().toHaveClass("shown"));
  }

  @Test
  public void negationAppliesToOneExpectationOnly() {
    render("<p class='shown'>here</p>");

    // The chain returns to positive after not(), as it does in Jasmine.
    expect(find("p")).not().toHaveClass("hidden").toHaveText("here");
  }

  @Test
  public void saysWhatItWantedWhenNegated() {
    render("<p class='shown'>here</p>");

    AssertionError failure =
        assertThrows(AssertionError.class, () -> expect(find("p")).not().toHaveText("here"));

    assertTrue(failure.getMessage(), failure.getMessage().contains("Wanted no text"));
  }

  @Test
  public void checksWhetherAnElementIsStillAttached() {
    render("<div id='parent'><span id='child'>x</span></div>");
    HTMLElement child = find("#child");

    expect(child).toBeInTheDocument();

    render("<p>replaced</p>");

    expect(child).not().toBeInTheDocument();
  }

  @Test
  public void checksFocus() {
    render("<input id='first'><input id='second'>");

    find("#first").focus();

    expect(find("#first")).toHaveFocus();
    expect(find("#second")).not().toHaveFocus();
  }

  @Test
  public void checksEmptiness() {
    render("<div id='empty'>   </div><div id='full'><span>x</span></div>");

    expect(find("#empty")).toBeEmpty();
    expect(find("#full")).not().toBeEmpty();
  }

  @Test
  public void checksContainment() {
    render("<div id='parent'><span id='child'>x</span></div><p id='outside'>y</p>");

    expect(find("#parent")).toContainElement(find("#child"));
    expect(find("#parent")).not().toContainElement(find("#outside"));
  }

  @Test
  public void checksComputedStyle() {
    render("<p id='loud' style='display:block'>here</p>");

    expect(find("#loud")).toHaveStyle("display", "block");
    expect(find("#loud")).not().toHaveStyle("display", "inline");
  }

  @Test
  public void checksContainedMarkup() {
    render("<div id='panel'><span class='tag'>A-17</span></div>");

    expect(find("#panel")).toContainHtml("class=\"tag\"");
    expect(find("#panel")).not().toContainHtml("<table");
  }

  @Test
  public void reportsTheElementWhenSomethingFails() {
    render("<button id='save' disabled>Save</button>");

    AssertionError failure =
        assertThrows(AssertionError.class, () -> expect(find("#save")).toBeEnabled());

    assertTrue(failure.getMessage(), failure.getMessage().contains("<button"));
    assertTrue(failure.getMessage(), failure.getMessage().contains("disabled"));
  }

  @Test
  public void actsAndExpectsInOneStatement() {
    render("<input type='checkbox' id='agree'><input id='term'>");

    expect(click(find("#agree"))).toBeChecked();
    expect(Dom.type(find("#term"), "A-17")).toHaveValue("A-17");
  }

  @Test
  public void readsStateAfterTheUserActs() {
    render("<input type='checkbox' id='agree'>");

    expect(find("#agree")).not().toBeChecked();
    click(find("#agree"));
    expect(find("#agree")).toBeChecked();
  }
}
