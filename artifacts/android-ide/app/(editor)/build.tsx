import { Feather } from "@expo/vector-icons";
import React, { useEffect, useRef } from "react";
import {
  ActivityIndicator,
  Platform,
  ScrollView,
  StyleSheet,
  Text,
  TouchableOpacity,
  View,
} from "react-native";
import BuildLogView from "@/components/BuildLogView";
import colors from "@/constants/colors";
import { useApp } from "@/context/AppContext";
import { BuildStep, useBuild } from "@/context/BuildContext";

function StepRow({ step, active }: { step: BuildStep; active: boolean }) {
  return (
    <View style={[styles.stepRow, active && styles.stepRowActive]}>
      <View style={styles.stepStatus}>
        {step.status === "running" && (
          <ActivityIndicator size="small" color={colors.light.primary} />
        )}
        {step.status === "done" && (
          <Feather name="check-circle" size={16} color={colors.light.success} />
        )}
        {step.status === "error" && (
          <Feather name="x-circle" size={16} color={colors.light.buildError} />
        )}
        {step.status === "pending" && (
          <View style={styles.pendingDot} />
        )}
      </View>
      <View style={styles.stepInfo}>
        <Text style={[styles.stepName, step.status === "done" && styles.stepDone]}>
          {step.name}
        </Text>
        <Text style={styles.stepTool}>{step.tool}</Text>
      </View>
      {step.duration && (
        <Text style={styles.stepDuration}>{step.duration}ms</Text>
      )}
    </View>
  );
}

export default function BuildScreen() {
  const { buildLogs, isBuilding, activeProject } = useApp();
  const { buildSteps, currentStep, runBuild, runClean, cancelBuild } = useBuild();
  const scrollRef = useRef<ScrollView>(null);

  useEffect(() => {
    scrollRef.current?.scrollToEnd({ animated: true });
  }, [buildLogs]);

  return (
    <View
      style={[
        styles.container,
        {
          paddingBottom: Platform.OS === "web" ? 34 : 0,
        },
      ]}
    >
      <View style={styles.toolbar}>
        <View style={styles.toolbarLeft}>
          <Text style={styles.projectLabel}>{activeProject?.name ?? "No Project"}</Text>
          <View style={[styles.variantBadge, { backgroundColor: colors.light.surface }]}>
            <Text style={styles.variantText}>
              {activeProject?.buildVariant ?? "debug"}
            </Text>
          </View>
        </View>
        <View style={styles.toolbarBtns}>
          {isBuilding ? (
            <TouchableOpacity style={styles.cancelBtn} onPress={cancelBuild} activeOpacity={0.8}>
              <Feather name="square" size={14} color={colors.light.destructive} />
              <Text style={styles.cancelBtnText}>Stop</Text>
            </TouchableOpacity>
          ) : (
            <>
              <TouchableOpacity style={styles.cleanBtn} onPress={runClean} activeOpacity={0.8}>
                <Feather name="trash-2" size={14} color={colors.light.mutedForeground} />
                <Text style={styles.cleanBtnText}>Clean</Text>
              </TouchableOpacity>
              <TouchableOpacity
                style={[styles.buildBtn, !activeProject && styles.buildBtnDisabled]}
                onPress={runBuild}
                disabled={!activeProject}
                activeOpacity={0.8}
              >
                <Feather name="play" size={14} color={colors.light.primaryForeground} />
                <Text style={styles.buildBtnText}>Build</Text>
              </TouchableOpacity>
            </>
          )}
        </View>
      </View>

      {buildSteps.length > 0 && (
        <View style={styles.stepsPanel}>
          <Text style={styles.stepsTitle}>Build Pipeline</Text>
          <ScrollView horizontal showsHorizontalScrollIndicator={false}>
            <View style={styles.stepsList}>
              {buildSteps.map((step, i) => (
                <StepRow key={i} step={step} active={i === currentStep} />
              ))}
            </View>
          </ScrollView>
        </View>
      )}

      <View style={styles.logPanel}>
        <View style={styles.logHeader}>
          <Feather name="terminal" size={14} color={colors.light.mutedForeground} />
          <Text style={styles.logHeaderText}>Build Output</Text>
          {isBuilding && (
            <ActivityIndicator
              size="small"
              color={colors.light.primary}
              style={{ marginLeft: 8 }}
            />
          )}
          <View style={{ flex: 1 }} />
          <TouchableOpacity
            onPress={() => scrollRef.current?.scrollToEnd({ animated: true })}
            hitSlop={8}
          >
            <Feather name="arrow-down" size={14} color={colors.light.mutedForeground} />
          </TouchableOpacity>
        </View>
        <BuildLogView logs={buildLogs} scrollRef={scrollRef} />
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
    borderBottomWidth: 1,
    borderBottomColor: colors.light.border,
    paddingHorizontal: 12,
    paddingVertical: 8,
    gap: 8,
  },
  toolbarLeft: { flex: 1, flexDirection: "row", alignItems: "center", gap: 8 },
  projectLabel: { color: colors.light.foreground, fontSize: 14, fontWeight: "600" },
  variantBadge: {
    paddingHorizontal: 8,
    paddingVertical: 2,
    borderRadius: 4,
    borderWidth: 1,
    borderColor: colors.light.border,
  },
  variantText: { color: colors.light.mutedForeground, fontSize: 11, fontWeight: "500" },
  toolbarBtns: { flexDirection: "row", gap: 8 },
  buildBtn: {
    flexDirection: "row",
    alignItems: "center",
    backgroundColor: colors.light.primary,
    paddingHorizontal: 14,
    paddingVertical: 7,
    borderRadius: 8,
    gap: 6,
  },
  buildBtnDisabled: { opacity: 0.5 },
  buildBtnText: { color: colors.light.primaryForeground, fontWeight: "600", fontSize: 13 },
  cleanBtn: {
    flexDirection: "row",
    alignItems: "center",
    backgroundColor: colors.light.surface,
    paddingHorizontal: 10,
    paddingVertical: 7,
    borderRadius: 8,
    gap: 4,
    borderWidth: 1,
    borderColor: colors.light.border,
  },
  cleanBtnText: { color: colors.light.mutedForeground, fontSize: 13 },
  cancelBtn: {
    flexDirection: "row",
    alignItems: "center",
    backgroundColor: colors.light.surface,
    paddingHorizontal: 10,
    paddingVertical: 7,
    borderRadius: 8,
    gap: 4,
    borderWidth: 1,
    borderColor: colors.light.destructive + "66",
  },
  cancelBtnText: { color: colors.light.destructive, fontSize: 13 },
  stepsPanel: {
    backgroundColor: colors.light.card,
    borderBottomWidth: 1,
    borderBottomColor: colors.light.border,
    paddingVertical: 8,
  },
  stepsTitle: {
    color: colors.light.mutedForeground,
    fontSize: 11,
    fontWeight: "600",
    textTransform: "uppercase",
    letterSpacing: 0.6,
    paddingHorizontal: 12,
    marginBottom: 6,
  },
  stepsList: {
    flexDirection: "column",
    paddingHorizontal: 12,
  },
  stepRow: {
    flexDirection: "row",
    alignItems: "center",
    paddingVertical: 4,
    gap: 8,
    borderRadius: 6,
    paddingHorizontal: 4,
  },
  stepRowActive: {
    backgroundColor: colors.light.surfaceHighlight,
  },
  stepStatus: { width: 20, alignItems: "center" },
  pendingDot: {
    width: 8,
    height: 8,
    borderRadius: 4,
    backgroundColor: colors.light.border,
  },
  stepInfo: { flex: 1 },
  stepName: { color: colors.light.foreground, fontSize: 12, fontWeight: "500" },
  stepDone: { color: colors.light.mutedForeground },
  stepTool: { color: colors.light.mutedForeground, fontSize: 10, fontFamily: "monospace" },
  stepDuration: { color: colors.light.mutedForeground, fontSize: 10, fontFamily: "monospace" },
  logPanel: { flex: 1 },
  logHeader: {
    flexDirection: "row",
    alignItems: "center",
    backgroundColor: colors.light.card,
    paddingHorizontal: 12,
    paddingVertical: 6,
    borderBottomWidth: 1,
    borderBottomColor: colors.light.border,
    gap: 6,
  },
  logHeaderText: { color: colors.light.mutedForeground, fontSize: 12, fontWeight: "500" },
});
