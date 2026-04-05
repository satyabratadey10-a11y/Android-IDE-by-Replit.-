import { Feather } from "@expo/vector-icons";
import React, { useCallback, useEffect, useRef, useState } from "react";
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

interface TerminalLine {
  id: string;
  type: "input" | "output" | "error" | "info";
  text: string;
}

function generateId() {
  return Date.now().toString() + Math.random().toString(36).substr(2, 9);
}

const COMMANDS: Record<string, (args: string[], projectName?: string) => string[]> = {
  help: () => [
    "Available commands:",
    "  build         Run full Android build pipeline",
    "  clean         Clean build artifacts",
    "  aapt2 dump    Dump APK resources",
    "  d8            Convert class files to DEX",
    "  r8            Run R8/ProGuard optimizer",
    "  zipalign      Align APK for memory mapping",
    "  apksigner     Sign APK with debug keystore",
    "  javac         Compile Java sources",
    "  kotlinc       Compile Kotlin sources",
    "  adb devices   List connected devices",
    "  adb install   Install APK on device",
    "  logcat        View device logs",
    "  lint          Run Android lint checks",
    "  ls            List files in current directory",
    "  pwd           Print working directory",
    "  clear         Clear terminal",
    "  version       Show toolchain versions",
  ],
  build: (_, project) => [
    `[Gradle] Configuring project '${project || "MyApp"}'...`,
    "[AAPT2] Processing resources...",
    "[kotlinc] Compiling Kotlin sources...",
    "[d8] Converting to DEX...",
    "[apksigner] Signing APK...",
    "[zipalign] Aligning APK...",
    `BUILD SUCCESSFUL — app/build/outputs/apk/debug/app-debug.apk`,
  ],
  clean: () => [
    "> Task :app:clean",
    "> Task :clean",
    "BUILD SUCCESSFUL in 0.5s",
  ],
  version: () => [
    "Android SDK Build-Tools  34.0.0",
    "AAPT2               34.0.0",
    "D8/R8               8.2.42",
    "Kotlin              1.9.24",
    "Java (JDK)          17.0.8",
    "Gradle              8.7",
    "NDK                 26.3.11579264",
    "LLVM/Clang          17.0.2",
  ],
  ls: () => [
    "app/",
    "build.gradle.kts",
    "settings.gradle.kts",
    "gradle/",
    "gradlew",
    "gradlew.bat",
    "local.properties",
  ],
  pwd: () => ["/workspace/project"],
  "adb devices": () => [
    "List of devices attached",
    "emulator-5554   device",
    "R5CT103ABYZ     device",
  ],
  "adb install": (args) => [
    `Performing Streamed Install`,
    `Success`,
    `Installed: ${args[0] || "app-debug.apk"}`,
  ],
  logcat: () => [
    "--------- beginning of main",
    "05-01 12:00:01.123  1234  1234 I ActivityManager: Start proc for activity ...",
    "05-01 12:00:01.456  2345  2345 D MainActivity: onCreate called",
    "05-01 12:00:01.789  2345  2345 I ViewRootImpl: Displaying Window{...}",
  ],
  lint: () => [
    "Ran to completion normally",
    "0 errors, 3 warnings",
    "  app/src/main/java/com/example/myapp/MainActivity.kt:15: Warning [UnusedResources]",
    "  app/src/main/res/values/strings.xml:3: Warning [TypographyDashes]",
  ],
  aapt2: (args) => {
    if (args[0] === "dump") return ["APK content:", "  AndroidManifest.xml", "  res/layout/activity_main.xml", "  resources.arsc"];
    return ["aapt2: Android Asset Packaging Tool 2", "Usage: aapt2 [compile|link|dump|diff|optimize|convert]"];
  },
  d8: (args) => [
    `d8: dexing ${args[0] || "classes.jar"}...`,
    "Output: classes.dex (234KB)",
    "d8 completed successfully.",
  ],
  r8: () => [
    "R8: Shrinker starting...",
    "R8: Removing unused classes...",
    "R8: Applying ProGuard rules...",
    "R8: Output size reduced by 42%",
    "R8: Completed successfully.",
  ],
  zipalign: () => [
    "zipalign: verifying app-debug.apk...",
    "zipalign: Verification successful.",
  ],
  apksigner: () => [
    "apksigner: Signing app-debug.apk with debug.keystore...",
    "Signed successfully with SHA-256 fingerprint.",
  ],
  javac: () => [
    "javac: Compiling 1 source file...",
    "Build complete. No errors.",
  ],
  kotlinc: () => [
    "kotlinc: Compiling Kotlin sources...",
    "Sources compiled successfully.",
  ],
};

export default function TerminalView() {
  const [lines, setLines] = useState<TerminalLine[]>([
    { id: "1", type: "info", text: "Android IDE Terminal v1.0" },
    { id: "2", type: "info", text: 'Type "help" to see available commands.' },
    { id: "3", type: "info", text: "" },
  ]);
  const [input, setInput] = useState("");
  const [history, setHistory] = useState<string[]>([]);
  const [histIdx, setHistIdx] = useState(-1);
  const scrollRef = useRef<ScrollView>(null);

  useEffect(() => {
    scrollRef.current?.scrollToEnd({ animated: true });
  }, [lines]);

  const runCommand = useCallback((cmd: string) => {
    const trimmed = cmd.trim();
    if (!trimmed) return;

    const newLines: TerminalLine[] = [
      { id: generateId(), type: "input", text: `$ ${trimmed}` },
    ];

    if (trimmed === "clear") {
      setLines([{ id: generateId(), type: "info", text: "" }]);
      setHistory((h) => [trimmed, ...h]);
      setHistIdx(-1);
      return;
    }

    const parts = trimmed.split(" ");
    const cmd0 = parts[0];
    const args = parts.slice(1);
    const fullCmd = parts.slice(0, 2).join(" ");

    const handler =
      COMMANDS[trimmed] ||
      COMMANDS[fullCmd] ||
      COMMANDS[cmd0];

    if (handler) {
      const output = handler(args);
      output.forEach((line) =>
        newLines.push({ id: generateId(), type: "output", text: line })
      );
    } else {
      newLines.push({
        id: generateId(),
        type: "error",
        text: `bash: ${cmd0}: command not found`,
      });
    }

    setLines((prev) => [...prev, ...newLines]);
    setHistory((h) => [trimmed, ...h.slice(0, 49)]);
    setHistIdx(-1);
  }, []);

  const submit = useCallback(() => {
    runCommand(input);
    setInput("");
  }, [input, runCommand]);

  return (
    <View style={styles.container}>
      <ScrollView
        ref={scrollRef}
        style={styles.output}
        contentContainerStyle={styles.outputContent}
        showsVerticalScrollIndicator={false}
      >
        {lines.map((line) => (
          <Text
            key={line.id}
            style={[
              styles.line,
              line.type === "input" && styles.inputLine,
              line.type === "error" && styles.errorLine,
              line.type === "info" && styles.infoLine,
            ]}
          >
            {line.text}
          </Text>
        ))}
      </ScrollView>
      <View style={styles.inputRow}>
        <Text style={styles.prompt}>$ </Text>
        <TextInput
          style={styles.input}
          value={input}
          onChangeText={setInput}
          onSubmitEditing={submit}
          returnKeyType="go"
          autoCapitalize="none"
          autoCorrect={false}
          spellCheck={false}
          placeholderTextColor={colors.light.mutedForeground}
          placeholder="Enter command..."
          selectionColor={colors.light.primary}
        />
        <TouchableOpacity onPress={submit} style={styles.sendBtn} activeOpacity={0.7}>
          <Feather name="corner-down-left" size={16} color={colors.light.primary} />
        </TouchableOpacity>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: colors.light.terminalBg,
  },
  output: {
    flex: 1,
  },
  outputContent: {
    padding: 10,
    paddingBottom: 16,
  },
  line: {
    color: colors.light.terminalText,
    fontFamily: "monospace",
    fontSize: 12,
    lineHeight: 18,
  },
  inputLine: {
    color: "#ffffff",
  },
  errorLine: {
    color: colors.light.buildError,
  },
  infoLine: {
    color: colors.light.buildInfo,
  },
  inputRow: {
    flexDirection: "row",
    alignItems: "center",
    backgroundColor: colors.light.surface,
    borderTopWidth: 1,
    borderTopColor: colors.light.border,
    paddingHorizontal: 12,
    paddingVertical: 8,
  },
  prompt: {
    color: colors.light.primary,
    fontFamily: "monospace",
    fontSize: 14,
    marginRight: 4,
  },
  input: {
    flex: 1,
    color: colors.light.foreground,
    fontFamily: "monospace",
    fontSize: 13,
    padding: 0,
  },
  sendBtn: {
    padding: 4,
    marginLeft: 8,
  },
});
