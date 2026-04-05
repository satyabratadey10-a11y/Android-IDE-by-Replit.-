import { Feather } from "@expo/vector-icons";
import React, { useState } from "react";
import {
  Platform,
  ScrollView,
  StyleSheet,
  Text,
  TouchableOpacity,
  View,
} from "react-native";
import colors from "@/constants/colors";
import { useApp } from "@/context/AppContext";

interface StackFrame {
  id: string;
  method: string;
  file: string;
  line: number;
  active: boolean;
}

interface Variable {
  name: string;
  type: string;
  value: string;
  children?: Variable[];
}

const DEMO_FRAMES: StackFrame[] = [
  { id: "1", method: "MainActivity.onCreate", file: "MainActivity.kt", line: 15, active: true },
  { id: "2", method: "ComponentActivity.onCreate", file: "ComponentActivity.kt", line: 224, active: false },
  { id: "3", method: "FragmentActivity.onCreate", file: "FragmentActivity.kt", line: 347, active: false },
  { id: "4", method: "Activity.performCreate", file: "Activity.java", line: 8052, active: false },
];

const DEMO_VARS: Variable[] = [
  { name: "this", type: "MainActivity", value: "MainActivity@0x1a2b3c" },
  {
    name: "savedInstanceState",
    type: "Bundle?",
    value: "null",
  },
  {
    name: "binding", type: "ActivityMainBinding",
    value: "ActivityMainBinding@0x4d5e6f",
    children: [
      { name: "root", type: "ConstraintLayout", value: "ConstraintLayout@0x7a8b9c" },
      { name: "tvHello", type: "TextView", value: "TextView@0xab1234" },
    ],
  },
];

function VariableRow({ variable, depth = 0 }: { variable: Variable; depth?: number }) {
  const [expanded, setExpanded] = useState(false);
  const hasChildren = variable.children && variable.children.length > 0;

  return (
    <View>
      <TouchableOpacity
        style={[styles.varRow, { paddingLeft: 12 + depth * 16 }]}
        onPress={() => hasChildren && setExpanded((v) => !v)}
        activeOpacity={hasChildren ? 0.7 : 1}
      >
        {hasChildren ? (
          <Feather
            name={expanded ? "chevron-down" : "chevron-right"}
            size={12}
            color={colors.light.mutedForeground}
            style={{ marginRight: 4 }}
          />
        ) : (
          <View style={{ width: 16 }} />
        )}
        <Text style={styles.varName}>{variable.name}</Text>
        <Text style={styles.varType}>{variable.type}</Text>
        <Text style={styles.varValue} numberOfLines={1}>{variable.value}</Text>
      </TouchableOpacity>
      {expanded && variable.children?.map((child, i) => (
        <VariableRow key={i} variable={child} depth={depth + 1} />
      ))}
    </View>
  );
}

export default function DebuggerScreen() {
  const { breakpoints, activeProject } = useApp();
  const [isDebugging, setIsDebugging] = useState(false);
  const [isPaused, setIsPaused] = useState(false);

  const start = () => { setIsDebugging(true); setIsPaused(true); };
  const stop = () => { setIsDebugging(false); setIsPaused(false); };

  return (
    <View
      style={[
        styles.container,
        { paddingBottom: Platform.OS === "web" ? 34 : 0 },
      ]}
    >
      <View style={styles.toolbar}>
        <Text style={styles.toolbarTitle}>{activeProject?.name ?? "No Project"}</Text>
        <View style={styles.debugBtns}>
          {!isDebugging ? (
            <TouchableOpacity style={styles.startBtn} onPress={start} activeOpacity={0.8}>
              <Feather name="play" size={14} color={colors.light.primaryForeground} />
              <Text style={styles.startBtnText}>Debug</Text>
            </TouchableOpacity>
          ) : (
            <>
              {isPaused && (
                <>
                  <TouchableOpacity style={styles.debugBtn} onPress={() => {}} activeOpacity={0.8}>
                    <Feather name="skip-forward" size={16} color={colors.light.foreground} />
                  </TouchableOpacity>
                  <TouchableOpacity style={styles.debugBtn} onPress={() => {}} activeOpacity={0.8}>
                    <Feather name="corner-down-right" size={16} color={colors.light.foreground} />
                  </TouchableOpacity>
                  <TouchableOpacity style={styles.debugBtn} onPress={() => {}} activeOpacity={0.8}>
                    <Feather name="corner-up-left" size={16} color={colors.light.foreground} />
                  </TouchableOpacity>
                  <TouchableOpacity style={styles.resumeBtn} onPress={() => setIsPaused(false)} activeOpacity={0.8}>
                    <Feather name="play" size={14} color={colors.light.success} />
                  </TouchableOpacity>
                </>
              )}
              <TouchableOpacity style={styles.stopBtn} onPress={stop} activeOpacity={0.8}>
                <Feather name="square" size={14} color={colors.light.destructive} />
              </TouchableOpacity>
            </>
          )}
        </View>
      </View>

      {!isDebugging ? (
        <View style={styles.idleView}>
          <Feather name="crosshair" size={48} color={colors.light.mutedForeground} />
          <Text style={styles.idleTitle}>Debugger Ready</Text>
          <Text style={styles.idleSub}>Press Debug to start a debug session</Text>
          <View style={styles.bpList}>
            <Text style={styles.bpTitle}>
              Breakpoints ({breakpoints.length})
            </Text>
            {breakpoints.length === 0 && (
              <Text style={styles.bpEmpty}>
                No breakpoints set. Click line numbers in the editor.
              </Text>
            )}
            {breakpoints.map((bp, i) => (
              <View key={i} style={styles.bpRow}>
                <Feather name="circle" size={12} color={colors.light.destructive} />
                <Text style={styles.bpFile}>{bp.file}</Text>
                <Text style={styles.bpLine}>:{bp.line}</Text>
              </View>
            ))}
          </View>
        </View>
      ) : (
        <View style={styles.debugView}>
          <View style={styles.panel}>
            <Text style={styles.panelTitle}>Call Stack</Text>
            <ScrollView showsVerticalScrollIndicator={false}>
              {DEMO_FRAMES.map((frame) => (
                <TouchableOpacity
                  key={frame.id}
                  style={[styles.frameRow, frame.active && styles.frameActive]}
                  activeOpacity={0.7}
                >
                  {frame.active && (
                    <Feather name="arrow-right" size={12} color={colors.light.primary} style={{ marginRight: 4 }} />
                  )}
                  <View>
                    <Text style={[styles.frameMethod, frame.active && { color: colors.light.primary }]}>
                      {frame.method}
                    </Text>
                    <Text style={styles.frameFile}>{frame.file}:{frame.line}</Text>
                  </View>
                </TouchableOpacity>
              ))}
            </ScrollView>
          </View>

          <View style={styles.divider} />

          <View style={styles.panel}>
            <Text style={styles.panelTitle}>Variables</Text>
            <ScrollView showsVerticalScrollIndicator={false}>
              {DEMO_VARS.map((v, i) => (
                <VariableRow key={i} variable={v} />
              ))}
            </ScrollView>
          </View>
        </View>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: colors.light.background },
  toolbar: {
    flexDirection: "row",
    alignItems: "center",
    backgroundColor: colors.light.headerBg,
    paddingHorizontal: 12,
    paddingVertical: 8,
    borderBottomWidth: 1,
    borderBottomColor: colors.light.border,
    gap: 8,
  },
  toolbarTitle: { flex: 1, color: colors.light.foreground, fontSize: 14, fontWeight: "600" },
  debugBtns: { flexDirection: "row", gap: 6 },
  startBtn: {
    flexDirection: "row",
    alignItems: "center",
    backgroundColor: colors.light.primary,
    paddingHorizontal: 12,
    paddingVertical: 6,
    borderRadius: 7,
    gap: 5,
  },
  startBtnText: { color: colors.light.primaryForeground, fontWeight: "600", fontSize: 13 },
  debugBtn: {
    padding: 6,
    backgroundColor: colors.light.surface,
    borderRadius: 6,
    borderWidth: 1,
    borderColor: colors.light.border,
  },
  resumeBtn: {
    padding: 6,
    backgroundColor: colors.light.surface,
    borderRadius: 6,
    borderWidth: 1,
    borderColor: colors.light.success + "66",
  },
  stopBtn: {
    padding: 6,
    backgroundColor: colors.light.surface,
    borderRadius: 6,
    borderWidth: 1,
    borderColor: colors.light.destructive + "66",
  },
  idleView: {
    flex: 1,
    alignItems: "center",
    justifyContent: "center",
    gap: 8,
    padding: 20,
  },
  idleTitle: { color: colors.light.foreground, fontSize: 18, fontWeight: "600" },
  idleSub: { color: colors.light.mutedForeground, fontSize: 13 },
  bpList: {
    width: "100%",
    marginTop: 20,
    backgroundColor: colors.light.card,
    borderRadius: 10,
    padding: 12,
    borderWidth: 1,
    borderColor: colors.light.border,
  },
  bpTitle: {
    color: colors.light.mutedForeground,
    fontSize: 11,
    fontWeight: "600",
    textTransform: "uppercase",
    letterSpacing: 0.6,
    marginBottom: 8,
  },
  bpEmpty: { color: colors.light.mutedForeground, fontSize: 13 },
  bpRow: { flexDirection: "row", alignItems: "center", gap: 6, paddingVertical: 3 },
  bpFile: { color: colors.light.foreground, fontSize: 12, fontFamily: "monospace" },
  bpLine: { color: colors.light.mutedForeground, fontSize: 12, fontFamily: "monospace" },
  debugView: { flex: 1 },
  panel: { flex: 1, padding: 4 },
  panelTitle: {
    color: colors.light.mutedForeground,
    fontSize: 11,
    fontWeight: "600",
    textTransform: "uppercase",
    letterSpacing: 0.6,
    paddingHorizontal: 8,
    paddingVertical: 6,
    borderBottomWidth: 1,
    borderBottomColor: colors.light.border,
  },
  divider: { height: 1, backgroundColor: colors.light.border },
  frameRow: {
    flexDirection: "row",
    alignItems: "flex-start",
    paddingHorizontal: 12,
    paddingVertical: 6,
    gap: 4,
  },
  frameActive: { backgroundColor: colors.light.surfaceHighlight },
  frameMethod: { color: colors.light.foreground, fontSize: 12, fontFamily: "monospace" },
  frameFile: { color: colors.light.mutedForeground, fontSize: 11, fontFamily: "monospace" },
  varRow: {
    flexDirection: "row",
    alignItems: "center",
    paddingVertical: 5,
    borderBottomWidth: 1,
    borderBottomColor: colors.light.border + "44",
    gap: 4,
  },
  varName: { color: colors.light.codeFunction, fontSize: 12, fontFamily: "monospace", width: 90 },
  varType: { color: colors.light.codeType, fontSize: 11, fontFamily: "monospace", width: 80 },
  varValue: { flex: 1, color: colors.light.codeString, fontSize: 12, fontFamily: "monospace" },
});
