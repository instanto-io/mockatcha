/*
 * Copyright 2026 Carl Stainton
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.teavm.classlib.java.util.concurrent;

import org.teavm.classlib.java.io.TSerializable;
import org.teavm.classlib.java.lang.TNullPointerException;
import org.teavm.classlib.java.util.TAbstractQueue;
import org.teavm.classlib.java.util.TArrayDeque;
import org.teavm.classlib.java.util.TCollection;
import org.teavm.classlib.java.util.TIterator;
import org.teavm.classlib.java.util.TQueue;

/** Single-threaded TeaVM classlib implementation of {@code ConcurrentLinkedQueue}. */
public class TConcurrentLinkedQueue<E> extends TAbstractQueue<E> implements TQueue<E>, TSerializable {
    private final TArrayDeque<E> elements = new TArrayDeque<>();

    /** Creates an empty queue. */
    public TConcurrentLinkedQueue() {
    }

    /** Creates a queue containing the supplied elements in iteration order. */
    public TConcurrentLinkedQueue(TCollection<? extends E> c) {
        for (TIterator<? extends E> iterator = c.iterator(); iterator.hasNext();) {
            offer(iterator.next());
        }
    }

    @Override
    public boolean offer(E e) {
        if (e == null) {
            throw new TNullPointerException();
        }
        return elements.offer(e);
    }

    @Override
    public E poll() {
        return elements.poll();
    }

    @Override
    public E peek() {
        return elements.peek();
    }

    @Override
    public TIterator<E> iterator() {
        return elements.iterator();
    }

    @Override
    public int size() {
        return elements.size();
    }

    @Override
    public boolean isEmpty() {
        return elements.isEmpty();
    }
}
