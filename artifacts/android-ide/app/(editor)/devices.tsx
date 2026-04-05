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

interface Device {
  id: string;
  serial: string;
  name: string;
  model: string;
  androidVersion: string;
  api: number;
  type: "physical" | "emulator";
  status: "online" | "offline" | "unauthorized";
  battery?: number;
}

interface AVD {
  id: string;
  name: string;
  target: string;
  api: number;
  abi: string;
  status: "stopped" | "running" | "starting";
}

const DEMO_DEVICES: Device[] = [
  {
    id: "1",
    serial: "R5CT103ABYZ",
    name: "Galaxy S23",
    model: "SM-S911B",
    androidVersion: "14",
    api: 34,
    type: "physical",
    status: "online",
    battery: 87,
  },
  {
    id: "2",
    serial: "emulator-5554",
    name: "Pixel 8 API 34",
    model: "google_sdk",
    androidVersion: "14",
    api: 34,
    type: "emulator",
    status: "online",
    battery: 100,
  },
];

const DEMO_AVDS: AVD[] = [
  { id: "1", name: "Pixel_8_API_34", target: "Google APIs", api: 34, abi: "x86_64", status: "running" },
  { id: "2", name: "Pixel_7_API_33", target: "Google APIs", api: 33, abi: "x86_64", status: "stopped" },
  { id: "3", name: "Nexus_5X_API_30", target: "Android 11", api: 30, abi: "x86", status: "stopped" },
];

function DeviceCard({ device }: { device: Device }) {
  const [installing, setInstalling] = useState(false);
  const [installed, setInstalled] = useState(false);

  const install = () => {
    setInstalling(true);
    setTimeout(() => {
      setInstalling(false);
      setInstalled(true);
    }, 2000);
  };

  const statusColor =
    device.status === "online"
      ? colors.light.success
      : device.status === "offline"
      ? colors.light.mutedForeground
      : colors.light.warning;

  return (
    <View style={styles.deviceCard}>
      <View style={styles.deviceIcon}>
        <Feather
          name={device.type === "emulator" ? "monitor" : "smartphone"}
          size={22}
          color={device.status === "online" ? colors.light.primary : colors.light.mutedForeground}
        />
      </View>
      <View style={styles.deviceInfo}>
        <View style={styles.deviceNameRow}>
          <Text style={styles.deviceName}>{device.name}</Text>
          <View style={[styles.statusDot, { backgroundColor: statusColor }]} />
          <Text style={[styles.statusText, { color: statusColor }]}>
            {device.status}
          </Text>
        </View>
        <Text style={styles.deviceModel}>{device.model} · Android {device.androidVersion} (API {device.api})</Text>
        <Text style={styles.deviceSerial}>{device.serial}</Text>
        {device.battery !== undefined && (
          <View style={styles.batteryRow}>
            <Feather name="battery-charging" size={12} color={colors.light.mutedForeground} />
            <Text style={styles.batteryText}>{device.battery}%</Text>
          </View>
        )}
      </View>
      {device.status === "online" && (
        <TouchableOpacity
          style={[styles.installBtn, installed && styles.installBtnDone]}
          onPress={install}
          disabled={installing || installed}
          activeOpacity={0.8}
        >
          {installing ? (
            <ActivityIndicator size="small" color={colors.light.primaryForeground} />
          ) : (
            <Feather
              name={installed ? "check" : "upload"}
              size={14}
              color={colors.light.primaryForeground}
            />
          )}
        </TouchableOpacity>
      )}
    </View>
  );
}

function AVDRow({ avd }: { avd: AVD }) {
  const [status, setStatus] = useState(avd.status);

  const toggle = () => {
    if (status === "stopped") {
      setStatus("starting");
      setTimeout(() => setStatus("running"), 2500);
    } else if (status === "running") {
      setStatus("stopped");
    }
  };

  const statusColor =
    status === "running"
      ? colors.light.success
      : status === "starting"
      ? colors.light.warning
      : colors.light.mutedForeground;

  return (
    <View style={styles.avdRow}>
      <Feather name="monitor" size={16} color={statusColor} />
      <View style={styles.avdInfo}>
        <Text style={styles.avdName}>{avd.name}</Text>
        <Text style={styles.avdDetail}>{avd.target} · API {avd.api} · {avd.abi}</Text>
      </View>
      <TouchableOpacity
        style={[
          styles.avdBtn,
          status === "running" ? styles.avdBtnStop : styles.avdBtnStart,
        ]}
        onPress={toggle}
        disabled={status === "starting"}
        activeOpacity={0.8}
      >
        {status === "starting" ? (
          <ActivityIndicator size="small" color={colors.light.warning} />
        ) : (
          <Feather
            name={status === "running" ? "square" : "play"}
            size={13}
            color={status === "running" ? colors.light.destructive : colors.light.success}
          />
        )}
      </TouchableOpacity>
    </View>
  );
}

export default function DevicesScreen() {
  const [tab, setTab] = useState<"devices" | "avd">("devices");

  return (
    <View
      style={[
        styles.container,
        { paddingBottom: Platform.OS === "web" ? 34 : 0 },
      ]}
    >
      <View style={styles.tabs}>
        <TouchableOpacity
          style={[styles.tabBtn, tab === "devices" && styles.tabBtnActive]}
          onPress={() => setTab("devices")}
          activeOpacity={0.7}
        >
          <Text style={[styles.tabText, tab === "devices" && styles.tabTextActive]}>
            Connected Devices
          </Text>
        </TouchableOpacity>
        <TouchableOpacity
          style={[styles.tabBtn, tab === "avd" && styles.tabBtnActive]}
          onPress={() => setTab("avd")}
          activeOpacity={0.7}
        >
          <Text style={[styles.tabText, tab === "avd" && styles.tabTextActive]}>
            Virtual Devices
          </Text>
        </TouchableOpacity>
      </View>

      <ScrollView style={styles.scroll} contentContainerStyle={styles.scrollContent}>
        {tab === "devices" && (
          <>
            <View style={styles.sectionHeader}>
              <Text style={styles.sectionTitle}>ADB Devices</Text>
              <TouchableOpacity style={styles.refreshBtn} activeOpacity={0.8}>
                <Feather name="refresh-cw" size={13} color={colors.light.primary} />
                <Text style={styles.refreshText}>Refresh</Text>
              </TouchableOpacity>
            </View>
            {DEMO_DEVICES.map((d) => (
              <DeviceCard key={d.id} device={d} />
            ))}
            <View style={styles.adbInfo}>
              <Feather name="info" size={14} color={colors.light.accent} />
              <Text style={styles.adbInfoText}>
                Tap the upload icon to install APK to a connected device
              </Text>
            </View>
          </>
        )}

        {tab === "avd" && (
          <>
            <View style={styles.sectionHeader}>
              <Text style={styles.sectionTitle}>AVD Manager</Text>
              <TouchableOpacity style={styles.refreshBtn} activeOpacity={0.8}>
                <Feather name="plus" size={13} color={colors.light.primary} />
                <Text style={styles.refreshText}>New</Text>
              </TouchableOpacity>
            </View>
            {DEMO_AVDS.map((a) => (
              <AVDRow key={a.id} avd={a} />
            ))}
          </>
        )}
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: colors.light.background },
  tabs: {
    flexDirection: "row",
    backgroundColor: colors.light.headerBg,
    borderBottomWidth: 1,
    borderBottomColor: colors.light.border,
  },
  tabBtn: {
    flex: 1,
    paddingVertical: 12,
    alignItems: "center",
    borderBottomWidth: 2,
    borderBottomColor: "transparent",
  },
  tabBtnActive: { borderBottomColor: colors.light.primary },
  tabText: { color: colors.light.mutedForeground, fontSize: 13, fontWeight: "500" },
  tabTextActive: { color: colors.light.primary },
  scroll: { flex: 1 },
  scrollContent: { padding: 12, gap: 8 },
  sectionHeader: {
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
    marginBottom: 8,
  },
  sectionTitle: {
    color: colors.light.mutedForeground,
    fontSize: 11,
    fontWeight: "600",
    textTransform: "uppercase",
    letterSpacing: 0.6,
  },
  refreshBtn: {
    flexDirection: "row",
    alignItems: "center",
    gap: 4,
    backgroundColor: colors.light.surface,
    paddingHorizontal: 10,
    paddingVertical: 4,
    borderRadius: 6,
    borderWidth: 1,
    borderColor: colors.light.border,
  },
  refreshText: { color: colors.light.primary, fontSize: 12, fontWeight: "500" },
  deviceCard: {
    flexDirection: "row",
    alignItems: "center",
    backgroundColor: colors.light.card,
    borderRadius: 12,
    padding: 12,
    borderWidth: 1,
    borderColor: colors.light.border,
    gap: 12,
  },
  deviceIcon: {
    width: 44,
    height: 44,
    borderRadius: 10,
    backgroundColor: colors.light.surface,
    alignItems: "center",
    justifyContent: "center",
  },
  deviceInfo: { flex: 1 },
  deviceNameRow: { flexDirection: "row", alignItems: "center", gap: 6 },
  deviceName: { color: colors.light.foreground, fontSize: 14, fontWeight: "600" },
  statusDot: { width: 7, height: 7, borderRadius: 4 },
  statusText: { fontSize: 11, fontWeight: "600" },
  deviceModel: { color: colors.light.mutedForeground, fontSize: 12, marginTop: 2 },
  deviceSerial: { color: colors.light.mutedForeground, fontSize: 11, fontFamily: "monospace" },
  batteryRow: { flexDirection: "row", alignItems: "center", gap: 4, marginTop: 3 },
  batteryText: { color: colors.light.mutedForeground, fontSize: 11 },
  installBtn: {
    width: 36,
    height: 36,
    borderRadius: 8,
    backgroundColor: colors.light.primary,
    alignItems: "center",
    justifyContent: "center",
  },
  installBtnDone: { backgroundColor: colors.light.success },
  avdRow: {
    flexDirection: "row",
    alignItems: "center",
    backgroundColor: colors.light.card,
    borderRadius: 10,
    padding: 12,
    borderWidth: 1,
    borderColor: colors.light.border,
    gap: 10,
  },
  avdInfo: { flex: 1 },
  avdName: { color: colors.light.foreground, fontSize: 13, fontWeight: "600" },
  avdDetail: { color: colors.light.mutedForeground, fontSize: 11, marginTop: 2 },
  avdBtn: {
    width: 32,
    height: 32,
    borderRadius: 7,
    backgroundColor: colors.light.surface,
    alignItems: "center",
    justifyContent: "center",
    borderWidth: 1,
    borderColor: colors.light.border,
  },
  avdBtnStart: { borderColor: colors.light.success + "66" },
  avdBtnStop: { borderColor: colors.light.destructive + "66" },
  adbInfo: {
    flexDirection: "row",
    alignItems: "flex-start",
    gap: 8,
    backgroundColor: colors.light.card,
    borderRadius: 8,
    padding: 10,
    borderWidth: 1,
    borderColor: colors.light.accent + "44",
    marginTop: 4,
  },
  adbInfoText: { flex: 1, color: colors.light.mutedForeground, fontSize: 12 },
});
