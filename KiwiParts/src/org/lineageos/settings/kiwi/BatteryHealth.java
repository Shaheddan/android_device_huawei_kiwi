package org.lineageos.settings.kiwi;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.BatteryManager;

/**
 * Battery health estimate for kiwi.
 *
 * The PM8916 fuel gauge is voltage-mode with no coulomb counter, so this phone cannot
 * measure its battery's real full capacity the way a Pixel does. What it can give is a real
 * charge-cycle count: the vm-bms adds up every rise in charge and counts a cycle per 100 %
 * (kernel "qcom,batt-aging-comp"), keeps it in the PMIC, and the bq24296m driver passes it to
 * the health HAL, so it arrives here as BatteryManager.EXTRA_CYCLE_COUNT.
 *
 * Capacity = 100 % - calendar wear - cycle wear:
 *  - calendar wear grows with the square root of the battery's age (the passivation layer
 *    on the anode grows that way), from the month the user says the battery was new;
 *  - cycle wear is close to linear in full cycles: the cycles the phone counted, plus, for
 *    the time before it counted, cycles estimated from how often the user says it was
 *    charged.
 * The constants are fitted to typical phone LiCoO2 cells like this one (ATL HB396481EBC):
 * about 90 % after a year of daily use and about 84 % after 500 cycles over 20 months.
 * It is an estimate and the page says so.
 */
final class BatteryHealth {

    /** HB396481EBC (ATL): charge_full_design = 3000000 uAh on the 2026-10-23 probe. */
    static final int DESIGN_MAH = 3000;
    /** Pixel's rule: Normal at or above 80 % of the design capacity, Reduced below. */
    static final int NORMAL_MIN_PCT = 80;
    /** A battery estimated below this still works; the model is not trusted any lower. */
    static final int LOWEST_PCT = 40;

    static final double CALENDAR_PCT_PER_SQRT_YEAR = 3.0;
    static final double CYCLE_PCT = 0.025;

    static final int USAGE_UNSET = -1;
    static final int USAGE_DAILY = 0;
    static final int USAGE_OFTEN = 1;
    static final int USAGE_RARELY = 2;
    /** Full cycles a day for each answer (a daily phone uses about three quarters a day). */
    private static final double[] CYCLES_PER_DAY = { 0.75, 0.35, 0.1 };

    static final int STATUS_UNKNOWN = 0;
    static final int STATUS_NORMAL = 1;
    static final int STATUS_REDUCED = 2;

    static final double DAY_MS = 24d * 60 * 60 * 1000;
    static final double YEAR_MS = 365.25 * DAY_MS;

    // Stored in the same device-protected preferences file as the bypass settings.
    private static final String KEY_KERNEL_LAST = "health_kernel_last";
    private static final String KEY_KERNEL_OFFSET = "health_kernel_offset";
    private static final String KEY_COUNT_START = "health_count_start";
    private static final String KEY_COUNT_START_TOTAL = "health_count_start_total";
    private static final String KEY_PRE_COUNTED = "health_pre_counted";
    private static final String KEY_NEW_SINCE = "health_new_since";
    private static final String KEY_USAGE = "health_usage";

    private BatteryHealth() {}

    /** What the page shows. -1 means "not known". */
    static final class Estimate {
        boolean setUp;
        int status = STATUS_UNKNOWN;
        int capacityPct = -1;
        int capacityMah = -1;
        /** Cycles the phone counted itself since {@link #countStart}. */
        int countedCycles = -1;
        /** Cycles before the phone counted: estimated, or counted before KiwiParts saw them. */
        int earlierCycles = -1;
        int totalCycles = -1;
        long countStart;
        long newSince;
        int usage = USAGE_UNSET;
        double ageYears = -1;
    }

    static SharedPreferences prefs(Context c) {
        return c.createDeviceProtectedStorageContext().getSharedPreferences(
                c.getPackageName() + "_preferences", Context.MODE_PRIVATE);
    }

    static Intent stickyBattery(Context c) {
        // A null receiver only reads the last battery broadcast, which is allowed from any
        // context, including a manifest receiver.
        return c.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
    }

    /**
     * Folds the kernel's count into the stored total and returns it, or -1 when no count
     * was ever seen. The vm-bms clears its count only at boot (when the PMIC lost its stored
     * data); a count lower than the last one seen means that happened, and the old count is
     * kept as an offset. BootReceiver calls this on every boot so no reset is missed.
     */
    static int update(Context c, Intent battery) {
        final SharedPreferences p = prefs(c);
        final int last = p.getInt(KEY_KERNEL_LAST, -1);
        int offset = p.getInt(KEY_KERNEL_OFFSET, 0);
        final int kernel = battery == null ? -1
                : battery.getIntExtra(BatteryManager.EXTRA_CYCLE_COUNT, -1);
        if (kernel < 0) {
            return last < 0 ? -1 : offset + last;
        }

        final SharedPreferences.Editor e = p.edit();
        if (last >= 0 && kernel < last) {
            offset += last;
            e.putInt(KEY_KERNEL_OFFSET, offset);
        }
        final int total = offset + kernel;
        if (!p.contains(KEY_COUNT_START)) {
            // First count seen. Cycles already on it happened before now and belong to
            // this battery (the PMIC kept counting through a reflash).
            e.putLong(KEY_COUNT_START, System.currentTimeMillis());
            e.putInt(KEY_COUNT_START_TOTAL, total);
            e.putInt(KEY_PRE_COUNTED, total);
        }
        e.putInt(KEY_KERNEL_LAST, kernel);
        e.apply();
        return total;
    }

    static Estimate estimate(Context c, Intent battery) {
        final int total = update(c, battery);
        final SharedPreferences p = prefs(c);
        final long now = System.currentTimeMillis();

        final Estimate e = new Estimate();
        e.newSince = p.getLong(KEY_NEW_SINCE, 0);
        e.usage = p.getInt(KEY_USAGE, USAGE_UNSET);
        e.countStart = p.getLong(KEY_COUNT_START, 0);
        if (total >= 0 && e.countStart > 0) {
            e.countedCycles = Math.max(0, total - p.getInt(KEY_COUNT_START_TOTAL, total));
        }
        e.setUp = e.newSince > 0 && e.usage >= USAGE_DAILY && e.usage <= USAGE_RARELY;
        if (!e.setUp) return e;

        // Before the phone counted (or all of the time, if it never could), cycles come
        // from the user's answer. Cycles the kernel had already counted before KiwiParts
        // first looked are real, so they set a floor.
        final long countFrom = e.countStart > 0 ? Math.max(e.countStart, e.newSince) : now;
        final double daysBefore = Math.max(0, countFrom - e.newSince) / DAY_MS;
        final int guessed = (int) Math.round(daysBefore * CYCLES_PER_DAY[e.usage]);
        final int preCounted = e.countStart > 0 ? p.getInt(KEY_PRE_COUNTED, 0) : 0;
        e.earlierCycles = Math.max(guessed, preCounted);
        e.totalCycles = e.earlierCycles + Math.max(0, e.countedCycles);
        e.ageYears = Math.max(0, now - e.newSince) / YEAR_MS;

        final double loss = CALENDAR_PCT_PER_SQRT_YEAR * Math.sqrt(e.ageYears)
                + CYCLE_PCT * e.totalCycles;
        e.capacityPct = (int) Math.round(Math.max(LOWEST_PCT, Math.min(100, 100 - loss)));
        // To the nearest 10 mAh: the estimate is not more precise than that.
        e.capacityMah = Math.round(DESIGN_MAH * e.capacityPct / 1000f) * 10;
        e.status = e.capacityPct >= NORMAL_MIN_PCT ? STATUS_NORMAL : STATUS_REDUCED;
        return e;
    }

    /**
     * Saves the battery history from the set-up dialog. If the battery is newer than the
     * counting (it was replaced), counting starts again from now so the old battery's cycles
     * are not charged to the new one.
     */
    static void saveHistory(Context c, long newSince, int usage) {
        final SharedPreferences p = prefs(c);
        final SharedPreferences.Editor e = p.edit()
                .putLong(KEY_NEW_SINCE, newSince)
                .putInt(KEY_USAGE, usage);
        final long start = p.getLong(KEY_COUNT_START, 0);
        if (start > 0 && newSince > start) {
            final int last = p.getInt(KEY_KERNEL_LAST, -1);
            e.putLong(KEY_COUNT_START, System.currentTimeMillis());
            e.putInt(KEY_COUNT_START_TOTAL,
                    p.getInt(KEY_KERNEL_OFFSET, 0) + Math.max(0, last));
            e.putInt(KEY_PRE_COUNTED, 0);
        }
        e.apply();
        HealthSummaryProvider.notifyChanged(c);
    }

    static int statusLabel(int status) {
        switch (status) {
            case STATUS_NORMAL:
                return R.string.health_status_normal;
            case STATUS_REDUCED:
                return R.string.health_status_reduced;
            default:
                return R.string.health_status_unknown;
        }
    }
}
