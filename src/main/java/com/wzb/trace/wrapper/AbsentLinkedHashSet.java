package com.wzb.trace.wrapper;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

public class AbsentLinkedHashSet<E> extends LinkedHashSet<E> implements Set<E>, Cloneable, java.io.Serializable {

    private static final long serialVersionUID = -2851667679971038690L;

    /**
     * Constructs a new, empty linked hash set with the specified initial
     * capacity and load factor.
     *
     * @param initialCapacity the initial capacity of the linked hash set
     * @param loadFactor      the load factor of the linked hash set
     * @throws IllegalArgumentException if the initial capacity is less
     *                                  than zero, or if the load factor is nonpositive
     */
    public AbsentLinkedHashSet(int initialCapacity, float loadFactor) {
        super(initialCapacity, loadFactor);
    }

    /**
     * Constructs a new, empty linked hash set with the specified initial
     * capacity and the default load factor (0.75).
     *
     * @param initialCapacity the initial capacity of the LinkedHashSet
     * @throws IllegalArgumentException if the initial capacity is less
     *                                  than zero
     */
    public AbsentLinkedHashSet(int initialCapacity) {
        super(initialCapacity, .75f);
    }

    /**
     * Constructs a new, empty linked hash set with the default initial
     * capacity (16) and load factor (0.75).
     */
    public AbsentLinkedHashSet() {
        super(16, .75f);
    }

    /**
     * Constructs a new linked hash set with the same elements as the
     * specified collection.  The linked hash set is created with an initial
     * capacity sufficient to hold the elements in the specified collection
     * and the default load factor (0.75).
     *
     * @param c the collection whose elements are to be placed into
     *          this set
     * @throws NullPointerException if the specified collection is null
     */
    public AbsentLinkedHashSet(Collection<? extends E> c) {
        super(Math.max(2 * c.size(), 11), .75f);
        addAll(c);
    }

    @Override
    public boolean add(E e) {
        if (!contains(e)) {
            return super.add(e);
        }
        return false;
    }
}
