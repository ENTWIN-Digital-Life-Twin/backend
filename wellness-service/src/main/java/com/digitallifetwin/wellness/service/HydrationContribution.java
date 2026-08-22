package com.digitallifetwin.wellness.service;

import com.digitallifetwin.wellness.enums.BeverageType;

/**
 * Hydration contribution factors for daily totalMl:
 * WATER 1.0, TEA 0.8, COFFEE 0.5, JUICE 0.9, OTHER 0.5.
 */
public final class HydrationContribution {

    private HydrationContribution() {
    }

    public static int contributeMl(BeverageType type, int quantityMl) {
        double factor = switch (type) {
            case WATER -> 1.0;
            case TEA -> 0.8;
            case COFFEE -> 0.5;
            case JUICE -> 0.9;
            case OTHER -> 0.5;
        };
        return (int) Math.round(quantityMl * factor);
    }
}
