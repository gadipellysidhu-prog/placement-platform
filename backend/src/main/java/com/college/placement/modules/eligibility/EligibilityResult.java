package com.college.placement.modules.eligibility;

import java.util.List;

public record EligibilityResult(boolean eligible, List<String> reasons) {

    public static EligibilityResult pass() {
        return new EligibilityResult(true, List.of());
    }

    public static EligibilityResult fail(List<String> reasons) {
        return new EligibilityResult(false, reasons);
    }
}
