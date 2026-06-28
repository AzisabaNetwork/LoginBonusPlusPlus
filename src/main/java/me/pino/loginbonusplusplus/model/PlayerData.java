package me.pino.loginbonusplusplus.model;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class PlayerData {

    private final UUID uuid;

    private Set<Integer> claimedDays;

    private int streak;

    // ★ LocalDateに変更
    private LocalDate lastLoginDate;

    private int totalLoginDays;

    private int monthlyLoginCount;

    private int freezeTickets = 0;

    private int lastLoginMonth;

    public PlayerData(UUID uuid) {
        this.uuid = uuid;
        this.claimedDays = new HashSet<>();
        this.streak = 0;
        this.lastLoginDate = null;
        this.totalLoginDays = 0;
        this.monthlyLoginCount = 0;
        this.lastLoginMonth = LocalDate.now().getMonthValue();
    }

    public UUID getUuid() {
        return uuid;
    }

    public Set<Integer> getClaimedDays() {
        return claimedDays;
    }

    public void setClaimedDays(Set<Integer> claimedDays) {
        this.claimedDays = claimedDays;
    }

    public void addClaimedDay(int day) {
        claimedDays.add(day);
    }

    public boolean hasClaimed(int day) {
        return claimedDays.contains(day);
    }

    public void clearClaimedDays() {
        claimedDays.clear();
    }

    public int getStreak() {
        return streak;
    }

    public void setStreak(int streak) {
        this.streak = streak;
    }

    public LocalDate getLastLoginDate() {
        return lastLoginDate;
    }

    public void setLastLoginDate(LocalDate lastLoginDate) {
        this.lastLoginDate = lastLoginDate;
    }

    public int getTotalLoginDays() {
        return totalLoginDays;
    }

    public void setTotalLoginDays(int totalLoginDays) {
        this.totalLoginDays = totalLoginDays;
    }

    public int getMonthlyLoginCount() {
        return monthlyLoginCount;
    }

    public void setMonthlyLoginCount(int monthlyLoginCount) {
        this.monthlyLoginCount = monthlyLoginCount;
    }

    public void incrementMonthlyLoginCount() {
        this.monthlyLoginCount++;
    }

    public int getLastLoginMonth() {
        return lastLoginMonth;
    }

    public void setLastLoginMonth(int lastLoginMonth) {
        this.lastLoginMonth = lastLoginMonth;
    }

    public int getFreezeTickets() { return freezeTickets; }
    public void setFreezeTickets(int freezeTickets) {
        this.freezeTickets = Math.max(0, Math.min(64, freezeTickets));
    }
    public void addFreezeTicket(int amount) {
        setFreezeTickets(this.freezeTickets + amount);
    }
    public boolean consumeFreezeTicket() {
        if (freezeTickets <= 0) return false;
        freezeTickets--;
        return true;
    }
}