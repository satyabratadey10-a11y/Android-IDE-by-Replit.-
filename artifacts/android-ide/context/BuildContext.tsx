import React, { createContext, useCallback, useContext, useRef, useState } from "react";
import { useApp } from "./AppContext";

export interface BuildStep {
  name: string;
  tool: string;
  status: "pending" | "running" | "done" | "error";
  duration?: number;
}

interface BuildContextType {
  buildSteps: BuildStep[];
  currentStep: number;
  runBuild: () => Promise<void>;
  runClean: () => Promise<void>;
  cancelBuild: () => void;
}

const BuildContext = createContext<BuildContextType | null>(null);

const BUILD_PIPELINE: Omit<BuildStep, "status">[] = [
  { name: "Compile AIDL interfaces", tool: "aidl" },
  { name: "Compile Kotlin sources", tool: "kotlinc" },
  { name: "Compile Java sources", tool: "javac" },
  { name: "Process resources", tool: "aapt2" },
  { name: "Link resources", tool: "aapt2 link" },
  { name: "Generate R.java", tool: "aapt2" },
  { name: "Convert to DEX", tool: "d8" },
  { name: "Merge DEX files", tool: "dexmerge" },
  { name: "Optimize with R8/ProGuard", tool: "r8" },
  { name: "Package APK", tool: "apkbuilder" },
  { name: "Align APK", tool: "zipalign" },
  { name: "Sign APK", tool: "apksigner" },
  { name: "Verify APK signature", tool: "apksigner verify" },
];

function sleep(ms: number) {
  return new Promise<void>((r) => setTimeout(r, ms));
}

export function BuildProvider({ children }: { children: React.ReactNode }) {
  const { addBuildLog, clearBuildLogs, setIsBuilding, activeProject } = useApp();
  const [buildSteps, setBuildSteps] = useState<BuildStep[]>([]);
  const [currentStep, setCurrentStep] = useState(-1);
  const cancelRef = useRef(false);

  const cancelBuild = useCallback(() => {
    cancelRef.current = true;
  }, []);

  const runBuild = useCallback(async () => {
    if (!activeProject) return;
    cancelRef.current = false;
    clearBuildLogs();
    setIsBuilding(true);

    const steps: BuildStep[] = BUILD_PIPELINE.map((s) => ({
      ...s,
      status: "pending",
    }));
    setBuildSteps(steps);
    setCurrentStep(0);

    const buildStart = Date.now();

    addBuildLog({ type: "info", tool: "gradle", message: `> Task :app:preBuild UP-TO-DATE` });
    addBuildLog({
      type: "info",
      tool: "gradle",
      message: `Starting build: ${activeProject.name} [${activeProject.buildVariant}]`,
    });

    for (let i = 0; i < steps.length; i++) {
      if (cancelRef.current) {
        addBuildLog({ type: "warning", tool: "gradle", message: "BUILD CANCELLED by user" });
        break;
      }

      const step = steps[i];
      setCurrentStep(i);
      setBuildSteps((prev) =>
        prev.map((s, idx) => (idx === i ? { ...s, status: "running" } : s))
      );

      addBuildLog({
        type: "info",
        tool: step.tool,
        message: `> Task :app:${step.name.replace(/\s/g, "")}`,
      });

      const duration = 200 + Math.random() * 800;
      await sleep(duration);

      if (cancelRef.current) break;

      const hasWarning = Math.random() < 0.15;
      if (hasWarning) {
        addBuildLog({
          type: "warning",
          tool: step.tool,
          message: `w: ${activeProject.name}: unused variable found in ${step.name}`,
        });
      }

      setBuildSteps((prev) =>
        prev.map((s, idx) =>
          idx === i ? { ...s, status: "done", duration: Math.round(duration) } : s
        )
      );

      addBuildLog({
        type: "verbose",
        tool: step.tool,
        message: `  ✓ ${step.name} completed in ${Math.round(duration)}ms`,
      });
    }

    const total = Date.now() - buildStart;

    if (!cancelRef.current) {
      addBuildLog({ type: "success", tool: "gradle", message: "" });
      addBuildLog({
        type: "success",
        tool: "gradle",
        message: `BUILD SUCCESSFUL in ${(total / 1000).toFixed(1)}s`,
      });
      addBuildLog({
        type: "info",
        tool: "gradle",
        message: `APK saved to: app/build/outputs/apk/${activeProject.buildVariant}/app-${activeProject.buildVariant}.apk`,
      });
    }

    setIsBuilding(false);
    setCurrentStep(-1);
  }, [activeProject, addBuildLog, clearBuildLogs, setIsBuilding]);

  const runClean = useCallback(async () => {
    clearBuildLogs();
    setIsBuilding(true);
    addBuildLog({ type: "info", tool: "gradle", message: "> Task :app:clean" });
    await sleep(300);
    addBuildLog({ type: "info", tool: "gradle", message: "> Task :clean" });
    await sleep(200);
    addBuildLog({ type: "success", tool: "gradle", message: "BUILD SUCCESSFUL in 0.5s" });
    setBuildSteps([]);
    setIsBuilding(false);
  }, [addBuildLog, clearBuildLogs, setIsBuilding]);

  return (
    <BuildContext.Provider value={{ buildSteps, currentStep, runBuild, runClean, cancelBuild }}>
      {children}
    </BuildContext.Provider>
  );
}

export function useBuild(): BuildContextType {
  const ctx = useContext(BuildContext);
  if (!ctx) throw new Error("useBuild must be used within BuildProvider");
  return ctx;
}
