import { Feather } from "@expo/vector-icons";
import { router } from "expo-router";
import React from "react";
import {
  FlatList,
  Platform,
  StyleSheet,
  Text,
  TouchableOpacity,
  View,
} from "react-native";
import { useSafeAreaInsets } from "react-native-safe-area-context";
import colors from "@/constants/colors";
import { useApp } from "@/context/AppContext";
import { Project } from "@/context/AppContext";

function ProjectCard({
  project,
  onOpen,
  onDelete,
}: {
  project: Project;
  onOpen: () => void;
  onDelete: () => void;
}) {
  const ago = Math.floor((Date.now() - project.createdAt) / 60000);
  const agoStr = ago < 1 ? "Just now" : ago < 60 ? `${ago}m ago` : `${Math.floor(ago / 60)}h ago`;

  return (
    <TouchableOpacity onPress={onOpen} style={styles.card} activeOpacity={0.8}>
      <View style={styles.cardIcon}>
        <Feather name="smartphone" size={22} color={colors.light.primary} />
      </View>
      <View style={styles.cardInfo}>
        <Text style={styles.cardName}>{project.name}</Text>
        <Text style={styles.cardPkg}>{project.packageName}</Text>
        <View style={styles.cardMeta}>
          <View style={styles.badge}>
            <Text style={styles.badgeText}>SDK {project.minSdk}+</Text>
          </View>
          <View style={[styles.badge, styles.variantBadge]}>
            <Text style={styles.badgeText}>{project.buildVariant}</Text>
          </View>
          <Text style={styles.cardAgo}>{agoStr}</Text>
        </View>
      </View>
      <TouchableOpacity onPress={onDelete} style={styles.cardDelete} hitSlop={8}>
        <Feather name="trash-2" size={16} color={colors.light.mutedForeground} />
      </TouchableOpacity>
    </TouchableOpacity>
  );
}

export default function HomeScreen() {
  const insets = useSafeAreaInsets();
  const { projects, setActiveProject, deleteProject, createProject } = useApp();

  const handleOpen = (project: Project) => {
    setActiveProject(project);
    router.push("/(editor)/editor");
  };

  const handleCreate = () => {
    const name = `Project${projects.length + 1}`;
    createProject(name, `com.example.${name.toLowerCase()}`);
    router.push("/(editor)/editor");
  };

  return (
    <View
      style={[
        styles.container,
        {
          paddingTop: Platform.OS === "web" ? 67 : 0,
          paddingBottom: Platform.OS === "web" ? 34 : 0,
        },
      ]}
    >
      <View style={styles.header}>
        <View>
          <Text style={styles.headerTitle}>Android IDE</Text>
          <Text style={styles.headerSub}>{projects.length} project{projects.length !== 1 ? "s" : ""}</Text>
        </View>
        <TouchableOpacity onPress={handleCreate} style={styles.newBtn} activeOpacity={0.8}>
          <Feather name="plus" size={18} color={colors.light.primaryForeground} />
          <Text style={styles.newBtnText}>New</Text>
        </TouchableOpacity>
      </View>

      <View style={styles.quickActions}>
        <TouchableOpacity
          style={styles.quickBtn}
          onPress={() => router.push("/(editor)/terminal")}
          activeOpacity={0.8}
        >
          <Feather name="terminal" size={20} color={colors.light.primary} />
          <Text style={styles.quickLabel}>Terminal</Text>
        </TouchableOpacity>
        <TouchableOpacity
          style={styles.quickBtn}
          onPress={() => router.push("/(editor)/devices")}
          activeOpacity={0.8}
        >
          <Feather name="smartphone" size={20} color={colors.light.accent} />
          <Text style={styles.quickLabel}>Devices</Text>
        </TouchableOpacity>
        <TouchableOpacity
          style={styles.quickBtn}
          onPress={() => router.push("/(editor)/logcat")}
          activeOpacity={0.8}
        >
          <Feather name="activity" size={20} color={colors.light.warning} />
          <Text style={styles.quickLabel}>Logcat</Text>
        </TouchableOpacity>
        <TouchableOpacity
          style={styles.quickBtn}
          onPress={() => router.push("/(editor)/settings")}
          activeOpacity={0.8}
        >
          <Feather name="settings" size={20} color={colors.light.mutedForeground} />
          <Text style={styles.quickLabel}>Settings</Text>
        </TouchableOpacity>
      </View>

      <Text style={styles.sectionTitle}>Recent Projects</Text>
      <FlatList
        data={projects}
        keyExtractor={(p) => p.id}
        renderItem={({ item }) => (
          <ProjectCard
            project={item}
            onOpen={() => handleOpen(item)}
            onDelete={() => deleteProject(item.id)}
          />
        )}
        contentContainerStyle={styles.list}
        showsVerticalScrollIndicator={false}
        ListEmptyComponent={
          <View style={styles.empty}>
            <Feather name="package" size={40} color={colors.light.mutedForeground} />
            <Text style={styles.emptyText}>No projects yet</Text>
            <TouchableOpacity style={styles.createBtn} onPress={handleCreate} activeOpacity={0.8}>
              <Text style={styles.createBtnText}>Create your first project</Text>
            </TouchableOpacity>
          </View>
        }
      />
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: colors.light.background },
  header: {
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
    paddingHorizontal: 16,
    paddingTop: 16,
    paddingBottom: 12,
    borderBottomWidth: 1,
    borderBottomColor: colors.light.border,
  },
  headerTitle: {
    color: colors.light.foreground,
    fontSize: 22,
    fontWeight: "700",
  },
  headerSub: {
    color: colors.light.mutedForeground,
    fontSize: 12,
    marginTop: 2,
  },
  newBtn: {
    flexDirection: "row",
    alignItems: "center",
    backgroundColor: colors.light.primary,
    paddingHorizontal: 14,
    paddingVertical: 8,
    borderRadius: 8,
    gap: 6,
  },
  newBtnText: {
    color: colors.light.primaryForeground,
    fontWeight: "600",
    fontSize: 14,
  },
  quickActions: {
    flexDirection: "row",
    paddingHorizontal: 12,
    paddingVertical: 12,
    gap: 8,
  },
  quickBtn: {
    flex: 1,
    alignItems: "center",
    justifyContent: "center",
    backgroundColor: colors.light.card,
    borderRadius: 10,
    paddingVertical: 12,
    gap: 6,
    borderWidth: 1,
    borderColor: colors.light.border,
  },
  quickLabel: {
    color: colors.light.mutedForeground,
    fontSize: 11,
    fontWeight: "500",
  },
  sectionTitle: {
    color: colors.light.mutedForeground,
    fontSize: 12,
    fontWeight: "600",
    textTransform: "uppercase",
    letterSpacing: 0.8,
    paddingHorizontal: 16,
    paddingBottom: 8,
  },
  list: { paddingHorizontal: 12, paddingBottom: 40 },
  card: {
    flexDirection: "row",
    alignItems: "center",
    backgroundColor: colors.light.card,
    borderRadius: 12,
    padding: 14,
    marginBottom: 10,
    borderWidth: 1,
    borderColor: colors.light.border,
  },
  cardIcon: {
    width: 44,
    height: 44,
    borderRadius: 10,
    backgroundColor: colors.light.surface,
    alignItems: "center",
    justifyContent: "center",
    marginRight: 12,
  },
  cardInfo: { flex: 1 },
  cardName: { color: colors.light.foreground, fontSize: 15, fontWeight: "600" },
  cardPkg: { color: colors.light.mutedForeground, fontSize: 11, marginTop: 2 },
  cardMeta: {
    flexDirection: "row",
    alignItems: "center",
    marginTop: 6,
    gap: 6,
  },
  badge: {
    backgroundColor: colors.light.surface,
    paddingHorizontal: 6,
    paddingVertical: 2,
    borderRadius: 4,
    borderWidth: 1,
    borderColor: colors.light.border,
  },
  variantBadge: {
    borderColor: colors.light.primary + "66",
  },
  badgeText: {
    color: colors.light.mutedForeground,
    fontSize: 10,
    fontWeight: "500",
  },
  cardAgo: {
    color: colors.light.mutedForeground,
    fontSize: 11,
    marginLeft: "auto",
  },
  cardDelete: { padding: 6 },
  empty: {
    alignItems: "center",
    paddingTop: 60,
    gap: 12,
  },
  emptyText: { color: colors.light.mutedForeground, fontSize: 16 },
  createBtn: {
    backgroundColor: colors.light.primary,
    paddingHorizontal: 20,
    paddingVertical: 10,
    borderRadius: 8,
    marginTop: 8,
  },
  createBtnText: {
    color: colors.light.primaryForeground,
    fontWeight: "600",
    fontSize: 14,
  },
});
