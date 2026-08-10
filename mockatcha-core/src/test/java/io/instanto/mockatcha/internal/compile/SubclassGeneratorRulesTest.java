package io.instanto.mockatcha.internal.compile;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import org.teavm.extension.introspect.IntrospectClass;
import org.teavm.extension.introspect.IntrospectMethod;

/**
 * Checks the rules that decide whether a class can be mocked.
 *
 * <p>This runs on the JVM against a stand-in for TeaVM's introspection, because the rules are
 * applied while TeaVM compiles a test and a deliberately unsupported metaprogram cannot live in a
 * green browser test source set. It covers the rule table and its messages; the surrounding
 * diagnostic plumbing is exercised by hand.
 */
public class SubclassGeneratorRulesTest {

  @Test
  public void acceptsAnOrdinaryClassWithANoArgumentConstructor() {
    assertNull(SubclassGenerator.rejectionReason(describe("com.example.Pricing", Modifier.PUBLIC)));
  }

  @Test
  public void rejectsAFinalClass() {
    String reason =
        SubclassGenerator.rejectionReason(
            describe("com.example.Pricing", Modifier.PUBLIC | Modifier.FINAL));

    assertEquals("com.example.Pricing is final and cannot be subclassed", reason);
  }

  @Test
  public void rejectsAClassWithoutANoArgumentConstructor() {
    IntrospectClass<?> type = describe("com.example.Pricing", Modifier.PUBLIC);
    ((Description) Proxy.getInvocationHandler(type)).constructor = null;

    String reason = SubclassGenerator.rejectionReason(type);

    assertTrue(reason, reason.startsWith("com.example.Pricing has no no-argument constructor"));
  }

  @Test
  public void rejectsAClassWhoseNoArgumentConstructorIsPrivate() {
    IntrospectClass<?> type = describe("com.example.Pricing", Modifier.PUBLIC);
    ((Description) Proxy.getInvocationHandler(type)).constructorModifiers = Modifier.PRIVATE;

    assertEquals(
        "com.example.Pricing has a private no-argument constructor",
        SubclassGenerator.rejectionReason(type));
  }

  @Test
  public void rejectsAnAnonymousClass() {
    assertEquals(
        "com.example.Pricing$1 is an anonymous or local class",
        SubclassGenerator.rejectionReason(describe("com.example.Pricing$1", Modifier.PUBLIC)));
  }

  @Test
  public void acceptsANestedClass() {
    assertNull(
        SubclassGenerator.rejectionReason(
            describe("com.example.Pricing$Rates", Modifier.PUBLIC)));
  }

  @Test
  public void rejectsKindsThatCannotBeSubclassed() {
    assertEquals(
        "com.example.Pricing is an interface",
        SubclassGenerator.rejectionReason(kind("com.example.Pricing", "isInterface")));
    assertEquals(
        "com.example.Pricing is an enum",
        SubclassGenerator.rejectionReason(kind("com.example.Pricing", "isEnum")));
    assertEquals(
        "com.example.Pricing is a record, and records are final",
        SubclassGenerator.rejectionReason(kind("com.example.Pricing", "isRecord")));
    assertEquals(
        "com.example.Pricing is an annotation type",
        SubclassGenerator.rejectionReason(kind("com.example.Pricing", "isAnnotation")));
    assertEquals(
        "com.example.Pricing is not a class",
        SubclassGenerator.rejectionReason(kind("com.example.Pricing", "isArray")));
  }

  private static IntrospectClass<?> kind(String name, String predicate) {
    IntrospectClass<?> type = describe(name, Modifier.PUBLIC);
    ((Description) Proxy.getInvocationHandler(type)).kind = predicate;
    return type;
  }

  private static IntrospectClass<?> describe(String name, int modifiers) {
    Description description = new Description(name, modifiers);
    return (IntrospectClass<?>)
        Proxy.newProxyInstance(
            SubclassGeneratorRulesTest.class.getClassLoader(),
            new Class<?>[] {IntrospectClass.class},
            description);
  }

  /** The few introspection answers the rules consult, with everything else left false or null. */
  private static final class Description implements InvocationHandler {

    private final String name;
    private final int modifiers;
    private String kind = "";
    private int constructorModifiers = Modifier.PUBLIC;
    private Object constructor = new Object();

    private Description(String name, int modifiers) {
      this.name = name;
      this.modifiers = modifiers;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] arguments) {
      Map<String, Object> answers = new HashMap<>();
      answers.put("name", name);
      answers.put("modifiers", modifiers);
      if (method.getName().equals("declaredMethod")) {
        return constructor == null ? null : constructorMethod();
      }
      if (answers.containsKey(method.getName())) {
        return answers.get(method.getName());
      }
      if (method.getReturnType() == boolean.class) {
        return method.getName().equals(kind);
      }
      return null;
    }

    private IntrospectMethod constructorMethod() {
      int constructorAccess = constructorModifiers;
      return (IntrospectMethod)
          Proxy.newProxyInstance(
              SubclassGeneratorRulesTest.class.getClassLoader(),
              new Class<?>[] {IntrospectMethod.class},
              (proxy, method, arguments) ->
                  method.getName().equals("modifiers")
                      ? constructorAccess
                      : method.getReturnType() == boolean.class ? false : null);
    }
  }
}
