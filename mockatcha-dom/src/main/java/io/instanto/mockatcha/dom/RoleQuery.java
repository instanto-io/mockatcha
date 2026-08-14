package io.instanto.mockatcha.dom;

import java.util.Objects;

/** A role query with optional accessible-name, heading-level, and ARIA-state filters. */
public final class RoleQuery {

  private final String role;
  private TextMatch name;
  private Integer level;
  private Boolean checked;
  private Boolean selected;
  private Boolean expanded;
  private Boolean pressed;

  private RoleQuery(String role) {
    this.role = Objects.requireNonNull(role, "role");
  }

  /** Starts a query for this explicit or implicit accessible role. */
  public static RoleQuery role(String role) {
    return new RoleQuery(role);
  }

  /** Requires this exact accessible name. */
  public RoleQuery named(String accessibleName) {
    return named(TextMatch.exact(accessibleName));
  }

  /** Requires an accessible name accepted by this matcher. */
  public RoleQuery named(TextMatch accessibleName) {
    this.name = Objects.requireNonNull(accessibleName, "accessibleName");
    return this;
  }

  /** Requires this heading level, from {@code h1}-{@code h6} or {@code aria-level}. */
  public RoleQuery level(int level) {
    if (level < 1) {
      throw new IllegalArgumentException("level must be positive");
    }
    this.level = level;
    return this;
  }

  /** Requires this checked state. */
  public RoleQuery checked(boolean checked) {
    this.checked = checked;
    return this;
  }

  /** Requires this selected state. */
  public RoleQuery selected(boolean selected) {
    this.selected = selected;
    return this;
  }

  /** Requires this expanded state. */
  public RoleQuery expanded(boolean expanded) {
    this.expanded = expanded;
    return this;
  }

  /** Requires this pressed state. */
  public RoleQuery pressed(boolean pressed) {
    this.pressed = pressed;
    return this;
  }

  boolean matches(org.teavm.jso.dom.html.HTMLElement element) {
    if (!role.equals(Browser.role(element))) return false;
    if (name != null && !name.matches(Browser.accessibleName(element))) return false;
    if (level != null && level.intValue() != Browser.headingLevel(element)) return false;
    if (!matches(checked, Browser.checkedState(element))) return false;
    if (!matches(selected, Browser.selectedState(element))) return false;
    if (!matches(expanded, Browser.expandedState(element))) return false;
    return matches(pressed, Browser.pressedState(element));
  }

  private static boolean matches(Boolean expected, int actual) {
    return expected == null || actual >= 0 && (actual == 1) == expected;
  }

  String description() {
    StringBuilder result = new StringBuilder("role \"").append(role).append('"');
    if (name != null) result.append(" and accessible name ").append(name.description());
    if (level != null) result.append(" at level ").append(level);
    if (checked != null) result.append(" with checked=").append(checked);
    if (selected != null) result.append(" with selected=").append(selected);
    if (expanded != null) result.append(" with expanded=").append(expanded);
    if (pressed != null) result.append(" with pressed=").append(pressed);
    return result.toString();
  }
}
