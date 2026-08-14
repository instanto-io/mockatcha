package io.instanto.mockatcha.dom;

import static io.instanto.mockatcha.dom.Dom.find;
import static io.instanto.mockatcha.dom.Dom.findAllByRole;
import static io.instanto.mockatcha.dom.Dom.findByAltText;
import static io.instanto.mockatcha.dom.Dom.findByDisplayValue;
import static io.instanto.mockatcha.dom.Dom.findByPlaceholderText;
import static io.instanto.mockatcha.dom.Dom.findByRole;
import static io.instanto.mockatcha.dom.Dom.findByRoleOrNull;
import static io.instanto.mockatcha.dom.Dom.findByText;
import static io.instanto.mockatcha.dom.Dom.findByTitle;
import static io.instanto.mockatcha.dom.Dom.render;
import static io.instanto.mockatcha.dom.Dom.within;
import static io.instanto.mockatcha.dom.Expect.expect;
import static io.instanto.mockatcha.dom.RoleQuery.role;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.SkipJVM;
import org.teavm.junit.TeaVMTestRunner;

@RunWith(TeaVMTestRunner.class)
@SkipJVM
public class ScopedQueriesTeaVmTest {

  @Rule public DomRule dom = new DomRule();

  @Test
  public void scopesAQueryToOnePartOfThePage() {
    render("<article id='first'><h2>Draft</h2><button>Save</button></article>"
        + "<article id='second'><h2>Submitted</h2><button>Save</button></article>");

    DomScope submitted = within(find("#second"));

    expect(submitted.findByRole("heading")).toHaveText("Submitted");
    expect(submitted.findByRole("button", "Save")).toHaveText("Save");
  }

  @Test
  public void singularQueriesRejectAmbiguousResultsAndShowEveryMatch() {
    render("<button id='first'>Save</button><button id='second'>Save</button>");

    AssertionError failure = assertThrows(AssertionError.class, () -> findByRole("button", "Save"));

    assertTrue(failure.getMessage(), failure.getMessage().contains("Found 2 elements"));
    assertTrue(failure.getMessage(), failure.getMessage().contains("id=\"first\""));
    assertTrue(failure.getMessage(), failure.getMessage().contains("id=\"second\""));
  }

  @Test
  public void optionalAndPluralQueriesRetainTheirCardinality() {
    render("<button>Save</button><button>Cancel</button>");

    assertNull(findByRoleOrNull(role("link")));
    assertEquals(2, findAllByRole("button").size());
    assertThrows(AssertionError.class, () -> findByRoleOrNull("button"));
  }

  @Test
  public void findsFieldsAndImagesThroughUserFacingAttributes() {
    render("<input placeholder='Search activities' value='OPS-142'>"
        + "<img alt='Airport map' src='map.png'>"
        + "<button title='Close panel'>×</button>");

    expect(findByPlaceholderText("Search activities")).toHaveValue("OPS-142");
    expect(findByDisplayValue("OPS-142")).toHaveAttribute("placeholder", "Search activities");
    expect(findByAltText("Airport map")).toHaveRole("img");
    expect(findByTitle("Close panel")).toHaveRole("button");
  }

  @Test
  public void filtersRolesByLevelAndAriaState() {
    render("<h2>Activities</h2><h3>History</h3>"
        + "<button aria-pressed='true'>Grid</button>"
        + "<button aria-expanded='false'>Filters</button>"
        + "<input type='checkbox' aria-label='Weekends' checked>"
        + "<div role='option' aria-selected='true'>July</div>");

    expect(findByRole(role("heading").level(2))).toHaveText("Activities");
    expect(findByRole(role("button").pressed(true))).toHaveText("Grid");
    expect(findByRole(role("button").expanded(false))).toHaveText("Filters");
    expect(findByRole(role("checkbox").checked(true))).toHaveAccessibleName("Weekends");
    expect(findByRole(role("option").selected(true))).toHaveText("July");
  }

  @Test
  public void matchesNormalizedNestedPartialAndCaseInsensitiveText() {
    render("<p id='message'>  Save <strong>this</strong> timesheet  </p>");

    expect(findByText("Save this timesheet")).toHaveAttribute("id", "message");
    expect(findByText(TextMatch.containingIgnoringCase("THIS TIMESHEET")))
        .toHaveAttribute("id", "message");
  }
}
