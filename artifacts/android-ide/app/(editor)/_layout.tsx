import { Stack } from "expo-router";
import React from "react";
import colors from "@/constants/colors";

export default function EditorLayout() {
  return (
    <Stack
      screenOptions={{
        headerStyle: { backgroundColor: colors.light.headerBg },
        headerTintColor: colors.light.foreground,
        headerTitleStyle: { fontWeight: "600", fontSize: 15 },
        headerShadowVisible: false,
        headerBackTitle: "Back",
        contentStyle: { backgroundColor: colors.light.background },
      }}
    >
      <Stack.Screen name="editor" options={{ headerShown: false }} />
      <Stack.Screen name="terminal" options={{ title: "Terminal", headerShown: true }} />
      <Stack.Screen name="build" options={{ title: "Build Console", headerShown: true }} />
      <Stack.Screen name="debugger" options={{ title: "Debugger", headerShown: true }} />
      <Stack.Screen name="logcat" options={{ title: "Logcat", headerShown: true }} />
      <Stack.Screen name="devices" options={{ title: "Device Manager", headerShown: true }} />
      <Stack.Screen name="package" options={{ title: "Package APK", headerShown: true }} />
      <Stack.Screen name="settings" options={{ title: "Settings", headerShown: true }} />
    </Stack>
  );
}
