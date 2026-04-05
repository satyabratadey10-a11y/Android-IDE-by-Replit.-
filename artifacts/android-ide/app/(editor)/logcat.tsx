import { Feather } from "@expo/vector-icons";
import React, { useEffect, useRef, useState } from "react";
import {
  Platform,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  TouchableOpacity,
  View,
} from "react-native";
import colors from "@/constants/colors";

interface LogEntry {
  id: string;
  time: string;
  pid: string;
  level: "V" | "D" | "I" | "W" | "E";
  tag: string;
  message: string;
}

function generateId() {
  return Date.now().toString() + Math.random().toString(36).substr(2, 9);
}

function levelColor(level: LogEntry["level"]): string {
  switch (level) {
    case "E": return colors.light.buildError;
    case "W": return colors.light.buildWarning;
    case "I": return colors.light.buildInfo;
    case "D": return colors.light.success;
    case "V": return colors.light.mutedForeground;
    default: return colors.light.foreground;
  }
}

const DEMO_LOGS: LogEntry[] = [
  { id: "1", time: "12:00:01.123", pid: "2345", level: "I", tag: "ActivityManager", message: "Start proc 2345:com.example.myapp/u0a82" },
  { id: "2", time: "12:00:01.234", pid: "2345", level: "D", tag: "MainActivity", message: "onCreate called" },
  { id: "3", time: "12:00:01.345", pid: "2345", level: "I", tag: "ViewRootImpl", message: "Displaying Window{...} of com.example.myapp" },
  { id: "4", time: "12:00:01.456", pid: "2345", level: "V", tag: "Compose", message: "Recomposing GreetingPreview: 0 states changed" },
  { id: "5", time: "12:00:02.000", pid: "2345", level: "W", tag: "StrictMode", message: "StrictMode policy violation: android.os.StrictMode$StrictModeDiskReadViolation" },
  { id: "6", time: "12:00:02.123", pid: "2345", level: "I", tag: "Choreographer", message: "Skipped 3 frames! App may be doing too much work on main thread." },
  { id: "7", time: "12:00:03.001", pid: "2345", level: "D", tag: "OkHttp", message: "--> GET https://api.example.com/data" },
  { id: "8", time: "12:00:03.450", pid: "2345", level: "D", tag: "OkHttp", message: "<-- 200 OK https://api.example.com/data (449ms)" },
  { id: "9", time: "12:00:04.001", pid: "2345", level: "E", tag: "Retrofit", message: "java.net.UnknownHostException: Unable to resolve host 'api.example.com'" },
  { id: "10", time: "12:00:05.100", pid: "2345", level: "I", tag: "System.gc", message: "Explicit concurrent GC (Alloc)" },
];

function generateRandomLog(): LogEntry {
  const levels: LogEntry["level"][] = ["V", "D", "I", "W", "E"];
  const tags = ["ActivityManager", "MainActivity", "Compose", "OkHttp", "SQLite", "GC", "View", "System"];
  const messages = [
    "Background thread executing task...",
    "Layout pass completed in 12ms",
    "Memory usage: 48MB / 192MB",
    "Dispatching key event ACTION_DOWN",
    "Saving instance state",
    "Fragment attached to activity",
    "Database query executed in 3ms",
    "Cache hit for key: user_prefs",
    "Network request queued",
    "Animation started: alpha 0.0 -> 1.0",
  ];
  const now = new Date();
  const t = `${now.getHours().toString().padStart(2,"0")}:${now.getMinutes().toString().padStart(2,"0")}:${now.getSeconds().toString().padStart(2,"0")}.${now.getMilliseconds().toString().padStart(3,"0")}`;
  return {
    id: generateId(),
    time: t,
    pid: "2345",
    level: levels[Math.floor(Math.random() * levels.length)],
    tag: tags[Math.floor(Math.random() * tags.length)],
    message: messages[Math.floor(Math.random() * messages.length)],
  };
}

export default function LogcatScreen() {
  const [logs, setLogs] = useState<LogEntry[]>(DEMO_LOGS);
  const [filter, setFilter] = useState("");
  const [levelFilter, setLevelFilter] = useState<LogEntry["level"] | "ALL">("ALL");
  const [streaming, setStreaming] = useState(false);
  const scrollRef = useRef<ScrollView>(null);
  const intervalRef = useRef<ReturnType<typeof setInterval> | null>(null);

  const toggleStream = () => {
    setStreaming((v) => {
      if (!v) {
        intervalRef.current = setInterval(() => {
          setLogs((prev) => [...prev.slice(-200), generateRandomLog()]);
          scrollRef.current?.scrollToEnd({ animated: false });
        }, 1200);
      } else {
        if (intervalRef.current) clearInterval(intervalRef.current);
      }
      return !v;
    });
  };

  useEffect(() => {
    return () => {
      if (intervalRef.current) clearInterval(intervalRef.current);
    };
  }, []);

  useEffect(() => {
    if (streaming) {
      scrollRef.current?.scrollToEnd({ animated: true });
    }
  }, [logs, streaming]);

  const levels: (LogEntry["level"] | "ALL")[] = ["ALL", "V", "D", "I", "W", "E"];

  const filtered = logs.filter((l) => {
    const matchLevel = levelFilter === "ALL" || l.level === levelFilter;
    const matchFilter =
      !filter ||
      l.tag.toLowerCase().includes(filter.toLowerCase()) ||
      l.message.toLowerCase().includes(filter.toLowerCase());
    return matchLevel && matchFilter;
  });

  return (
    <View
      style={[
        styles.container,
        { paddingBottom: Platform.OS === "web" ? 34 : 0 },
      ]}
    >
      <View style={styles.toolbar}>
        <TextInput
          style={styles.searchInput}
          value={filter}
          onChangeText={setFilter}
          placeholder="Filter by tag or message..."
          placeholderTextColor={colors.light.mutedForeground}
          autoCapitalize="none"
          autoCorrect={false}
        />
        <TouchableOpacity
          style={[styles.streamBtn, streaming && styles.streamBtnActive]}
          onPress={toggleStream}
          activeOpacity={0.8}
        >
          <Feather
            name={streaming ? "pause" : "play"}
            size={14}
            color={streaming ? colors.light.success : colors.light.foreground}
          />
        </TouchableOpacity>
        <TouchableOpacity
          style={styles.clearBtn}
          onPress={() => setLogs([])}
          activeOpacity={0.8}
        >
          <Feather name="trash-2" size={14} color={colors.light.mutedForeground} />
        </TouchableOpacity>
      </View>

      <ScrollView horizontal style={styles.levelBar} showsHorizontalScrollIndicator={false}>
        <View style={styles.levelBarInner}>
          {levels.map((l) => (
            <TouchableOpacity
              key={l}
              style={[styles.levelBtn, levelFilter === l && styles.levelBtnActive]}
              onPress={() => setLevelFilter(l)}
              activeOpacity={0.7}
            >
              <Text
                style={[
                  styles.levelBtnText,
                  l !== "ALL" && { color: levelColor(l as LogEntry["level"]) },
                  levelFilter === l && styles.levelBtnTextActive,
                ]}
              >
                {l}
              </Text>
            </TouchableOpacity>
          ))}
        </View>
      </ScrollView>

      <ScrollView
        ref={scrollRef}
        style={styles.logScroll}
        contentContainerStyle={styles.logContent}
        showsVerticalScrollIndicator={false}
      >
        {filtered.map((log) => (
          <View key={log.id} style={styles.logRow}>
            <Text style={styles.logTime}>{log.time}</Text>
            <Text style={[styles.logLevel, { color: levelColor(log.level) }]}>
              {log.level}
            </Text>
            <Text style={styles.logTag} numberOfLines={1}>
              {log.tag}
            </Text>
            <Text style={styles.logMsg} numberOfLines={2}>
              {log.message}
            </Text>
          </View>
        ))}
        {filtered.length === 0 && (
          <View style={styles.empty}>
            <Feather name="activity" size={32} color={colors.light.mutedForeground} />
            <Text style={styles.emptyText}>No logs match filter</Text>
          </View>
        )}
      </ScrollView>

      <View style={styles.statusBar}>
        <Text style={styles.statusText}>{filtered.length} entries</Text>
        {streaming && (
          <View style={styles.streamingDot}>
            <View style={styles.dot} />
            <Text style={styles.streamingText}>Live</Text>
          </View>
        )}
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: colors.light.background },
  toolbar: {
    flexDirection: "row",
    alignItems: "center",
    backgroundColor: colors.light.headerBg,
    paddingHorizontal: 10,
    paddingVertical: 8,
    borderBottomWidth: 1,
    borderBottomColor: colors.light.border,
    gap: 8,
  },
  searchInput: {
    flex: 1,
    height: 32,
    backgroundColor: colors.light.input,
    borderRadius: 6,
    paddingHorizontal: 10,
    color: colors.light.foreground,
    fontSize: 12,
    borderWidth: 1,
    borderColor: colors.light.border,
  },
  streamBtn: {
    padding: 7,
    backgroundColor: colors.light.surface,
    borderRadius: 6,
    borderWidth: 1,
    borderColor: colors.light.border,
  },
  streamBtnActive: { borderColor: colors.light.success + "66" },
  clearBtn: {
    padding: 7,
    backgroundColor: colors.light.surface,
    borderRadius: 6,
    borderWidth: 1,
    borderColor: colors.light.border,
  },
  levelBar: { maxHeight: 36, backgroundColor: colors.light.surface, borderBottomWidth: 1, borderBottomColor: colors.light.border },
  levelBarInner: { flexDirection: "row", alignItems: "center", paddingHorizontal: 6, gap: 4, height: 36 },
  levelBtn: {
    paddingHorizontal: 10,
    paddingVertical: 4,
    borderRadius: 4,
  },
  levelBtnActive: { backgroundColor: colors.light.surfaceHighlight },
  levelBtnText: { color: colors.light.mutedForeground, fontSize: 12, fontWeight: "600", fontFamily: "monospace" },
  levelBtnTextActive: { color: colors.light.foreground },
  logScroll: { flex: 1, backgroundColor: colors.light.terminalBg },
  logContent: { paddingVertical: 4 },
  logRow: {
    flexDirection: "row",
    alignItems: "flex-start",
    paddingHorizontal: 8,
    paddingVertical: 2,
    borderBottomWidth: 1,
    borderBottomColor: colors.light.border + "22",
  },
  logTime: {
    color: colors.light.mutedForeground,
    fontSize: 10,
    fontFamily: "monospace",
    width: 82,
    paddingTop: 1,
  },
  logLevel: {
    fontSize: 11,
    fontFamily: "monospace",
    fontWeight: "700",
    width: 16,
    textAlign: "center",
    marginHorizontal: 4,
    paddingTop: 1,
  },
  logTag: {
    color: colors.light.accent,
    fontSize: 11,
    fontFamily: "monospace",
    width: 90,
    paddingTop: 1,
  },
  logMsg: {
    flex: 1,
    color: colors.light.foreground,
    fontSize: 11,
    fontFamily: "monospace",
    lineHeight: 16,
  },
  empty: {
    alignItems: "center",
    paddingTop: 40,
    gap: 8,
  },
  emptyText: { color: colors.light.mutedForeground, fontSize: 14 },
  statusBar: {
    flexDirection: "row",
    alignItems: "center",
    paddingHorizontal: 12,
    paddingVertical: 4,
    backgroundColor: colors.light.surface,
    borderTopWidth: 1,
    borderTopColor: colors.light.border,
    gap: 12,
  },
  statusText: { color: colors.light.mutedForeground, fontSize: 11 },
  streamingDot: { flexDirection: "row", alignItems: "center", gap: 4 },
  dot: { width: 6, height: 6, borderRadius: 3, backgroundColor: colors.light.success },
  streamingText: { color: colors.light.success, fontSize: 11, fontWeight: "600" },
});
