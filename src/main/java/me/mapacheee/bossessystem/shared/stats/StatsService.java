package me.mapacheee.bossessystem.shared.stats;

import com.google.inject.Inject;
import com.thewinterframework.configurate.Container;
import com.thewinterframework.service.annotation.Service;

@Service
public final class StatsService {

  private final Container<Stats> stats;

  @Inject
  public StatsService(final Container<Stats> stats) {
    this.stats = stats;
  }

  public void recordVictory(final String bossId, final long durationMillis, final String bestBy) {
    final var root = this.stats.get();
    final var map = root.bosses();
    final var prev = map.get(bossId);
    if (prev == null) {
      map.put(bossId, new Stats.BossStats(1, durationMillis, bestBy));
    } else {
      final int defeats = prev.defeats() + 1;
      final long best = prev.bestTimeMillis() <= 0 || durationMillis < prev.bestTimeMillis() ? durationMillis : prev.bestTimeMillis();
      final String bestPlayer = (prev.bestTimeMillis() <= 0 || durationMillis < prev.bestTimeMillis()) ? bestBy : prev.bestBy();
      map.put(bossId, new Stats.BossStats(defeats, best, bestPlayer));
    }
  }

  public int defeats(final String bossId) {
    final var s = this.stats.get().bosses().get(bossId);
    return s == null ? 0 : s.defeats();
  }

  public long bestTimeMillis(final String bossId) {
    final var s = this.stats.get().bosses().get(bossId);
    return s == null ? 0L : s.bestTimeMillis();
  }
}
