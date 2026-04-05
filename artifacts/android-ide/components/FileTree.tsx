import { Feather } from "@expo/vector-icons";
import React, { useState } from "react";
import { StyleSheet, Text, TouchableOpacity, View } from "react-native";
import colors from "@/constants/colors";
import { FileNode } from "@/context/AppContext";

interface Props {
  nodes: FileNode[];
  onSelectFile: (file: FileNode) => void;
  activeFileId?: string;
  depth?: number;
}

function getFileIcon(name: string): string {
  if (name.endsWith(".kt")) return "code";
  if (name.endsWith(".java")) return "coffee";
  if (name.endsWith(".xml")) return "file-text";
  if (name.endsWith(".gradle") || name.endsWith(".gradle.kts")) return "settings";
  if (name.endsWith(".cpp") || name.endsWith(".c") || name.endsWith(".h")) return "terminal";
  if (name.endsWith(".json")) return "file";
  if (name.endsWith(".md")) return "book";
  if (name.endsWith(".pro")) return "shield";
  return "file";
}

function getLangColor(name: string): string {
  if (name.endsWith(".kt")) return "#A97BFF";
  if (name.endsWith(".java")) return "#F89820";
  if (name.endsWith(".xml")) return "#58a6ff";
  if (name.endsWith(".gradle") || name.endsWith(".gradle.kts")) return "#3fb950";
  if (name.endsWith(".cpp") || name.endsWith(".c") || name.endsWith(".h")) return "#f78166";
  return colors.light.mutedForeground;
}

function FileTreeNode({
  node,
  onSelectFile,
  activeFileId,
  depth = 0,
}: {
  node: FileNode;
  onSelectFile: (file: FileNode) => void;
  activeFileId?: string;
  depth: number;
}) {
  const [expanded, setExpanded] = useState(depth < 2);
  const isActive = node.id === activeFileId;
  const isFolder = node.type === "folder";

  return (
    <View>
      <TouchableOpacity
        onPress={() => {
          if (isFolder) setExpanded((v) => !v);
          else onSelectFile(node);
        }}
        style={[
          styles.row,
          { paddingLeft: 12 + depth * 14 },
          isActive && styles.activeRow,
        ]}
        activeOpacity={0.7}
      >
        <View style={styles.rowInner}>
          {isFolder ? (
            <Feather
              name={expanded ? "chevron-down" : "chevron-right"}
              size={12}
              color={colors.light.mutedForeground}
              style={{ marginRight: 4 }}
            />
          ) : (
            <View style={{ width: 16 }} />
          )}
          <Feather
            name={isFolder ? (expanded ? "folder-open" : "folder") : (getFileIcon(node.name) as any)}
            size={14}
            color={isFolder ? "#d29922" : getLangColor(node.name)}
            style={{ marginRight: 6 }}
          />
          <Text
            style={[styles.name, isActive && styles.activeName]}
            numberOfLines={1}
          >
            {node.name}
          </Text>
        </View>
      </TouchableOpacity>
      {isFolder && expanded && node.children && (
        <View>
          {node.children.map((child) => (
            <FileTreeNode
              key={child.id}
              node={child}
              onSelectFile={onSelectFile}
              activeFileId={activeFileId}
              depth={depth + 1}
            />
          ))}
        </View>
      )}
    </View>
  );
}

export default function FileTree({ nodes, onSelectFile, activeFileId, depth = 0 }: Props) {
  return (
    <View>
      {nodes.map((node) => (
        <FileTreeNode
          key={node.id}
          node={node}
          onSelectFile={onSelectFile}
          activeFileId={activeFileId}
          depth={depth}
        />
      ))}
    </View>
  );
}

const styles = StyleSheet.create({
  row: {
    height: 28,
    justifyContent: "center",
  },
  rowInner: {
    flexDirection: "row",
    alignItems: "center",
  },
  activeRow: {
    backgroundColor: colors.light.surfaceHighlight,
    borderLeftWidth: 2,
    borderLeftColor: colors.light.primary,
  },
  name: {
    color: colors.light.foreground,
    fontSize: 13,
    flex: 1,
  },
  activeName: {
    color: colors.light.primary,
    fontWeight: "600",
  },
});
