package com.example.orders.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class OrderStatusTest {

    // Verifies all 9 possible combinations of from and to statuses against the transition business rules
    @ParameterizedTest(name = "canTransitionTo: {0} -> {1} should be {2}")
    @CsvSource({
            "PLACED, PLACED, false",
            "PLACED, SHIPPED, true",
            "PLACED, DELIVERED, false",
            "SHIPPED, PLACED, false",
            "SHIPPED, SHIPPED, false",
            "SHIPPED, DELIVERED, true",
            "DELIVERED, PLACED, false",
            "DELIVERED, SHIPPED, false",
            "DELIVERED, DELIVERED, false"
    })
    @DisplayName("Verify lifecycle transitions across all state pairs")
    void canTransitionTo_allPairs_returnsExpectedBoolean(OrderStatus from, OrderStatus to, boolean expected) {
        // Evaluate state transition rule
        boolean actual = from.canTransitionTo(to);

        // Assert the result matches expected transition validity
        assertEquals(expected, actual);
    }

    // Explicitly verifies null input returns false across statuses without NullPointerException
    @Test
    @DisplayName("canTransitionTo with null target status returns false")
    void canTransitionTo_nullTarget_returnsFalse() {
        // Assert null transition is rejected for every status
        for (OrderStatus status : OrderStatus.values()) {
            assertFalse(status.canTransitionTo(null));
        }
    }
}
