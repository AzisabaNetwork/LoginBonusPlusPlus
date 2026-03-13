package me.pino.loginbonusplusplus.manager;

import me.pino.loginbonusplusplus.model.PlayerData;

import java.time.LocalDate;

public class StreakManager {

    public void handleLogin(PlayerData data) {

        LocalDate today = LocalDate.now();
        LocalDate lastLogin = data.getLastLoginDate();

        if (lastLogin == null) {
            data.setStreak(1);
        } else {

            if (lastLogin.plusDays(1).isEqual(today)) {
                // 連続ログイン
                data.setStreak(data.getStreak() + 1);

            } else if (lastLogin.isEqual(today)) {
                // 同日ログイン → 何もしない
                return;

            } else {
                // 途切れ
                data.setStreak(1);
            }
        }

        data.setLastLoginDate(today);
    }
}