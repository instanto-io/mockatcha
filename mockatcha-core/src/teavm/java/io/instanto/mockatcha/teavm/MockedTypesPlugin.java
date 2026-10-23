/*
 * Copyright 2026 Carl Stainton
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.instanto.mockatcha.teavm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.teavm.diagnostics.Diagnostics;
import org.teavm.model.AccessLevel;
import org.teavm.model.CallLocation;
import org.teavm.model.ClassHolder;
import org.teavm.model.ClassHolderTransformer;
import org.teavm.model.ClassHolderTransformerContext;
import org.teavm.model.ClassReader;
import org.teavm.model.ClassReaderSource;
import org.teavm.model.ElementModifier;
import org.teavm.model.MethodHolder;
import org.teavm.model.MethodReader;
import org.teavm.model.MethodReference;
import org.teavm.model.ProgramReader;
import org.teavm.model.TextLocation;
import org.teavm.model.ValueType;
import org.teavm.model.VariableReader;
import org.teavm.model.instructions.AbstractInstructionReader;
import org.teavm.model.instructions.InstructionReader;
import org.teavm.model.instructions.InvocationType;
import org.teavm.vm.spi.TeaVMHost;
import org.teavm.vm.spi.TeaVMPlugin;

/**
 * Records the types a TeaVM build mocks, in {@link MockedTypes}, as TeaVM loads each class and
 * before any of its code is analysed.
 *
 * <p>A mocked type is a class literal that reaches a mock or spy. It reaches one when it is passed
 * to {@code Mockatcha.mock} or {@code Mockatcha.spy}, or to a <em>forwarder</em>: a static, private
 * or final method that passes one of its parameters on, unchanged, to a mock or spy or to another
 * forwarder. Such a call names one method, so the class can be followed back to its caller.
 *
 * <p>Any other class value reaching a mock or spy is an error, so the mocks a build generates are
 * bounded by the classes its code names.
 */
public final class MockedTypesPlugin implements TeaVMPlugin, ClassHolderTransformer {
  private static final String GENERATOR = "io.instanto.mockatcha.teavm.TeaVmMocks";
  private static final Set<String> FACTORIES = Set.of("mock", "spy");
  private static final ValueType CLASS = ValueType.object("java.lang.Class");

  /** For each method read so far, the parameters it hands on to a mock or spy. */
  private final Map<MethodReference, Set<Integer>> forwarded = new HashMap<>();

  @Override
  public void install(TeaVMHost host) {
    MockedTypes.clear();
    host.add(this);
  }

  @Override
  public void transformClass(ClassHolder cls, ClassHolderTransformerContext context) {
    ClassReaderSource classes = context.getHierarchy().getClassSource();
    Diagnostics diagnostics = context.getDiagnostics();
    for (MethodHolder method : cls.getMethods()) {
      if (method.getProgram() != null) {
        record(cls, method, classes, diagnostics);
      }
    }
  }

  private void record(
      ClassHolder cls, MethodHolder method, ClassReaderSource classes, Diagnostics diagnostics) {
    Calls calls = calls(method, new Classes(cls, classes));
    calls.named.forEach(MockedTypes::declare);
    boolean mapsBack = mapsBackToCallers(cls, method);
    forwarded.put(method.getReference(), mapsBack ? calls.forwardedParameters() : Set.of());
    for (Calls.Handed handed : calls.handedOn) {
      if (handed.parameter < 0 || !mapsBack) {
        diagnostics.error(
            new CallLocation(method.getReference(), handed.location),
            "{{m0}} creates a mock under TeaVM, so the class it is given must be a class literal,"
                + " or a parameter of a static, private or final method whose callers pass one",
            handed.target);
      }
    }
  }

  /** The parameters of a method that reach a mock or spy, read from its code if not yet known. */
  private Set<Integer> forwardedParameters(MethodReference method, Classes classes) {
    if (method.getClassName().equals(GENERATOR) && FACTORIES.contains(method.getName())) {
      return Set.of(0);
    }
    Set<Integer> known = forwarded.get(method);
    if (known != null) {
      return known;
    }
    // A method that hands a class to itself, directly or through others, forwards nothing more.
    forwarded.put(method, Set.of());
    Set<Integer> parameters = Set.of();
    if (!isPlatformClass(method.getClassName()) && takesAClass(method)) {
      ClassReader cls = classes.get(method.getClassName());
      MethodReader reader = cls == null ? null : cls.getMethod(method.getDescriptor());
      if (reader != null && reader.getProgram() != null && mapsBackToCallers(cls, reader)) {
        parameters = calls(reader, classes).forwardedParameters();
      }
    }
    forwarded.put(method, parameters);
    return parameters;
  }

  /** What one method's calls hand to mocks and spies. */
  private Calls calls(MethodReader method, Classes classes) {
    ProgramReader program = method.getProgram();
    Map<Integer, ValueType> literals = new HashMap<>();
    readAll(
        program,
        new AbstractInstructionReader() {
          @Override
          public void classConstant(VariableReader receiver, ValueType constant) {
            literals.put(receiver.getIndex(), constant);
          }
        });
    Calls calls = new Calls();
    int parameterCount = method.parameterCount();
    readAll(
        program,
        new AbstractInstructionReader() {
          private TextLocation location;

          @Override
          public void location(TextLocation location) {
            this.location = location;
          }

          @Override
          public void invoke(
              VariableReader receiver,
              VariableReader instance,
              MethodReference target,
              List<? extends VariableReader> arguments,
              InvocationType type) {
            if (!takesAClass(target)) {
              return;
            }
            for (int index : forwardedParameters(target, classes)) {
              int variable = arguments.get(index).getIndex();
              ValueType literal = literals.get(variable);
              if (literal instanceof ValueType.Object object) {
                calls.named.add(object.getClassName());
              } else {
                // Variables 1 to n hold the method's parameters; 0 is the instance.
                int parameter = variable >= 1 && variable <= parameterCount ? variable - 1 : -1;
                calls.handedOn.add(new Calls.Handed(target, parameter, location));
              }
            }
          }
        });
    return calls;
  }

  private static void readAll(ProgramReader program, InstructionReader reader) {
    for (int i = 0; i < program.basicBlockCount(); i++) {
      program.basicBlockAt(i).readAllInstructions(reader);
    }
  }

  /** Whether every call to the method names it, so its callers are the ones that reach it. */
  private static boolean mapsBackToCallers(ClassReader cls, MethodReader method) {
    return method.hasModifier(ElementModifier.STATIC)
        || method.getLevel() == AccessLevel.PRIVATE
        || method.hasModifier(ElementModifier.FINAL)
        || cls.hasModifier(ElementModifier.FINAL);
  }

  private static boolean takesAClass(MethodReference method) {
    for (int i = 0; i < method.parameterCount(); i++) {
      if (method.parameterType(i).equals(CLASS)) {
        return true;
      }
    }
    return false;
  }

  private static boolean isPlatformClass(String className) {
    return className.startsWith("java.")
        || className.startsWith("javax.")
        || className.startsWith("jdk.")
        || className.startsWith("org.teavm.");
  }

  /**
   * The classes a method's calls are read from: the one TeaVM is loading, read as it stands rather
   * than fetched again, and the rest from TeaVM's class source.
   */
  private record Classes(ClassReader loading, ClassReaderSource source) {
    ClassReader get(String name) {
      return loading.getName().equals(name) ? loading : source.get(name);
    }
  }

  /** The classes a method's calls name for mocks, and the class values they hand on otherwise. */
  private static final class Calls {
    final Set<String> named = new LinkedHashSet<>();
    final List<Handed> handedOn = new ArrayList<>();

    /** A class value, not a literal, handed to a target that reaches a mock or spy. */
    record Handed(MethodReference target, int parameter, TextLocation location) {}

    /** The method's own parameters it hands on. */
    Set<Integer> forwardedParameters() {
      Set<Integer> parameters = new LinkedHashSet<>();
      for (Handed handed : handedOn) {
        if (handed.parameter >= 0) {
          parameters.add(handed.parameter);
        }
      }
      return parameters;
    }
  }
}
