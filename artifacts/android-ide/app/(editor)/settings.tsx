import { Feather } from "@expo/vector-icons";
import React, { useState } from "react";
import {
  Platform,
  ScrollView,
  StyleSheet,
  Switch,
  Text,
  TouchableOpacity,
  View,
} from "react-native";
import colors from "@/constants/colors";
import { useApp } from "@/context/AppContext";

interface SettingRow {
  label: string;
  sub?: string;
  type: "toggle" | "value" | "action";
  value?: string;
  key?: string;
}

export default function SettingsScreen() {
  const { activeProject } = useApp();
  const [autoSave, setAutoSave] = useState(true);
  const [showLineNumbers, setShowLineNumbers] = useState(true);
  const [wordWrap, setWordWrap] = useState(false);
  const [autoBuild, setAutoBuild] = useState(false);
  const [r8Enabled, setR8Enabled] = useState(true);
  const [minify, setMinify] = useState(true);
  const [fontSize, setFontSize] = useState("13");

  const EDITOR_SETTINGS: SettingRow[] = [
    { label: "Auto Save", sub: "Save files automatically on edit", type: "toggle", key: "autoSave" },
    { label: "Line Numbers", sub: "Show line numbers in editor", type: "toggle", key: "lineNumbers" },
    { label: "Word Wrap", sub: "Wrap long lines in editor", type: "toggle", key: "wordWrap" },
    { label: "Font Size", sub: "Code editor font size (px)", type: "value", value: fontSize },
  ];

  const BUILD_SETTINGS: SettingRow[] = [
    { label: "Auto Build", sub: "Build on every file save", type: "toggle", key: "autoBuild" },
    { label: "R8 / ProGuard", sub: "Enable code shrinking", type: "toggle", key: "r8" },
    { label: "Minify", sub: "Enable resource shrinking", type: "toggle", key: "minify" },
    { label: "Target SDK", sub: "Target Android SDK version", type: "value", value: "34" },
    { label: "Min SDK", sub: "Minimum SDK version", type: "value", value: "24" },
    { label: "Build Variant", sub: "Active build configuration", type: "value", value: activeProject?.buildVariant ?? "debug" },
  ];

  const TOOLCHAIN_SETTINGS: SettingRow[] = [
    { label: "NDK Version", type: "value", value: "26.3.11579264" },
    { label: "Build Tools", type: "value", value: "34.0.0" },
    { label: "Kotlin Version", type: "value", value: "1.9.24" },
    { label: "Gradle Version", type: "value", value: "8.7" },
    { label: "Java Version", type: "value", value: "17" },
    { label: "LLVM/Clang", type: "value", value: "17.0.2" },
    { label: "D8/R8", type: "value", value: "8.2.42" },
    { label: "AAPT2", type: "value", value: "34.0.0" },
  ];

  function getToggleValue(key: string | undefined): boolean {
    if (key === "autoSave") return autoSave;
    if (key === "lineNumbers") return showLineNumbers;
    if (key === "wordWrap") return wordWrap;
    if (key === "autoBuild") return autoBuild;
    if (key === "r8") return r8Enabled;
    if (key === "minify") return minify;
    return false;
  }

  function setToggleValue(key: string | undefined, v: boolean) {
    if (key === "autoSave") setAutoSave(v);
    if (key === "lineNumbers") setShowLineNumbers(v);
    if (key === "wordWrap") setWordWrap(v);
    if (key === "autoBuild") setAutoBuild(v);
    if (key === "r8") setR8Enabled(v);
    if (key === "minify") setMinify(v);
  }

  function renderSection(title: string, rows: SettingRow[]) {
    return (
      <View style={styles.section}>
        <Text style={styles.sectionTitle}>{title}</Text>
        <View style={styles.sectionCard}>
          {rows.map((row, i) => (
            <View
              key={i}
              style={[
                styles.row,
                i < rows.length - 1 && styles.rowDivider,
              ]}
            >
              <View style={styles.rowLabel}>
                <Text style={styles.label}>{row.label}</Text>
                {row.sub && <Text style={styles.sub}>{row.sub}</Text>}
              </View>
              {row.type === "toggle" && (
                <Switch
                  value={getToggleValue(row.key)}
                  onValueChange={(v) => setToggleValue(row.key, v)}
                  trackColor={{
                    false: colors.light.border,
                    true: colors.light.primary,
                  }}
                  thumbColor={colors.light.foreground}
                />
              )}
              {row.type === "value" && (
                <Text style={styles.valueText}>{row.value}</Text>
              )}
            </View>
          ))}
        </View>
      </View>
    );
  }

  return (
    <ScrollView
      style={[
        styles.container,
        { paddingBottom: Platform.OS === "web" ? 34 : 0 },
      ]}
      contentContainerStyle={styles.content}
    >
      {renderSection("Editor", EDITOR_SETTINGS)}
      {renderSection("Build", BUILD_SETTINGS)}
      {renderSection("Toolchain Versions", TOOLCHAIN_SETTINGS)}

      <View style={styles.section}>
        <Text style={styles.sectionTitle}>About</Text>
        <View style={styles.sectionCard}>
          <View style={styles.aboutRow}>
            <Feather name="code" size={32} color={colors.light.primary} />
            <View>
              <Text style={styles.aboutName}>Android IDE</Text>
              <Text style={styles.aboutVersion}>v1.0.0 · Full Toolchain Edition</Text>
            </View>
          </View>
          <Text style={styles.aboutDesc}>
            A fully-featured Android development environment running on your mobile device.
            Includes the complete Android SDK toolchain: AAPT2, D8, R8, APKSigner, ZipAlign, ADB, NDK, and more.
          </Text>
        </View>
      </View>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: colors.light.background },
  content: { padding: 12, gap: 16, paddingBottom: 40 },
  section: { gap: 6 },
  sectionTitle: {
    color: colors.light.mutedForeground,
    fontSize: 11,
    fontWeight: "600",
    textTransform: "uppercase",
    letterSpacing: 0.6,
    paddingHorizontal: 4,
  },
  sectionCard: {
    backgroundColor: colors.light.card,
    borderRadius: 12,
    borderWidth: 1,
    borderColor: colors.light.border,
    overflow: "hidden",
  },
  row: {
    flexDirection: "row",
    alignItems: "center",
    paddingHorizontal: 14,
    paddingVertical: 12,
    gap: 12,
  },
  rowDivider: {
    borderBottomWidth: 1,
    borderBottomColor: colors.light.border,
  },
  rowLabel: { flex: 1 },
  label: { color: colors.light.foreground, fontSize: 14 },
  sub: { color: colors.light.mutedForeground, fontSize: 11, marginTop: 2 },
  valueText: {
    color: colors.light.mutedForeground,
    fontSize: 13,
    fontFamily: "monospace",
  },
  aboutRow: {
    flexDirection: "row",
    alignItems: "center",
    gap: 14,
    paddingHorizontal: 14,
    paddingVertical: 14,
    borderBottomWidth: 1,
    borderBottomColor: colors.light.border,
  },
  aboutName: { color: colors.light.foreground, fontSize: 16, fontWeight: "700" },
  aboutVersion: { color: colors.light.mutedForeground, fontSize: 12 },
  aboutDesc: {
    color: colors.light.mutedForeground,
    fontSize: 13,
    lineHeight: 20,
    paddingHorizontal: 14,
    paddingVertical: 12,
  },
});
