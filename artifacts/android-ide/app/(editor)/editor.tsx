import { Feather } from "@expo/vector-icons";
import { router } from "expo-router";
import React, { useCallback, useRef, useState } from "react";
import {
  Dimensions,
  Modal,
  Platform,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  TouchableOpacity,
  View,
} from "react-native";
import { useSafeAreaInsets } from "react-native-safe-area-context";
import FileTree from "@/components/FileTree";
import SyntaxHighlighter from "@/components/SyntaxHighlighter";
import colors from "@/constants/colors";
import { FileNode, useApp } from "@/context/AppContext";
import { useBuild } from "@/context/BuildContext";

const { width: SCREEN_W } = Dimensions.get("window");
const SIDEBAR_W = 220;

export default function EditorScreen() {
  const insets = useSafeAreaInsets();
  const { activeProject, activeFile, openFiles, openFile, closeFile, updateFileContent, breakpoints, toggleBreakpoint } = useApp();
  const { runBuild, runClean } = useBuild();

  const [sidebarOpen, setSidebarOpen] = useState(false);
  const [editMode, setEditMode] = useState(false);
  const [editContent, setEditContent] = useState("");

  const toggleSidebar = useCallback(() => setSidebarOpen((v) => !v), []);

  const handleSelectFile = useCallback(
    (file: FileNode) => {
      openFile(file);
      setSidebarOpen(false);
    },
    [openFile]
  );

  const startEdit = useCallback(() => {
    if (activeFile?.content !== undefined) {
      setEditContent(activeFile.content);
      setEditMode(true);
    }
  }, [activeFile]);

  const saveEdit = useCallback(() => {
    if (activeFile) {
      updateFileContent(activeFile.id, editContent);
      setEditMode(false);
    }
  }, [activeFile, editContent, updateFileContent]);

  const fileBreakpoints = breakpoints
    .filter((b) => b.file === activeFile?.path)
    .map((b) => b.line);

  return (
    <View
      style={[
        styles.container,
        {
          paddingTop: insets.top + (Platform.OS === "web" ? 27 : 0),
          paddingBottom: Platform.OS === "web" ? 34 : insets.bottom,
        },
      ]}
    >
      <View style={styles.topBar}>
        <TouchableOpacity
          onPress={() => router.back()}
          style={styles.topBarBtn}
          hitSlop={8}
        >
          <Feather name="arrow-left" size={18} color={colors.light.foreground} />
        </TouchableOpacity>
        <TouchableOpacity onPress={toggleSidebar} style={styles.topBarBtn} hitSlop={8}>
          <Feather name="sidebar" size={18} color={sidebarOpen ? colors.light.primary : colors.light.foreground} />
        </TouchableOpacity>
        <Text style={styles.projectName} numberOfLines={1}>
          {activeProject?.name ?? "No Project"}
        </Text>
        <View style={styles.topBarActions}>
          <TouchableOpacity
            style={styles.topBarBtn}
            onPress={() => router.push("/(editor)/build")}
            hitSlop={8}
          >
            <Feather name="play" size={18} color={colors.light.primary} />
          </TouchableOpacity>
          <TouchableOpacity
            style={styles.topBarBtn}
            onPress={() => router.push("/(editor)/terminal")}
            hitSlop={8}
          >
            <Feather name="terminal" size={18} color={colors.light.foreground} />
          </TouchableOpacity>
          <TouchableOpacity
            style={styles.topBarBtn}
            onPress={() => router.push("/(editor)/debugger")}
            hitSlop={8}
          >
            <Feather name="crosshair" size={18} color={colors.light.foreground} />
          </TouchableOpacity>
        </View>
      </View>

      {openFiles.length > 0 && (
        <ScrollView
          horizontal
          style={styles.tabBar}
          showsHorizontalScrollIndicator={false}
          contentContainerStyle={styles.tabBarContent}
        >
          {openFiles.map((f) => (
            <TouchableOpacity
              key={f.id}
              onPress={() => openFile(f)}
              style={[styles.tab, activeFile?.id === f.id && styles.activeTab]}
              activeOpacity={0.7}
            >
              <Text
                style={[styles.tabText, activeFile?.id === f.id && styles.activeTabText]}
                numberOfLines={1}
              >
                {f.name}
              </Text>
              <TouchableOpacity onPress={() => closeFile(f.id)} hitSlop={6}>
                <Feather
                  name="x"
                  size={12}
                  color={
                    activeFile?.id === f.id
                      ? colors.light.foreground
                      : colors.light.mutedForeground
                  }
                />
              </TouchableOpacity>
            </TouchableOpacity>
          ))}
        </ScrollView>
      )}

      <View style={styles.body}>
        {sidebarOpen && (
          <View style={styles.sidebar}>
            <View style={styles.sidebarHeader}>
              <Text style={styles.sidebarTitle}>
                {activeProject?.name ?? "Explorer"}
              </Text>
              <TouchableOpacity
                onPress={() => router.push("/(editor)/settings")}
                hitSlop={8}
              >
                <Feather name="settings" size={14} color={colors.light.mutedForeground} />
              </TouchableOpacity>
            </View>
            {activeProject ? (
              <ScrollView showsVerticalScrollIndicator={false}>
                <FileTree
                  nodes={activeProject.files}
                  onSelectFile={handleSelectFile}
                  activeFileId={activeFile?.id}
                />
              </ScrollView>
            ) : (
              <View style={styles.noProjectMsg}>
                <Text style={styles.noProjectText}>No project open</Text>
              </View>
            )}
          </View>
        )}

        <View style={styles.editorArea}>
          {activeFile ? (
            editMode ? (
              <View style={styles.textEditContainer}>
                <View style={styles.editToolbar}>
                  <Text style={styles.editToolbarName}>{activeFile.name}</Text>
                  <TouchableOpacity onPress={saveEdit} style={styles.saveBtn} activeOpacity={0.8}>
                    <Feather name="save" size={14} color={colors.light.primaryForeground} />
                    <Text style={styles.saveBtnText}>Save</Text>
                  </TouchableOpacity>
                  <TouchableOpacity onPress={() => setEditMode(false)} style={styles.cancelBtn} activeOpacity={0.8}>
                    <Text style={styles.cancelBtnText}>Cancel</Text>
                  </TouchableOpacity>
                </View>
                <TextInput
                  style={styles.textEdit}
                  value={editContent}
                  onChangeText={setEditContent}
                  multiline
                  autoCapitalize="none"
                  autoCorrect={false}
                  spellCheck={false}
                  selectionColor={colors.light.primary}
                />
              </View>
            ) : (
              <View style={styles.codeWrapper}>
                <View style={styles.codeToolbar}>
                  <Text style={styles.codeToolbarName}>{activeFile.name}</Text>
                  <TouchableOpacity onPress={startEdit} style={styles.editBtn} activeOpacity={0.8}>
                    <Feather name="edit-3" size={13} color={colors.light.primary} />
                  </TouchableOpacity>
                  <TouchableOpacity
                    onPress={() => toggleBreakpoint(activeFile.path, 1)}
                    style={styles.editBtn}
                    hitSlop={6}
                    activeOpacity={0.8}
                  >
                    <Feather name="circle" size={13} color={colors.light.destructive} />
                  </TouchableOpacity>
                </View>
                <SyntaxHighlighter
                  code={activeFile.content || ""}
                  language={activeFile.language}
                  showLineNumbers
                  breakpointLines={fileBreakpoints}
                  onToggleBreakpoint={(line) =>
                    toggleBreakpoint(activeFile.path, line)
                  }
                />
              </View>
            )
          ) : (
            <View style={styles.noFile}>
              <Feather name="code" size={48} color={colors.light.mutedForeground} />
              <Text style={styles.noFileText}>No file open</Text>
              <Text style={styles.noFileSub}>
                {activeProject
                  ? "Open a file from the sidebar"
                  : "Open or create a project first"}
              </Text>
              {!sidebarOpen && (
                <TouchableOpacity
                  onPress={toggleSidebar}
                  style={styles.openSidebarBtn}
                  activeOpacity={0.8}
                >
                  <Feather name="sidebar" size={14} color={colors.light.primaryForeground} />
                  <Text style={styles.openSidebarBtnText}>Open Explorer</Text>
                </TouchableOpacity>
              )}
            </View>
          )}
        </View>
      </View>

      <View style={styles.bottomBar}>
        <TouchableOpacity
          style={styles.bottomBarItem}
          onPress={() => router.push("/(editor)/build")}
          activeOpacity={0.8}
        >
          <Feather name="hammer" size={14} color={colors.light.primary} />
          <Text style={styles.bottomBarText}>Build</Text>
        </TouchableOpacity>
        <TouchableOpacity
          style={styles.bottomBarItem}
          onPress={() => runClean()}
          activeOpacity={0.8}
        >
          <Feather name="trash" size={14} color={colors.light.mutedForeground} />
          <Text style={styles.bottomBarText}>Clean</Text>
        </TouchableOpacity>
        <TouchableOpacity
          style={styles.bottomBarItem}
          onPress={() => router.push("/(editor)/package")}
          activeOpacity={0.8}
        >
          <Feather name="package" size={14} color={colors.light.warning} />
          <Text style={styles.bottomBarText}>Package</Text>
        </TouchableOpacity>
        <TouchableOpacity
          style={styles.bottomBarItem}
          onPress={() => router.push("/(editor)/logcat")}
          activeOpacity={0.8}
        >
          <Feather name="activity" size={14} color={colors.light.accent} />
          <Text style={styles.bottomBarText}>Logcat</Text>
        </TouchableOpacity>
        <TouchableOpacity
          style={styles.bottomBarItem}
          onPress={() => router.push("/(editor)/devices")}
          activeOpacity={0.8}
        >
          <Feather name="smartphone" size={14} color={colors.light.success} />
          <Text style={styles.bottomBarText}>Devices</Text>
        </TouchableOpacity>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: colors.light.background },
  topBar: {
    flexDirection: "row",
    alignItems: "center",
    backgroundColor: colors.light.headerBg,
    borderBottomWidth: 1,
    borderBottomColor: colors.light.border,
    paddingHorizontal: 8,
    height: 44,
    gap: 4,
  },
  topBarBtn: {
    padding: 6,
    borderRadius: 6,
  },
  projectName: {
    flex: 1,
    color: colors.light.foreground,
    fontSize: 14,
    fontWeight: "600",
    marginLeft: 2,
  },
  topBarActions: { flexDirection: "row", gap: 2 },
  tabBar: {
    maxHeight: 36,
    backgroundColor: colors.light.surface,
    borderBottomWidth: 1,
    borderBottomColor: colors.light.border,
  },
  tabBarContent: { paddingHorizontal: 4, gap: 2, alignItems: "center" },
  tab: {
    flexDirection: "row",
    alignItems: "center",
    paddingHorizontal: 10,
    height: 30,
    borderRadius: 4,
    gap: 6,
  },
  activeTab: {
    backgroundColor: colors.light.card,
    borderBottomWidth: 2,
    borderBottomColor: colors.light.primary,
  },
  tabText: { color: colors.light.mutedForeground, fontSize: 12 },
  activeTabText: { color: colors.light.foreground, fontWeight: "500" },
  body: { flex: 1, flexDirection: "row" },
  sidebar: {
    width: SIDEBAR_W,
    backgroundColor: colors.light.card,
    borderRightWidth: 1,
    borderRightColor: colors.light.border,
  },
  sidebarHeader: {
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
    paddingHorizontal: 12,
    paddingVertical: 8,
    borderBottomWidth: 1,
    borderBottomColor: colors.light.border,
  },
  sidebarTitle: {
    color: colors.light.mutedForeground,
    fontSize: 11,
    fontWeight: "600",
    textTransform: "uppercase",
    letterSpacing: 0.6,
  },
  noProjectMsg: { padding: 12 },
  noProjectText: { color: colors.light.mutedForeground, fontSize: 13 },
  editorArea: { flex: 1, backgroundColor: colors.light.codeBg },
  noFile: {
    flex: 1,
    alignItems: "center",
    justifyContent: "center",
    gap: 8,
  },
  noFileText: { color: colors.light.mutedForeground, fontSize: 16, fontWeight: "600" },
  noFileSub: { color: colors.light.mutedForeground, fontSize: 13 },
  openSidebarBtn: {
    flexDirection: "row",
    alignItems: "center",
    backgroundColor: colors.light.primary,
    paddingHorizontal: 14,
    paddingVertical: 8,
    borderRadius: 8,
    gap: 6,
    marginTop: 8,
  },
  openSidebarBtnText: { color: colors.light.primaryForeground, fontWeight: "600", fontSize: 13 },
  codeWrapper: { flex: 1 },
  codeToolbar: {
    flexDirection: "row",
    alignItems: "center",
    backgroundColor: colors.light.headerBg,
    paddingHorizontal: 12,
    paddingVertical: 6,
    borderBottomWidth: 1,
    borderBottomColor: colors.light.border,
    gap: 8,
  },
  codeToolbarName: {
    flex: 1,
    color: colors.light.foreground,
    fontSize: 12,
    fontWeight: "500",
  },
  editBtn: { padding: 4 },
  textEditContainer: { flex: 1 },
  editToolbar: {
    flexDirection: "row",
    alignItems: "center",
    backgroundColor: colors.light.headerBg,
    paddingHorizontal: 12,
    paddingVertical: 6,
    borderBottomWidth: 1,
    borderBottomColor: colors.light.border,
    gap: 8,
  },
  editToolbarName: {
    flex: 1,
    color: colors.light.foreground,
    fontSize: 12,
    fontWeight: "500",
  },
  saveBtn: {
    flexDirection: "row",
    alignItems: "center",
    backgroundColor: colors.light.primary,
    paddingHorizontal: 10,
    paddingVertical: 4,
    borderRadius: 6,
    gap: 4,
  },
  saveBtnText: { color: colors.light.primaryForeground, fontSize: 12, fontWeight: "600" },
  cancelBtn: {
    paddingHorizontal: 8,
    paddingVertical: 4,
  },
  cancelBtnText: { color: colors.light.mutedForeground, fontSize: 12 },
  textEdit: {
    flex: 1,
    color: colors.light.foreground,
    fontFamily: "monospace",
    fontSize: 13,
    padding: 12,
    textAlignVertical: "top",
    backgroundColor: colors.light.codeBg,
  },
  bottomBar: {
    flexDirection: "row",
    backgroundColor: colors.light.tabBar,
    borderTopWidth: 1,
    borderTopColor: colors.light.border,
    paddingVertical: 6,
    paddingHorizontal: 4,
  },
  bottomBarItem: {
    flex: 1,
    alignItems: "center",
    justifyContent: "center",
    gap: 3,
    paddingVertical: 4,
  },
  bottomBarText: {
    color: colors.light.mutedForeground,
    fontSize: 9,
    fontWeight: "500",
  },
});
