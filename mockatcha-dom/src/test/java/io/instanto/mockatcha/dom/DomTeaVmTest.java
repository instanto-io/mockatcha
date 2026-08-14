package io.instanto.mockatcha.dom;

import static io.instanto.mockatcha.dom.Dom.find;
import static io.instanto.mockatcha.dom.Dom.findAll;
import static io.instanto.mockatcha.dom.Dom.findByText;
import static io.instanto.mockatcha.dom.Dom.findByLabelText;
import static io.instanto.mockatcha.dom.Dom.findByRole;
import static io.instanto.mockatcha.dom.Dom.findByTestId;
import static io.instanto.mockatcha.dom.Dom.render;
import static io.instanto.mockatcha.dom.Expect.expect;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.SkipJVM;
import org.teavm.junit.TeaVMTestRunner;

/** Rendering into a container, finding things in it, and asserting on what is there. */
@RunWith(TeaVMTestRunner.class)
@SkipJVM
public class DomTeaVmTest {

  @Rule
  public DomRule dom = new DomRule();

  @Test
  public void findsAnElementBySelector() {
    render("<h1 class='title'>Hello Ada</h1>");

    expect(find("h1")).toHaveText("Hello Ada").toHaveClass("title");
  }

  @Test
  public void findsEveryMatchingElement() {
    render("<ul><li>one</li><li>two</li><li>three</li></ul>");

    assertEquals(3, findAll("li").size());
    expect(findAll("li").get(1)).toHaveText("two");
  }

  @Test
  public void findsAnElementByItsText() {
    render("<div><button id='save'>Save</button><button>Cancel</button></div>");

    expect(findByText("Save")).toHaveAttribute("id", "save");
  }

  @Test
  public void textLookupDoesNotReturnAWrapperContainingTheSameText() {
    render("<div id='wrapper'><button id='save'>Save</button></div>");

    expect(findByText("Save")).toHaveAttribute("id", "save");
  }

  @Test
  public void findsControlsByImplicitRoleAndAccessibleName() {
    render("<button id='save' aria-label='Save timesheet'>disk icon</button>");

    expect(findByRole("button", "Save timesheet")).toHaveAttribute("id", "save");
  }

  @Test
  public void roleQueriesIgnoreElementsHiddenFromAccessibility() {
    render("<button hidden>Hidden</button><button id='shown'>Shown</button>");

    expect(findByRole("button")).toHaveAttribute("id", "shown");
  }

  @Test
  public void findsControlsByExplicitAndImplicitLabels() {
    render(
        "<label for='account'>Account</label><input id='account'>"
            + "<label>Hours<input id='hours'></label>");

    expect(findByLabelText("Account")).toHaveAttribute("id", "account");
    expect(findByLabelText("Hours")).toHaveAttribute("id", "hours");
  }

  @Test
  public void ariaLabelOverridesTextFromAWrappingLabel() {
    render("<label>Sat<input id='hours' aria-label='OPS-142 2026-07-18'></label>");

    expect(findByLabelText("OPS-142 2026-07-18")).toHaveAttribute("id", "hours");
  }

  @Test
  public void findsAnElementByTestIdWithoutTreatingItAsCss() {
    render("<section data-testid='account:17'>details</section>");

    expect(findByTestId("account:17")).toHaveText("details");
  }

  @Test
  public void saysWhatWasThereWhenNothingMatches() {
    render("<p>only a paragraph</p>");

    AssertionError failure = assertThrows(AssertionError.class, () -> find(".missing"));

    assertTrue(failure.getMessage(), failure.getMessage().contains("No element matching"));
    assertTrue(failure.getMessage(), failure.getMessage().contains("only a paragraph"));
  }

  @Test
  public void showsTheElementWhenAnAssertionFails() {
    render("<h1 class='title'>Hello Ada</h1>");

    AssertionError failure =
        assertThrows(AssertionError.class, () -> expect(find("h1")).toHaveText("Goodbye"));

    assertTrue(failure.getMessage(), failure.getMessage().contains("Hello Ada"));
    assertTrue(failure.getMessage(), failure.getMessage().contains("<h1"));
  }

  @Test
  public void assertsOnAttributesClassesAndState() {
    render("<input id='name' value='A-17' class='field wide' disabled>");

    expect(find("#name"))
        .toHaveValue("A-17")
        .toHaveClass("wide")
        .not().toHaveClass("narrow")
        .toHaveAttribute("id", "name")
        .toBeDisabled();
  }

  @Test
  public void assertsOnVisibility() {
    render("<p id='shown'>here</p><p id='gone' style='display:none'>hidden</p>");

    expect(find("#shown")).toBeVisible();
    expect(find("#gone")).toBeHidden();
  }

  @Test
  public void doesNotSeeMarkupFromAnotherTest() {
    // The rule removed the previous test's container, so nothing here is left over.
    assertNull(Dom.findOrNull("#name"));
    assertNull(Dom.findOrNull("h1"));
  }
}
