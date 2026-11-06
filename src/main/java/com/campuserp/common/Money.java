package com.campuserp.common;

import java.math.BigDecimal;

public final class Money {

    private Money() {}

    public static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    /** Percent of allocated that is used, 0 when nothing is allocated. */
    public static double percent(BigDecimal used, BigDecimal allocated) {
        if (allocated == null || allocated.signum() == 0) {
            return used != null && used.signum() > 0 ? 100.0 : 0.0;
        }
        return used.multiply(BigDecimal.valueOf(100)).divide(allocated, 1, java.math.RoundingMode.HALF_UP).doubleValue();
    }
}
