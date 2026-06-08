package com.milktea.statemachine;

import com.milktea.enums.OrderStatus;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public class OrderStateMachine {

    private static final Map<OrderStatus, Set<OrderStatus>> TRANSITIONS = new EnumMap<>(OrderStatus.class);

    static {
        TRANSITIONS.put(OrderStatus.PENDING_PAYMENT, EnumSet.of(OrderStatus.PAID, OrderStatus.CANCELLED));
        TRANSITIONS.put(OrderStatus.PAID, EnumSet.of(OrderStatus.PREPARING, OrderStatus.CANCELLED));
        TRANSITIONS.put(OrderStatus.PREPARING, EnumSet.of(OrderStatus.DELIVERING, OrderStatus.CANCELLED));
        TRANSITIONS.put(OrderStatus.DELIVERING, EnumSet.of(OrderStatus.COMPLETED, OrderStatus.CANCELLED));
        TRANSITIONS.put(OrderStatus.COMPLETED, EnumSet.of(OrderStatus.REVIEWED));
        TRANSITIONS.put(OrderStatus.CANCELLED, EnumSet.noneOf(OrderStatus.class));
        TRANSITIONS.put(OrderStatus.REVIEWED, EnumSet.noneOf(OrderStatus.class));
    }

    public static boolean canTransit(OrderStatus from, OrderStatus to) {
        if (from == null || to == null) {
            return false;
        }
        Set<OrderStatus> allowed = TRANSITIONS.get(from);
        return allowed != null && allowed.contains(to);
    }

    public static void validateTransition(OrderStatus from, OrderStatus to) {
        if (!canTransit(from, to)) {
            throw new IllegalArgumentException(
                    "Invalid status transition from " + from.getDescription() + " to " + to.getDescription());
        }
    }

    private static final Set<OrderStatus> CANCELLABLE_STATES = EnumSet.of(
            OrderStatus.PENDING_PAYMENT,
            OrderStatus.PAID,
            OrderStatus.PREPARING,
            OrderStatus.DELIVERING
    );

    public static boolean isUserAllowedTransition(OrderStatus from, OrderStatus to) {
        if (!canTransit(from, to)) {
            return false;
        }
        if (to == OrderStatus.PAID && from == OrderStatus.PENDING_PAYMENT) {
            return true;
        }
        if (to == OrderStatus.CANCELLED && CANCELLABLE_STATES.contains(from)) {
            return true;
        }
        if (to == OrderStatus.COMPLETED && from == OrderStatus.DELIVERING) {
            return true;
        }
        return false;
    }

    public static OrderStatus getNextStatus(OrderStatus current) {
        switch (current) {
            case PENDING_PAYMENT:
                return OrderStatus.PAID;
            case PAID:
                return OrderStatus.PREPARING;
            case PREPARING:
                return OrderStatus.DELIVERING;
            case DELIVERING:
                return OrderStatus.COMPLETED;
            case COMPLETED:
                return OrderStatus.REVIEWED;
            default:
                return null;
        }
    }
}
