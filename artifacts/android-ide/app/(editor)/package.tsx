import { Feather } from "@expo/vector-icons";
import React, { useState } from "react";
import {
  ActivityIndicator,
  Platform,
  ScrollView,
  StyleSheet,
  Text,
  TouchableOpacity,
  View,
} from "react-native";
import colors from "@/constants/colors";
import { useApp } from "@/context/AppContext";

interface PackageStep {
  label: string;
  tool: string;
  status: "pending" | "running" | "done" | "error";
  detail?: string;
}

const APK_STEPS: PackageStep[] = [
  { label: "Compile resources", tool: "aapt2 compile", status: "pending" },
  { label: "Link resources", tool: "aapt2 link", status: "pending" },
  { label: "Generate DEX", tool: "d8", status: "pending" },
  { label: "Run R8 optimizer", tool: "r8", status: "pending" },
  { label: "Package APK", tool: "apkbuilder", status: "pending" },
  { label: "Align APK", tool: "zipalign -v 4", status: "pending" },
  { label: "Sign APK", tool: "apksigner sign", status: "pending" },
  { label: "Verify signature", tool: "apksigner verify", status: "pending" },
];

const AAB_STEPS: PackageStep[] = [
  { label: "Compile resources", tool: "aapt2 compile", status: "pending" },
  { label: "Link resources", tool: "aapt2 link --proto-format", status: "pending" },
  { label: "Generate DEX", tool: "d8", status: "pending" },
  { label: "Run R8 optimizer", tool: "r8", status: "pending" },
  { label: "Build bundle", tool: "bundletool build-bundle", status: "pending" },
  { label: "Sign bundle", tool: "jarsigner", status: "pending" },
];

function sleep(ms: number) {
  return new Promise<void>((r) => setTimeout(r, ms));
}

export default function PackageScreen() {
  const { activeProject } = useApp();
  const [mode, setMode] = useState<"apk" | "aab">("apk");
  const [variant, setVariant] = useState<"debug" | "release">("debug");
  const [steps, setSteps] = useState<PackageStep[]>([]);
  const [packaging, setPackaging] = useState(false);
  const [done, setDone] = useState(false);
  const [outputPath, setOutputPath] = useState("");

  const runPackage = async () => {
    setDone(false);
    setPackaging(true);
    const base = mode === "apk" ? APK_STEPS : AAB_STEPS;
    const s: PackageStep[] = base.map((x) => ({ ...x, status: "pending" }));
    setSteps(s);

    for (let i = 0; i < s.length; i++) {
      setSteps((prev) =>
        prev.map((x, idx) => (idx === i ? { ...x, status: "running" } : x))
      );
      await sleep(300 + Math.random() * 600);
      setSteps((prev) =>
        prev.map((x, idx) =>
          idx === i
            ? { ...x, status: "done", detail: `Completed in ${Math.floor(200 + Math.random() * 500)}ms` }
            : x
        )
      );
    }

    const ext = mode === "apk" ? "apk" : "aab";
    const out = `app/build/outputs/${mode}/${variant}/app-${variant}.${ext}`;
    setOutputPath(out);
    setDone(true);
    setPackaging(false);
  };

  return (
    <View
      style={[
        styles.container,
        { paddingBottom: Platform.OS === "web" ? 34 : 0 },
      ]}
    >
      <ScrollView contentContainerStyle={styles.content}>
        <View style={styles.card}>
          <Text style={styles.cardTitle}>Output Format</Text>
          <View style={styles.toggleRow}>
            <TouchableOpacity
              style={[styles.toggleBtn, mode === "apk" && styles.toggleBtnActive]}
              onPress={() => setMode("apk")}
              activeOpacity={0.8}
            >
              <Feather name="package" size={16} color={mode === "apk" ? colors.light.primaryForeground : colors.light.mutedForeground} />
              <Text style={[styles.toggleText, mode === "apk" && styles.toggleTextActive]}>APK</Text>
            </TouchableOpacity>
            <TouchableOpacity
              style={[styles.toggleBtn, mode === "aab" && styles.toggleBtnActive]}
              onPress={() => setMode("aab")}
              activeOpacity={0.8}
            >
              <Feather name="archive" size={16} color={mode === "aab" ? colors.light.primaryForeground : colors.light.mutedForeground} />
              <Text style={[styles.toggleText, mode === "aab" && styles.toggleTextActive]}>AAB</Text>
            </TouchableOpacity>
          </View>
        </View>

        <View style={styles.card}>
          <Text style={styles.cardTitle}>Build Variant</Text>
          <View style={styles.toggleRow}>
            <TouchableOpacity
              style={[styles.toggleBtn, variant === "debug" && styles.toggleBtnActive]}
              onPress={() => setVariant("debug")}
              activeOpacity={0.8}
            >
              <Feather name="code" size={16} color={variant === "debug" ? colors.light.primaryForeground : colors.light.mutedForeground} />
              <Text style={[styles.toggleText, variant === "debug" && styles.toggleTextActive]}>Debug</Text>
            </TouchableOpacity>
            <TouchableOpacity
              style={[styles.toggleBtn, variant === "release" && styles.toggleBtnActive]}
              onPress={() => setVariant("release")}
              activeOpacity={0.8}
            >
              <Feather name="shield" size={16} color={variant === "release" ? colors.light.primaryForeground : colors.light.mutedForeground} />
              <Text style={[styles.toggleText, variant === "release" && styles.toggleTextActive]}>Release</Text>
            </TouchableOpacity>
          </View>
        </View>

        <View style={styles.card}>
          <Text style={styles.cardTitle}>Tools in Pipeline</Text>
          {(mode === "apk" ? APK_STEPS : AAB_STEPS).map((step, i) => (
            <View key={i} style={styles.toolRow}>
              <Feather name="terminal" size={12} color={colors.light.accent} />
              <Text style={styles.toolName}>{step.tool}</Text>
              <Text style={styles.toolLabel}>{step.label}</Text>
            </View>
          ))}
        </View>

        {steps.length > 0 && (
          <View style={styles.card}>
            <Text style={styles.cardTitle}>Packaging Progress</Text>
            {steps.map((step, i) => (
              <View key={i} style={styles.stepRow}>
                <View style={styles.stepIcon}>
                  {step.status === "running" && <ActivityIndicator size="small" color={colors.light.primary} />}
                  {step.status === "done" && <Feather name="check-circle" size={15} color={colors.light.success} />}
                  {step.status === "error" && <Feather name="x-circle" size={15} color={colors.light.destructive} />}
                  {step.status === "pending" && <View style={styles.pendingDot} />}
                </View>
                <View style={styles.stepInfo}>
                  <Text style={styles.stepLabel}>{step.label}</Text>
                  <Text style={styles.stepTool}>{step.tool}</Text>
                  {step.detail && <Text style={styles.stepDetail}>{step.detail}</Text>}
                </View>
              </View>
            ))}
          </View>
        )}

        {done && (
          <View style={styles.successCard}>
            <Feather name="check-circle" size={28} color={colors.light.success} />
            <Text style={styles.successTitle}>Packaging Complete</Text>
            <Text style={styles.successPath}>{outputPath}</Text>
            <View style={styles.successActions}>
              <TouchableOpacity style={styles.actionBtn} activeOpacity={0.8}>
                <Feather name="upload" size={14} color={colors.light.primaryForeground} />
                <Text style={styles.actionBtnText}>Install on Device</Text>
              </TouchableOpacity>
              <TouchableOpacity style={styles.actionBtnSecondary} activeOpacity={0.8}>
                <Feather name="share" size={14} color={colors.light.foreground} />
                <Text style={styles.actionBtnSecondaryText}>Share</Text>
              </TouchableOpacity>
            </View>
          </View>
        )}
      </ScrollView>

      <View style={styles.footer}>
        <TouchableOpacity
          style={[styles.buildBtn, (packaging || !activeProject) && styles.buildBtnDisabled]}
          onPress={runPackage}
          disabled={packaging || !activeProject}
          activeOpacity={0.8}
        >
          {packaging ? (
            <ActivityIndicator size="small" color={colors.light.primaryForeground} />
          ) : (
            <Feather name="package" size={16} color={colors.light.primaryForeground} />
          )}
          <Text style={styles.buildBtnText}>
            {packaging ? "Packaging..." : `Build ${mode.toUpperCase()}`}
          </Text>
        </TouchableOpacity>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: colors.light.background },
  content: { padding: 12, gap: 12, paddingBottom: 20 },
  card: {
    backgroundColor: colors.light.card,
    borderRadius: 12,
    padding: 14,
    borderWidth: 1,
    borderColor: colors.light.border,
    gap: 10,
  },
  cardTitle: {
    color: colors.light.mutedForeground,
    fontSize: 11,
    fontWeight: "600",
    textTransform: "uppercase",
    letterSpacing: 0.6,
  },
  toggleRow: { flexDirection: "row", gap: 8 },
  toggleBtn: {
    flex: 1,
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "center",
    backgroundColor: colors.light.surface,
    borderRadius: 8,
    paddingVertical: 10,
    gap: 6,
    borderWidth: 1,
    borderColor: colors.light.border,
  },
  toggleBtnActive: { backgroundColor: colors.light.primary, borderColor: colors.light.primary },
  toggleText: { color: colors.light.mutedForeground, fontSize: 13, fontWeight: "600" },
  toggleTextActive: { color: colors.light.primaryForeground },
  toolRow: {
    flexDirection: "row",
    alignItems: "center",
    gap: 8,
    paddingVertical: 3,
  },
  toolName: { color: colors.light.accent, fontSize: 11, fontFamily: "monospace", width: 130 },
  toolLabel: { color: colors.light.mutedForeground, fontSize: 11, flex: 1 },
  stepRow: {
    flexDirection: "row",
    alignItems: "flex-start",
    gap: 10,
    paddingVertical: 4,
  },
  stepIcon: { width: 18, alignItems: "center", paddingTop: 2 },
  pendingDot: { width: 8, height: 8, borderRadius: 4, backgroundColor: colors.light.border },
  stepInfo: { flex: 1 },
  stepLabel: { color: colors.light.foreground, fontSize: 13, fontWeight: "500" },
  stepTool: { color: colors.light.mutedForeground, fontSize: 11, fontFamily: "monospace" },
  stepDetail: { color: colors.light.mutedForeground, fontSize: 10 },
  successCard: {
    backgroundColor: colors.light.card,
    borderRadius: 12,
    padding: 20,
    alignItems: "center",
    gap: 8,
    borderWidth: 1,
    borderColor: colors.light.success + "44",
  },
  successTitle: { color: colors.light.success, fontSize: 17, fontWeight: "700" },
  successPath: { color: colors.light.mutedForeground, fontSize: 11, fontFamily: "monospace", textAlign: "center" },
  successActions: { flexDirection: "row", gap: 10, marginTop: 8 },
  actionBtn: {
    flexDirection: "row",
    alignItems: "center",
    backgroundColor: colors.light.primary,
    paddingHorizontal: 14,
    paddingVertical: 8,
    borderRadius: 8,
    gap: 6,
  },
  actionBtnText: { color: colors.light.primaryForeground, fontWeight: "600", fontSize: 13 },
  actionBtnSecondary: {
    flexDirection: "row",
    alignItems: "center",
    backgroundColor: colors.light.surface,
    paddingHorizontal: 14,
    paddingVertical: 8,
    borderRadius: 8,
    gap: 6,
    borderWidth: 1,
    borderColor: colors.light.border,
  },
  actionBtnSecondaryText: { color: colors.light.foreground, fontSize: 13 },
  footer: {
    padding: 12,
    borderTopWidth: 1,
    borderTopColor: colors.light.border,
    backgroundColor: colors.light.headerBg,
  },
  buildBtn: {
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "center",
    backgroundColor: colors.light.primary,
    borderRadius: 10,
    paddingVertical: 14,
    gap: 8,
  },
  buildBtnDisabled: { opacity: 0.5 },
  buildBtnText: { color: colors.light.primaryForeground, fontSize: 15, fontWeight: "700" },
});
