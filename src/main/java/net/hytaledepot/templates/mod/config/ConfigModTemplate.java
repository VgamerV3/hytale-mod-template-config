package net.hytaledepot.templates.mod.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicBoolean;

public final class ConfigModTemplate {
  private final Map<String, AtomicLong> actionCounters = new ConcurrentHashMap<>();
  private final Map<String, String> lastActionBySender = new ConcurrentHashMap<>();
  private final AtomicBoolean demoFlagEnabled = new AtomicBoolean(false);
  private final AtomicLong errorCount = new AtomicLong();
  Properties liveConfig = new Properties();
  private volatile Path dataDirectory;
  private volatile Path configFile;

  public void onInitialize(Path dataDirectory) {
    this.dataDirectory = dataDirectory;
    this.configFile = dataDirectory.resolve("config-template.properties");
    loadConfig();
  }

  public void onShutdown() {
    saveConfig();
  }

  public void onHeartbeat(long tick) {
    actionCounters.computeIfAbsent("heartbeat", key -> new AtomicLong()).incrementAndGet();

  }

  public String runAction(String sender, String action, long heartbeatTicks) {
    String normalizedSender = String.valueOf(sender == null ? "unknown" : sender);
    String normalizedAction = normalizeAction(action);

    actionCounters.computeIfAbsent(normalizedAction, key -> new AtomicLong()).incrementAndGet();
    lastActionBySender.put(normalizedSender, normalizedAction);

    if ("toggle".equals(normalizedAction)) {
      boolean enabled = toggleFlag(demoFlagEnabled);
      return "[ConfigMod] demoFlag=" + enabled + ", heartbeatTicks=" + heartbeatTicks;
    }

    if ("info".equals(normalizedAction)) {
      return "[ConfigMod] " + diagnostics(normalizedSender, heartbeatTicks);
    }

    String domainResult = handleDomainAction(normalizedSender, normalizedAction, heartbeatTicks);
    if (domainResult != null) {
      return "[ConfigMod] " + domainResult;
    }

    return "[ConfigMod] unknown action='" + normalizedAction + "' (try: info, toggle, sample, reload-config, set-config, get-config)";
  }

  public String diagnostics(String sender, long heartbeatTicks) {
    String directory = dataDirectory == null ? "unset" : dataDirectory.toString();
    return "sender=" + sender
        + ", heartbeatTicks=" + heartbeatTicks
        + ", demoFlag=" + demoFlagEnabled.get()
        + ", ops=" + operationCount()
        + ", lastAction=" + lastActionBySender.getOrDefault(sender, "none")
        + ", errors=" + errorCount.get()
        + ", configKeys=" + liveConfig.size() + ", pvp=" + liveConfig.getProperty("pvp", "true") + ", maxPlayers=" + liveConfig.getProperty("maxPlayers", "80") + ", dataDirectory=" + directory;
  }

  public long operationCount() {
    long total = 0;
    for (AtomicLong value : actionCounters.values()) {
      total += value.get();
    }
    return total;
  }

  public void incrementErrorCount() {
    errorCount.incrementAndGet();
  }

  private String handleDomainAction(String sender, String action, long heartbeatTicks) {
    if ("sample".equals(action) || "reload-config".equals(action)) {
      loadConfig();
      return "config reloaded (pvp=" + liveConfig.getProperty("pvp") + ", maxPlayers=" + liveConfig.getProperty("maxPlayers") + ")";
    }
    if ("set-config".equals(action)) {
      String next = "true".equals(liveConfig.getProperty("pvp", "true")) ? "false" : "true";
      liveConfig.setProperty("pvp", next);
      saveConfig();
      return "config:pvp=" + next;
    }
    if ("get-config".equals(action)) {
      return "config:pvp=" + liveConfig.getProperty("pvp", "true") + ", config:maxPlayers=" + liveConfig.getProperty("maxPlayers", "80");
    }
    return null;
  }

  private void loadConfig() {
    liveConfig.clear();
    liveConfig.setProperty("pvp", "true");
    liveConfig.setProperty("maxPlayers", "80");
    if (configFile == null || !Files.exists(configFile)) {
      saveConfig();
      return;
    }
    try (InputStream in = Files.newInputStream(configFile)) {
      liveConfig.load(in);
    } catch (IOException ignored) {
      liveConfig.setProperty("pvp", "true");
      liveConfig.setProperty("maxPlayers", "80");
    }
  }

  private void saveConfig() {
    if (configFile == null) {
      return;
    }
    try {
      Files.createDirectories(configFile.getParent());
      try (OutputStream out = Files.newOutputStream(configFile)) {
        liveConfig.store(out, "Config template state");
      }
    } catch (IOException ignored) {
    }
  }

  private static String normalizeAction(String action) {
    String normalized = String.valueOf(action == null ? "" : action).trim().toLowerCase();
    return normalized.isEmpty() ? "sample" : normalized;
  }

  private static boolean toggleFlag(AtomicBoolean flag) {
    while (true) {
      boolean current = flag.get();
      boolean next = !current;
      if (flag.compareAndSet(current, next)) {
        return next;
      }
    }
  }
}
