import { Feather } from "@expo/vector-icons";
import React from "react";
import { ScrollView, StyleSheet, Text, View } from "react-native";
import colors from "@/constants/colors";
import { BuildLog } from "@/context/AppContext";

interface Props {
  logs: BuildLog[];
  scrollRef?: React.RefObject<ScrollView | null>;
}

function logColor(type: BuildLog["type"]): string {
  switch (type) {
    case "success": return colors.light.buildSuccess;
    case "error": return colors.light.buildError;
    case "warning": return colors.light.buildWarning;
    case "info": return colors.light.buildInfo;
    case "verbose": return colors.light.mutedForeground;
    default: return colors.light.foreground;
  }
}

function logIcon(type: BuildLog["type"]) {
  switch (type) {
    case "success": return <Feather name="check-circle" size={12} color={colors.light.buildSuccess} />;
    case "error": return <Feather name="x-circle" size={12} color={colors.light.buildError} />;
    case "warning": return <Feather name="alert-triangle" size={12} color={colors.light.buildWarning} />;
    case "info": return <Feather name="info" size={12} color={colors.light.buildInfo} />;
    default: return null;
  }
}

export default function BuildLogView({ logs, scrollRef }: Props) {
  return (
    <ScrollView
      ref={scrollRef as any}
      style={styles.container}
      contentContainerStyle={styles.content}
      showsVerticalScrollIndicator={false}
    >
      {logs.length === 0 && (
        <View style={styles.empty}>
          <Feather name="terminal" size={32} color={colors.light.mutedForeground} />
          <Text style={styles.emptyText}>No build output yet</Text>
          <Text style={styles.emptySubText}>Run a build to see logs here</Text>
        </View>
      )}
      {logs.map((log) => (
        <View key={log.id} style={styles.logRow}>
          <View style={styles.iconCol}>{logIcon(log.type)}</View>
          <View style={styles.textCol}>
            {log.tool && (
              <Text style={styles.toolLabel}>[{log.tool}] </Text>
            )}
            <Text style={[styles.logText, { color: logColor(log.type) }]}>
              {log.message}
            </Text>
          </View>
        </View>
      ))}
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: colors.light.terminalBg,
  },
  content: {
    padding: 8,
    paddingBottom: 40,
  },
  empty: {
    flex: 1,
    alignItems: "center",
    justifyContent: "center",
    paddingTop: 60,
    gap: 8,
  },
  emptyText: {
    color: colors.light.mutedForeground,
    fontSize: 16,
    fontWeight: "600",
  },
  emptySubText: {
    color: colors.light.mutedForeground,
    fontSize: 13,
  },
  logRow: {
    flexDirection: "row",
    alignItems: "flex-start",
    minHeight: 18,
    marginBottom: 2,
  },
  iconCol: {
    width: 18,
    paddingTop: 2,
  },
  textCol: {
    flex: 1,
    flexDirection: "row",
    flexWrap: "wrap",
  },
  toolLabel: {
    color: colors.light.mutedForeground,
    fontSize: 11,
    fontFamily: "monospace",
  },
  logText: {
    fontSize: 12,
    fontFamily: "monospace",
    lineHeight: 18,
  },
});
