/*
 * Copyright 2026 Carl Stainton
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.instanto.mockatcha.jvm;

import io.instanto.mockatcha.internal.MockRuntime;
import io.instanto.mockatcha.internal.MockState;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 * Generates the subclass used to mock or spy on an ordinary class, at run time on the JVM.
 *
 * <p>The bytecode matches what the TeaVM generator emits while it compiles, so a class mock behaves
 * the same on either platform. The tests that mock classes run on both and hold the two together.
 */
final class JvmSubclasses {
  private static final String STATE = Type.getInternalName(MockState.class);
  private static final String RUNTIME = Type.getInternalName(MockRuntime.class);
  private static final String OBJECT = "java/lang/Object";
  private static final String STATE_FIELD = "mockatchaState";
  private static final String DELEGATE_FIELD = "mockatchaDelegate";

  /** Answered directly by the generated subclass, so never treated as overridable methods. */
  private static final Set<String> OBJECT_METHODS =
      Set.of(
          "equals(Ljava/lang/Object;)Z",
          "hashCode()I",
          "toString()Ljava/lang/String;",
          "clone()Ljava/lang/Object;",
          "finalize()V",
          "getClass()Ljava/lang/Class;",
          "notify()V",
          "notifyAll()V",
          "wait()V",
          "wait(J)V",
          "wait(JI)V");

  private static final Map<Class<?>, Constructor<?>> GENERATED = new ConcurrentHashMap<>();

  private JvmSubclasses() {}

  /** Explains why a type cannot be subclassed, or returns null when it can. */
  static String rejectionReason(Class<?> type) {
    String name = type.getName();
    if (type.isPrimitive() || type.isArray()) {
      return name + " is not a class";
    }
    if (type.isInterface()) {
      return name + " is an interface";
    }
    if (type.isAnnotation()) {
      return name + " is an annotation type";
    }
    if (type.isEnum()) {
      return name + " is an enum";
    }
    if (type.isRecord()) {
      return name + " is a record, and records are final";
    }
    if (Modifier.isFinal(type.getModifiers())) {
      return name + " is final and cannot be subclassed";
    }
    if (type.isAnonymousClass() || type.isLocalClass()) {
      return name + " is an anonymous or local class";
    }
    Constructor<?> constructor;
    try {
      constructor = type.getDeclaredConstructor();
    } catch (NoSuchMethodException absent) {
      return name
          + " has no no-argument constructor, and Mockatcha cannot allocate an object without"
          + " running one";
    }
    if (Modifier.isPrivate(constructor.getModifiers())) {
      return name + " has a private no-argument constructor";
    }
    return null;
  }

  /** Instantiates a generated subclass; a mock is an instance whose delegate is null. */
  static <T> T instantiate(Class<T> type, MockState state, T delegate) {
    Constructor<?> constructor = GENERATED.computeIfAbsent(type, JvmSubclasses::generate);
    try {
      return type.cast(constructor.newInstance(state, delegate));
    } catch (ReflectiveOperationException failure) {
      throw new IllegalStateException("Could not instantiate a mock of " + type.getName(), failure);
    }
  }

  private static Constructor<?> generate(Class<?> type) {
    String target = Type.getInternalName(type);
    String generated = target + "$Mockatcha";

    // Version 49, as the TeaVM generator uses, so branches need no stack map frames.
    ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
    writer.visit(
        Opcodes.V1_5,
        Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL | Opcodes.ACC_SUPER,
        generated,
        null,
        target,
        null);
    writer
        .visitField(
            Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL, STATE_FIELD, "L" + STATE + ";", null, null)
        .visitEnd();
    writer
        .visitField(
            Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL, DELEGATE_FIELD, "L" + target + ";", null, null)
        .visitEnd();

    writeConstructor(writer, generated, target);
    for (Method method : overridableMethods(type)) {
      writeOverride(writer, generated, target, method);
    }
    writeObjectMethods(writer, generated, type);
    writer.visitEnd();

    return define(type, generated, writer.toByteArray());
  }

  private static Constructor<?> define(Class<?> type, String generated, byte[] bytecode) {
    try {
      // Defined in the mocked class's own package and loader, so package-private methods and
      // package-private superclasses remain reachable.
      MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(type, MethodHandles.lookup());
      Class<?> subclass = lookup.defineClass(bytecode);
      Constructor<?> constructor = subclass.getDeclaredConstructor(MockState.class, type);
      constructor.setAccessible(true);
      return constructor;
    } catch (IllegalAccessException denied) {
      throw new UnsupportedOperationException(
          "Mockatcha cannot mock "
              + type.getName()
              + " because its package is not open to Mockatcha. Open it, or mock an interface.",
          denied);
    } catch (ReflectiveOperationException | LinkageError failure) {
      throw new IllegalStateException(
          "Could not define " + generated.replace('/', '.'), failure);
    }
  }

  private static void writeConstructor(ClassWriter writer, String generated, String target) {
    MethodVisitor visitor =
        writer.visitMethod(
            Opcodes.ACC_PUBLIC, "<init>", "(L" + STATE + ";L" + target + ";)V", null, null);
    visitor.visitCode();
    visitor.visitVarInsn(Opcodes.ALOAD, 0);
    visitor.visitMethodInsn(Opcodes.INVOKESPECIAL, target, "<init>", "()V", false);
    visitor.visitVarInsn(Opcodes.ALOAD, 0);
    visitor.visitVarInsn(Opcodes.ALOAD, 1);
    visitor.visitFieldInsn(Opcodes.PUTFIELD, generated, STATE_FIELD, "L" + STATE + ";");
    visitor.visitVarInsn(Opcodes.ALOAD, 0);
    visitor.visitVarInsn(Opcodes.ALOAD, 2);
    visitor.visitFieldInsn(Opcodes.PUTFIELD, generated, DELEGATE_FIELD, "L" + target + ";");
    visitor.visitInsn(Opcodes.RETURN);
    visitor.visitMaxs(0, 0);
    visitor.visitEnd();
  }

  private static void writeOverride(
      ClassWriter writer, String generated, String target, Method method) {
    Class<?>[] parameters = method.getParameterTypes();
    Class<?> returnType = method.getReturnType();
    String descriptor = Type.getMethodDescriptor(method);
    MethodVisitor visitor =
        writer.visitMethod(
            Opcodes.ACC_PUBLIC, method.getName(), descriptor, null, exceptionsOf(method));
    visitor.visitCode();

    int[] slots = new int[parameters.length];
    int nextSlot = 1;
    for (int index = 0; index < parameters.length; index++) {
      slots[index] = nextSlot;
      nextSlot += Type.getType(parameters[index]).getSize();
    }
    int argumentsSlot = nextSlot++;
    int resultSlot = nextSlot++;

    // The superclass constructor runs before this subclass's fields are assigned. If it calls an
    // overridable method, keep normal construction behaviour rather than dispatching with a null
    // MockState. An abstract hook has no implementation to call, so it answers an empty value.
    Label stateReady = new Label();
    visitor.visitVarInsn(Opcodes.ALOAD, 0);
    visitor.visitFieldInsn(Opcodes.GETFIELD, generated, STATE_FIELD, "L" + STATE + ";");
    visitor.visitJumpInsn(Opcodes.IFNONNULL, stateReady);
    if (Modifier.isAbstract(method.getModifiers())) {
      emitEmptyAnswer(visitor, returnType);
    } else {
      visitor.visitVarInsn(Opcodes.ALOAD, 0);
      loadArguments(visitor, parameters, slots);
      visitor.visitMethodInsn(Opcodes.INVOKESPECIAL, target, method.getName(), descriptor, false);
      visitor.visitInsn(returnOpcode(returnType));
    }
    visitor.visitLabel(stateReady);

    visitor.visitLdcInsn(parameters.length);
    visitor.visitTypeInsn(Opcodes.ANEWARRAY, OBJECT);
    for (int index = 0; index < parameters.length; index++) {
      visitor.visitInsn(Opcodes.DUP);
      visitor.visitLdcInsn(index);
      visitor.visitVarInsn(Type.getType(parameters[index]).getOpcode(Opcodes.ILOAD), slots[index]);
      box(visitor, parameters[index]);
      visitor.visitInsn(Opcodes.AASTORE);
    }
    visitor.visitVarInsn(Opcodes.ASTORE, argumentsSlot);

    String identity = JvmMocks.identity(method);
    visitor.visitVarInsn(Opcodes.ALOAD, 0);
    visitor.visitFieldInsn(Opcodes.GETFIELD, generated, STATE_FIELD, "L" + STATE + ";");
    visitor.visitLdcInsn(identity);
    visitor.visitVarInsn(Opcodes.ALOAD, argumentsSlot);
    visitor.visitMethodInsn(
        Opcodes.INVOKESTATIC,
        RUNTIME,
        "dispatchSpy",
        "(L" + STATE + ";Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/Object;",
        false);
    visitor.visitVarInsn(Opcodes.ASTORE, resultSlot);

    Label answerFromRuntime = new Label();
    visitor.visitVarInsn(Opcodes.ALOAD, 0);
    visitor.visitFieldInsn(Opcodes.GETFIELD, generated, STATE_FIELD, "L" + STATE + ";");
    visitor.visitLdcInsn(identity);
    visitor.visitVarInsn(Opcodes.ALOAD, argumentsSlot);
    visitor.visitVarInsn(Opcodes.ALOAD, resultSlot);
    visitor.visitVarInsn(Opcodes.ALOAD, 0);
    visitor.visitFieldInsn(Opcodes.GETFIELD, generated, DELEGATE_FIELD, "L" + target + ";");
    visitor.visitMethodInsn(
        Opcodes.INVOKESTATIC,
        RUNTIME,
        "callsReal",
        "(L"
            + STATE
            + ";Ljava/lang/String;[Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Z",
        false);
    visitor.visitJumpInsn(Opcodes.IFEQ, answerFromRuntime);

    visitor.visitVarInsn(Opcodes.ALOAD, 0);
    visitor.visitFieldInsn(Opcodes.GETFIELD, generated, DELEGATE_FIELD, "L" + target + ";");
    loadArguments(visitor, parameters, slots);
    visitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, target, method.getName(), descriptor, false);
    visitor.visitInsn(returnOpcode(returnType));

    visitor.visitLabel(answerFromRuntime);
    emitRuntimeAnswer(visitor, returnType, resultSlot);

    visitor.visitMaxs(0, 0);
    visitor.visitEnd();
  }

  private static void loadArguments(MethodVisitor visitor, Class<?>[] parameters, int[] slots) {
    for (int index = 0; index < parameters.length; index++) {
      visitor.visitVarInsn(Type.getType(parameters[index]).getOpcode(Opcodes.ILOAD), slots[index]);
    }
  }

  private static void emitRuntimeAnswer(MethodVisitor visitor, Class<?> returnType, int slot) {
    if (returnType == void.class) {
      visitor.visitInsn(Opcodes.RETURN);
      return;
    }
    visitor.visitVarInsn(Opcodes.ALOAD, slot);
    String conversion = conversionFor(returnType);
    if (conversion == null) {
      visitor.visitMethodInsn(
          Opcodes.INVOKESTATIC, RUNTIME, "toObject", "(Ljava/lang/Object;)Ljava/lang/Object;", false);
      visitor.visitTypeInsn(
          Opcodes.CHECKCAST,
          returnType.isArray() ? Type.getDescriptor(returnType) : Type.getInternalName(returnType));
    } else {
      visitor.visitMethodInsn(
          Opcodes.INVOKESTATIC,
          RUNTIME,
          conversion,
          "(Ljava/lang/Object;)" + Type.getDescriptor(returnType),
          false);
    }
    visitor.visitInsn(returnOpcode(returnType));
  }

  private static void emitEmptyAnswer(MethodVisitor visitor, Class<?> returnType) {
    if (!returnType.isPrimitive()) {
      visitor.visitInsn(Opcodes.ACONST_NULL);
      visitor.visitInsn(Opcodes.ARETURN);
      return;
    }
    if (returnType == void.class) {
      visitor.visitInsn(Opcodes.RETURN);
    } else if (returnType == long.class) {
      visitor.visitInsn(Opcodes.LCONST_0);
      visitor.visitInsn(Opcodes.LRETURN);
    } else if (returnType == float.class) {
      visitor.visitInsn(Opcodes.FCONST_0);
      visitor.visitInsn(Opcodes.FRETURN);
    } else if (returnType == double.class) {
      visitor.visitInsn(Opcodes.DCONST_0);
      visitor.visitInsn(Opcodes.DRETURN);
    } else {
      visitor.visitInsn(Opcodes.ICONST_0);
      visitor.visitInsn(Opcodes.IRETURN);
    }
  }

  private static void writeObjectMethods(ClassWriter writer, String generated, Class<?> type) {
    Set<String> unavailable = finalMethods(type);
    if (!unavailable.contains("equals(Ljava/lang/Object;)Z")) {
      MethodVisitor equals =
          writer.visitMethod(Opcodes.ACC_PUBLIC, "equals", "(Ljava/lang/Object;)Z", null, null);
      equals.visitCode();
      equals.visitVarInsn(Opcodes.ALOAD, 0);
      equals.visitVarInsn(Opcodes.ALOAD, 1);
      equals.visitMethodInsn(
          Opcodes.INVOKESTATIC,
          RUNTIME,
          "sameInstance",
          "(Ljava/lang/Object;Ljava/lang/Object;)Z",
          false);
      equals.visitInsn(Opcodes.IRETURN);
      equals.visitMaxs(0, 0);
      equals.visitEnd();
    }
    if (!unavailable.contains("hashCode()I")) {
      MethodVisitor hashCode = writer.visitMethod(Opcodes.ACC_PUBLIC, "hashCode", "()I", null, null);
      hashCode.visitCode();
      hashCode.visitVarInsn(Opcodes.ALOAD, 0);
      hashCode.visitMethodInsn(
          Opcodes.INVOKESTATIC, RUNTIME, "identityHashCode", "(Ljava/lang/Object;)I", false);
      hashCode.visitInsn(Opcodes.IRETURN);
      hashCode.visitMaxs(0, 0);
      hashCode.visitEnd();
    }
    if (!unavailable.contains("toString()Ljava/lang/String;")) {
      MethodVisitor toString =
          writer.visitMethod(Opcodes.ACC_PUBLIC, "toString", "()Ljava/lang/String;", null, null);
      toString.visitCode();
      toString.visitVarInsn(Opcodes.ALOAD, 0);
      toString.visitFieldInsn(Opcodes.GETFIELD, generated, STATE_FIELD, "L" + STATE + ";");
      toString.visitMethodInsn(
          Opcodes.INVOKESTATIC, RUNTIME, "describe", "(L" + STATE + ";)Ljava/lang/String;", false);
      toString.visitInsn(Opcodes.ARETURN);
      toString.visitMaxs(0, 0);
      toString.visitEnd();
    }
  }

  /**
   * Collects the public and protected instance methods a subclass may override.
   *
   * <p>A bridge method is skipped: the inherited bridge calls the override virtually, so one stub
   * still covers one source-level method.
   */
  static List<Method> overridableMethods(Class<?> type) {
    Map<String, Method> chosen = new LinkedHashMap<>();
    for (Class<?> current = type; current != null; current = current.getSuperclass()) {
      for (Method method : current.getDeclaredMethods()) {
        int modifiers = method.getModifiers();
        if (Modifier.isStatic(modifiers)
            || Modifier.isPrivate(modifiers)
            || Modifier.isFinal(modifiers)) {
          continue;
        }
        if (!Modifier.isPublic(modifiers) && !Modifier.isProtected(modifiers)) {
          continue;
        }
        if (OBJECT_METHODS.contains(method.getName() + Type.getMethodDescriptor(method))) {
          continue;
        }
        String signature =
            method.getName() + Type.getMethodDescriptor(method).replaceAll("\\).*", ")");
        Method previous = chosen.get(signature);
        if (previous == null || previous.isBridge()) {
          chosen.put(signature, method);
        }
      }
    }
    return new ArrayList<>(chosen.values());
  }

  private static Set<String> finalMethods(Class<?> type) {
    Set<String> unavailable = new HashSet<>();
    for (Class<?> current = type; current != null; current = current.getSuperclass()) {
      for (Method method : current.getDeclaredMethods()) {
        if (Modifier.isFinal(method.getModifiers())) {
          unavailable.add(method.getName() + Type.getMethodDescriptor(method));
        }
      }
    }
    return unavailable;
  }

  private static String[] exceptionsOf(Method method) {
    Class<?>[] thrown = method.getExceptionTypes();
    if (thrown.length == 0) {
      return null;
    }
    String[] names = new String[thrown.length];
    for (int index = 0; index < thrown.length; index++) {
      names[index] = Type.getInternalName(thrown[index]);
    }
    return names;
  }

  private static void box(MethodVisitor visitor, Class<?> type) {
    if (!type.isPrimitive()) {
      return;
    }
    Class<?> boxed = boxedType(type);
    visitor.visitMethodInsn(
        Opcodes.INVOKESTATIC,
        Type.getInternalName(boxed),
        "valueOf",
        "(" + Type.getDescriptor(type) + ")" + Type.getDescriptor(boxed),
        false);
  }

  private static Class<?> boxedType(Class<?> type) {
    if (type == boolean.class) {
      return Boolean.class;
    }
    if (type == byte.class) {
      return Byte.class;
    }
    if (type == short.class) {
      return Short.class;
    }
    if (type == char.class) {
      return Character.class;
    }
    if (type == int.class) {
      return Integer.class;
    }
    if (type == long.class) {
      return Long.class;
    }
    if (type == float.class) {
      return Float.class;
    }
    return Double.class;
  }

  private static String conversionFor(Class<?> type) {
    if (!type.isPrimitive()) {
      return null;
    }
    if (type == boolean.class) {
      return "toBoolean";
    }
    if (type == byte.class) {
      return "toByte";
    }
    if (type == short.class) {
      return "toShort";
    }
    if (type == char.class) {
      return "toChar";
    }
    if (type == int.class) {
      return "toInt";
    }
    if (type == long.class) {
      return "toLong";
    }
    if (type == float.class) {
      return "toFloat";
    }
    return "toDouble";
  }

  private static int returnOpcode(Class<?> type) {
    return type == void.class ? Opcodes.RETURN : Type.getType(type).getOpcode(Opcodes.IRETURN);
  }
}
