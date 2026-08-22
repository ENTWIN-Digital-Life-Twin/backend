package com.digitallifetwin.wellness.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.digitallifetwin.wellness.enums.BeverageType;
import org.junit.jupiter.api.Test;

class HydrationContributionTest {

    @Test
    void contributeMl_appliesFactors() {
        assertThat(HydrationContribution.contributeMl(BeverageType.WATER, 250)).isEqualTo(250);
        assertThat(HydrationContribution.contributeMl(BeverageType.TEA, 250)).isEqualTo(200);
        assertThat(HydrationContribution.contributeMl(BeverageType.COFFEE, 200)).isEqualTo(100);
        assertThat(HydrationContribution.contributeMl(BeverageType.JUICE, 100)).isEqualTo(90);
        assertThat(HydrationContribution.contributeMl(BeverageType.OTHER, 100)).isEqualTo(50);
    }
}
