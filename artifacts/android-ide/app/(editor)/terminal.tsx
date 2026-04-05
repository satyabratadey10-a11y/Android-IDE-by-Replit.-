import React, { Platform } from "react";
import { StyleSheet, View } from "react-native";
import TerminalView from "@/components/TerminalView";
import colors from "@/constants/colors";

export default function TerminalScreen() {
  return (
    <View
      style={[
        styles.container,
        { paddingBottom: Platform.OS === "web" ? 34 : 0 },
      ]}
    >
      <TerminalView />
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: colors.light.terminalBg },
});
