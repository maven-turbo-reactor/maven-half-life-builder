package com.github.seregamorph.maven.halflife;

import java.util.concurrent.Callable;

/**
 * @author Sergey Chernov
 */
public class OrderedCallable<T> implements Callable<T>, Comparable<OrderedCallable<T>> {

    private final int primaryOrder;
    private final int secondaryOrder;
    private final Callable<T> delegate;

    public OrderedCallable(int primaryOrder, int secondaryOrder, Callable<T> delegate) {
        this.primaryOrder = primaryOrder;
        this.secondaryOrder = secondaryOrder;
        this.delegate = delegate;
    }

    @Override
    public int compareTo(OrderedCallable that) {
        int result = Integer.compare(primaryOrder, that.primaryOrder);
        if (result == 0) {
            result = Integer.compare(secondaryOrder, that.secondaryOrder);
        }
        return result;
    }

    @Override
    public T call() throws Exception {
        return delegate.call();
    }
}
