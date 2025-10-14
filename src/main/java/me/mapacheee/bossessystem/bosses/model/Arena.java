package me.mapacheee.bossessystem.bosses.model;

public record Arena(
    String id,
    String world,
    double x,
    double y,
    double z,
    float yaw,
    float pitch,
    String bossId,
    Integer spawnDelaySeconds
) {}

