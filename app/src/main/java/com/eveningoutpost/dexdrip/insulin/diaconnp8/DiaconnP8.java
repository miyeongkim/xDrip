package com.eveningoutpost.dexdrip.insulin.diaconnp8;

// Diaconn P8 driver control and utility class

import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;

public class DiaconnP8 {

    static final String DIACONN_LOG_LAST_NUM = "DiaconnP8-log-lastnum-";
    static final String DIACONN_LOG_INCARNATION = "DiaconnP8-log-incarnation-";
    static final String DIACONN_LOG_WRAPPING_COUNT = "DiaconnP8-log-wrapping-count-";

    static boolean isDiaconnP8Name(String name) {
        return name != null && (name.startsWith("P8-") && name.endsWith(Pref.getString("diaconnp8_searial_5digits", "00000")));
    }
    public static boolean enabled() {
        return Pref.getBooleanDefaultFalse("use_diaconnp8");
    }
    public static void immortality() {
        if (enabled()) {
            if (JoH.ratelimit("diaconnp8-auto-start-service", 30)) {
                JoH.startService(DiaconnP8Service.class);
            }
        }
    }
}



