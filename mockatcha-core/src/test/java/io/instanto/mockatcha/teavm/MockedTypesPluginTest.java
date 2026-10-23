/*
 * Copyright 2026 Carl Stainton
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.instanto.mockatcha.teavm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.teavm.diagnostics.Diagnostics;
import org.teavm.model.AccessLevel;
import org.teavm.model.BasicBlock;
import org.teavm.model.ClassHierarchy;
import org.teavm.model.ClassHolder;
import org.teavm.model.ClassHolderTransformerContext;
import org.teavm.model.ElementModifier;
import org.teavm.model.MethodHolder;
import org.teavm.model.MethodReference;
import org.teavm.model.MutableClassHolderSource;
import org.teavm.model.Program;
import org.teavm.model.ValueType;
import org.teavm.model.Variable;
import org.teavm.model.instructions.ClassConstantInstruction;
import org.teavm.model.instructions.InvocationType;
import org.teavm.model.instructions.InvokeInstruction;

/**
 * Checks that a TeaVM build mocks only the types its code names.
 *
 * <p>This runs on the JVM against classes built in TeaVM's own model, as TeaVM hands them to the
 * plugin while it loads them.
 */
public class MockedTypesPluginTest {
  private static final String MOCKATCHA = "io.instanto.mockatcha.Mockatcha";
  private static final String GENERATOR = "io.instanto.mockatcha.teavm.TeaVmMocks";
  private static final String TEST = "com.example.PricingTest";
  private static final String HELPERS = "com.example.Mocks";
  private static final ValueType CLASS = ValueType.object("java.lang.Class");
  private static final ValueType OBJECT = ValueType.object("java.lang.Object");

  private final MutableClassHolderSource classes = new MutableClassHolderSource();
  private final MockedTypesPlugin plugin = new MockedTypesPlugin();
  private final List<String> errors = new ArrayList<>();

  @Before
  public void loadMockatcha() {
    MockedTypes.clear();
    ClassHolder mockatcha = new ClassHolder(MOCKATCHA);
    for (String factory : List.of("mock", "spy")) {
      Code code = new Code(1);
      code.call(GENERATOR, factory, code.parameter(0));
      mockatcha.addMethod(code.method(factory, ElementModifier.STATIC));
    }
    classes.putClassHolder(mockatcha);
  }

  @Test
  public void recordsTheClassLiteralPassedToMock() {
    Code code = new Code(0);
    code.call(MOCKATCHA, "mock", code.literal("com.example.Pricing"));

    load(TEST, code.method("create"));

    assertTrue(MockedTypes.isDeclared("com.example.Pricing"));
    assertTrue(errors.toString(), errors.isEmpty());
  }

  @Test
  public void recordsTheClassLiteralPassedToSpy() {
    Code code = new Code(0);
    code.call(MOCKATCHA, "spy", code.literal("com.example.Ledger"));

    load(TEST, code.method("create"));

    assertTrue(MockedTypes.isDeclared("com.example.Ledger"));
  }

  @Test
  public void leavesUnnamedTypesUnmocked() {
    Code code = new Code(0);
    code.call(MOCKATCHA, "mock", code.literal("com.example.Pricing"));

    load(TEST, code.method("create"));

    assertFalse(MockedTypes.isDeclared("java.lang.Exception"));
  }

  @Test
  public void refusesAClassThatIsNeitherALiteralNorAParameter() {
    Code code = new Code(0);
    code.call(MOCKATCHA, "mock", code.unknown());

    load(TEST, code.method("create"));

    assertEquals(errors.toString(), 1, errors.size());
    assertTrue(errors.get(0), errors.get(0).contains("must be a class literal"));
  }

  @Test
  public void acceptsMockatchaHandingItsCallersClassOn() {
    load(classes.get(MOCKATCHA));

    assertTrue(errors.toString(), errors.isEmpty());
  }

  @Test
  public void followsAStaticHelperBackToTheLiteralItsCallerPasses() {
    classes.putClassHolder(helpers(ElementModifier.STATIC));
    Code code = new Code(0);
    code.call(HELPERS, "mockOf", code.literal("com.example.Pricing"));

    load(TEST, code.method("create"));

    assertTrue(MockedTypes.isDeclared("com.example.Pricing"));
    assertTrue(errors.toString(), errors.isEmpty());
  }

  @Test
  public void followsAHelperThroughAnotherHelper() {
    ClassHolder helpers = helpers(ElementModifier.STATIC);
    Code named = new Code(1);
    named.call(HELPERS, "mockOf", named.parameter(0));
    helpers.addMethod(named.method("namedMock", ElementModifier.STATIC));
    classes.putClassHolder(helpers);
    Code code = new Code(0);
    code.call(HELPERS, "namedMock", code.literal("com.example.Ledger"));

    load(TEST, code.method("create"));

    assertTrue(MockedTypes.isDeclared("com.example.Ledger"));
  }

  @Test
  public void followsAHelperDeclaredInTheClassBeingLoaded() {
    Code helper = new Code(1);
    helper.call(MOCKATCHA, "mock", helper.parameter(0));
    Code code = new Code(0);
    code.call(TEST, "mockOf", code.literal("com.example.Pricing"));

    load(TEST, code.method("create"), helper.method("mockOf", ElementModifier.STATIC));

    assertTrue(MockedTypes.isDeclared("com.example.Pricing"));
    assertTrue(errors.toString(), errors.isEmpty());
  }

  @Test
  public void refusesAHelperItsCallersCannotBeFollowedTo() {
    ClassHolder helpers = helpers();
    classes.putClassHolder(helpers);
    Code code = new Code(0);
    code.call(HELPERS, "mockOf", code.literal("com.example.Pricing"));

    load(TEST, code.method("create"));
    load(helpers);

    assertFalse(MockedTypes.isDeclared("com.example.Pricing"));
    assertEquals(errors.toString(), 1, errors.size());
  }

  /** A class of helpers with {@code mockOf(Class)}, which hands its class to a mock. */
  private static ClassHolder helpers(ElementModifier... modifiers) {
    Code code = new Code(1);
    code.call(MOCKATCHA, "mock", code.parameter(0));
    ClassHolder helpers = new ClassHolder(HELPERS);
    helpers.addMethod(code.method("mockOf", modifiers));
    return helpers;
  }

  private void load(String className, MethodHolder... methods) {
    ClassHolder cls = new ClassHolder(className);
    for (MethodHolder method : methods) {
      cls.addMethod(method);
    }
    load(cls);
  }

  /** Hands a class to the plugin as TeaVM does while loading it. */
  private void load(ClassHolder cls) {
    Diagnostics diagnostics =
        (Diagnostics)
            Proxy.newProxyInstance(
                Diagnostics.class.getClassLoader(),
                new Class<?>[] {Diagnostics.class},
                (proxy, method, args) -> {
                  if (method.getName().equals("error")) {
                    errors.add((String) args[1]);
                  }
                  return null;
                });
    ClassHierarchy hierarchy = new ClassHierarchy(classes);
    ClassHolderTransformerContext context =
        (ClassHolderTransformerContext)
            Proxy.newProxyInstance(
                ClassHolderTransformerContext.class.getClassLoader(),
                new Class<?>[] {ClassHolderTransformerContext.class},
                (proxy, method, args) ->
                    switch (method.getName()) {
                      case "getDiagnostics" -> diagnostics;
                      case "getHierarchy" -> hierarchy;
                      default -> null;
                    });
    plugin.transformClass(cls, context);
  }

  /** One method's code: class arguments, then calls passing them on. */
  private static final class Code {
    private final Program program = new Program();
    private final BasicBlock block;
    private final int parameters;

    /** Code taking the given number of {@code Class} parameters. */
    Code(int parameters) {
      this.parameters = parameters;
      for (int i = 0; i <= parameters; i++) {
        program.createVariable();
      }
      block = program.createBasicBlock();
    }

    Variable parameter(int index) {
      return program.variableAt(index + 1);
    }

    Variable literal(String className) {
      ClassConstantInstruction literal = new ClassConstantInstruction();
      literal.setConstant(ValueType.object(className));
      literal.setReceiver(program.createVariable());
      block.add(literal);
      return literal.getReceiver();
    }

    /** A class value from somewhere the plugin cannot follow, such as a field. */
    Variable unknown() {
      return program.createVariable();
    }

    void call(String owner, String name, Variable type) {
      InvokeInstruction invoke = new InvokeInstruction();
      invoke.setType(InvocationType.SPECIAL);
      invoke.setMethod(new MethodReference(owner, name, CLASS, OBJECT));
      invoke.setArguments(type);
      block.add(invoke);
    }

    MethodHolder method(String name, ElementModifier... modifiers) {
      ValueType[] signature = new ValueType[parameters + 1];
      for (int i = 0; i < parameters; i++) {
        signature[i] = CLASS;
      }
      signature[parameters] = OBJECT;
      MethodHolder method = new MethodHolder(name, signature);
      method.setLevel(AccessLevel.PUBLIC);
      for (ElementModifier modifier : modifiers) {
        method.getModifiers().add(modifier);
      }
      method.setProgram(program);
      return method;
    }
  }
}
