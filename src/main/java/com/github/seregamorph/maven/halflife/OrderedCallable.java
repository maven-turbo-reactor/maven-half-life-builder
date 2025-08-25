package com.github.seregamorph.maven.halflife;

import java.util.concurrent.Callable;

/**
 * @author Sergey Chernov
 */
public class OrderedCallable<T> implements Callable<T>, Comparable<OrderedCallable<T>> {

    private final int order;
    private final Callable<T> delegate;

    public OrderedCallable(int order, Callable<T> delegate) {
        this.order = order;
        this.delegate = delegate;
    }

    @Override
    public int compareTo(OrderedCallable that) {
        return Integer.compare(order, that.order);
    }

    @Override
    public T call() throws Exception {
        return delegate.call();
    }
}
