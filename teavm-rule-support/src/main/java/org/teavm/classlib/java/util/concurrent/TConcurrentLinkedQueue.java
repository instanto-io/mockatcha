/*
 *  Copyright 2026 Carl Stainton.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
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
