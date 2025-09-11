package com.github.seregamorph.maven.halflife;

import com.github.seregamorph.maven.halflife.graph.ProjectPart;
import org.jspecify.annotations.Nullable;

/**
 * @author Sergey Chernov
 */
public final class OrderKey implements Comparable<OrderKey> {

    private final ProjectPart projectPart;
    @Nullable
    private final Integer order;

    public OrderKey(ProjectPart projectPart, @Nullable Integer order) {
        this.projectPart = projectPart;
        this.order = order;
    }

    @Override
    public int compareTo(OrderKey that) {
        int compare = compareOrder(order, that.order);
        if (compare == 0) {
            compare = compareProjectPart(projectPart, that.projectPart);
        }
        return compare;
    }

    private static int compareProjectPart(ProjectPart p1, ProjectPart p2) {
        if (p1 == p2) {
            return 0;
        }
        return p1 == ProjectPart.MAIN ? -1 : 1;
    }

    private static int compareOrder(@Nullable Integer o1, @Nullable Integer o2) {
        // nulls last
        if (o1 == null) {
            return o2 == null ? 0 : 1;
        } else {
            return o2 == null ? -1 : o1.compareTo(o2);
        }
    }

    @Override
    public String toString() {
        return "OrderKey{" +
            "projectPart=" + projectPart +
            ", order=" + order +
            '}';
    }
}
