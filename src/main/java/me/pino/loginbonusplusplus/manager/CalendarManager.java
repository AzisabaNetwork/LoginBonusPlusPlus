package me.pino.loginbonusplusplus.manager;

import me.pino.loginbonusplusplus.model.PlayerData;

import java.time.LocalDate;

public class CalendarManager {

    public void checkMonthlyReset(PlayerData data) {

        LocalDate today = LocalDate.now();

        if (data.getLastLoginMonth() != today.getMonthValue()) {

            data.setMonthlyLoginCount(0);
            data.clearClaimedDays();
            data.setLastLoginMonth(today.getMonthValue());
        }
    }
}