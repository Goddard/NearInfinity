// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.util.tuples;

import java.util.Collection;
import java.util.Iterator;

/**
 * A tuple class that can store three elements.
 */
public class Triple<A, B, C> extends Tuple implements TupleValue0<A>, TupleValue1<B>, TupleValue2<C> {
  private static final int SIZE = 3;

  private A value0;
  private B value1;
  private C value2;

  /**
   * Creates a new tuple instance with the specified elements.
   *
   * @param <A>    the first tuple element type.
   * @param <B>    the second tuple element type.
   * @param <C>    the third tuple element type.
   * @param value0 The first element to store in the tuple.
   * @param value1 The second element to store in the tuple.
   * @param value2 The third element to store in the tuple.
   * @return A new tuple instance.
   */
  public static <A, B, C> Triple<A, B, C> with(A value0, B value1, C value2) {
    return new Triple<>(value0, value1, value2);
  }

  /**
   * Creates a new tuple from the array. The array must contain at least 3 elements.
   *
   * @param <T> the tuple element type.
   * @param arr The array to be used as source for the tuple.
   * @return A new tuple instance.
   */
  public static <T> Triple<T, T, T> fromArray(T[] arr) {
    if (arr == null) {
      throw new IllegalArgumentException("Array cannot be null");
    }
    if (arr.length < SIZE) {
      throw new IllegalArgumentException(String.format("Array must contain at least %d elements", SIZE));
    }
    return new Triple<>(arr[0], arr[1], arr[2]);
  }

  /**
   * Creates a new tuple from the collection. The collection must contain at least 3 elements.
   *
   * @param <T> the tuple element type.
   * @param col the collection to be used as source for the tuple.
   * @return a new tuple instance.
   */
  public static <T> Triple<T, T, T> fromCollection(Collection<T> col) {
    if (col == null) {
      throw new IllegalArgumentException("Collection cannot be null");
    }
    if (col.size() < SIZE) {
      throw new IllegalArgumentException(String.format("Collection must contain at least %d elements", SIZE));
    }
    Iterator<T> iter = col.iterator();
    T el0 = iter.next();
    T el1 = iter.next();
    T el2 = iter.next();
    return new Triple<>(el0, el1, el2);
  }

  /**
   * Creates a new tuple from the {@code Iterable} object.
   *
   * @param <T>      the tuple element type.
   * @param iterator the {@code Iterable} object to be used as source for the tuple.
   * @return a new tuple instance.
   */
  public static <T> Triple<T, T, T> fromIterable(Iterable<T> iterator) {
    return fromIterable(iterator, 0);
  }

  /**
   * Creates a new tuple from the {@code Iterable} object, starting the specified index.
   *
   * @param <T>      the tuple element type.
   * @param iterator the {@code Iterable} object to be used as source for the tuple.
   * @param index    start index in {@code Iterable} object.
   * @return A new tuple instance.
   */
  public static <T> Triple<T, T, T> fromIterable(Iterable<T> iterator, int index) {
    if (iterator == null) {
      throw new IllegalArgumentException("Iterator cannot be null");
    }

    Iterator<T> iter = iterator.iterator();
    for (int i = 0; i < index; i++) {
      if (iter.hasNext()) {
        iter.next();
      }
      i++;
    }

    T el0 = iter.hasNext() ? iter.next() : null;
    T el1 = iter.hasNext() ? iter.next() : null;
    T el2 = iter.hasNext() ? iter.next() : null;
    return new Triple<>(el0, el1, el2);
  }

  /**
   * Constructs a new Triple instance and initializes it with the specified arguments.
   *
   * @param value0 the first value of the Triple.
   * @param value1 the second value of the Triple.
   * @param value2 the third value of the Triple.
   */
  public Triple(A value0, B value1, C value2) {
    super(value0, value1, value2);
    this.value0 = value0;
    this.value1 = value1;
    this.value2 = value2;
  }

  @Override
  public int size() {
    return SIZE;
  }

  @Override
  public A getValue0() {
    return value0;
  }

  @Override
  public A setValue0(A newValue) {
    A retVal = value0;
    setValue(0, newValue);
    value0 = newValue;
    return retVal;
  }

  @Override
  public B getValue1() {
    return value1;
  }

  @Override
  public B setValue1(B newValue) {
    B retVal = value1;
    setValue(1, newValue);
    value1 = newValue;
    return retVal;
  }

  @Override
  public C getValue2() {
    return value2;
  }

  @Override
  public C setValue2(C newValue) {
    C retVal = value2;
    setValue(2, newValue);
    value2 = newValue;
    return retVal;
  }
}
