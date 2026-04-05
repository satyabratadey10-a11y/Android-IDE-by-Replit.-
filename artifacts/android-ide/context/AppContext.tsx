import AsyncStorage from "@react-native-async-storage/async-storage";
import React, {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useState,
} from "react";

export interface FileNode {
  id: string;
  name: string;
  type: "file" | "folder";
  content?: string;
  children?: FileNode[];
  language?: string;
  path: string;
}

export interface Project {
  id: string;
  name: string;
  packageName: string;
  minSdk: number;
  targetSdk: number;
  createdAt: number;
  files: FileNode[];
  buildVariant: "debug" | "release";
}

export interface BuildLog {
  id: string;
  timestamp: number;
  type: "info" | "success" | "error" | "warning" | "verbose";
  message: string;
  tool?: string;
}

export interface BreakPoint {
  file: string;
  line: number;
}

interface AppContextType {
  projects: Project[];
  activeProject: Project | null;
  activeFile: FileNode | null;
  buildLogs: BuildLog[];
  isBuilding: boolean;
  openFiles: FileNode[];
  breakpoints: BreakPoint[];
  setActiveProject: (project: Project | null) => void;
  setActiveFile: (file: FileNode | null) => void;
  createProject: (name: string, packageName: string) => void;
  deleteProject: (id: string) => void;
  updateFileContent: (fileId: string, content: string) => void;
  addBuildLog: (log: Omit<BuildLog, "id" | "timestamp">) => void;
  clearBuildLogs: () => void;
  setIsBuilding: (v: boolean) => void;
  openFile: (file: FileNode) => void;
  closeFile: (fileId: string) => void;
  toggleBreakpoint: (file: string, line: number) => void;
}

const AppContext = createContext<AppContextType | null>(null);

function generateId(): string {
  return Date.now().toString() + Math.random().toString(36).substr(2, 9);
}

function createDefaultProject(name: string, packageName: string): Project {
  const pkg = packageName.replace(/\./g, "/");
  return {
    id: generateId(),
    name,
    packageName,
    minSdk: 24,
    targetSdk: 34,
    createdAt: Date.now(),
    buildVariant: "debug",
    files: [
      {
        id: generateId(),
        name: "app",
        type: "folder",
        path: "app",
        children: [
          {
            id: generateId(),
            name: "src",
            type: "folder",
            path: "app/src",
            children: [
              {
                id: generateId(),
                name: "main",
                type: "folder",
                path: "app/src/main",
                children: [
                  {
                    id: generateId(),
                    name: "java",
                    type: "folder",
                    path: "app/src/main/java",
                    children: [
                      {
                        id: generateId(),
                        name: pkg,
                        type: "folder",
                        path: `app/src/main/java/${pkg}`,
                        children: [
                          {
                            id: generateId(),
                            name: "MainActivity.kt",
                            type: "file",
                            language: "kotlin",
                            path: `app/src/main/java/${pkg}/MainActivity.kt`,
                            content: `package ${packageName}

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import ${packageName}.ui.theme.${name}Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ${name}Theme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Greeting("Android")
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello \$name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    ${name}Theme {
        Greeting("Android")
    }
}`,
                          },
                        ],
                      },
                    ],
                  },
                  {
                    id: generateId(),
                    name: "res",
                    type: "folder",
                    path: "app/src/main/res",
                    children: [
                      {
                        id: generateId(),
                        name: "layout",
                        type: "folder",
                        path: "app/src/main/res/layout",
                        children: [],
                      },
                      {
                        id: generateId(),
                        name: "values",
                        type: "folder",
                        path: "app/src/main/res/values",
                        children: [
                          {
                            id: generateId(),
                            name: "strings.xml",
                            type: "file",
                            language: "xml",
                            path: "app/src/main/res/values/strings.xml",
                            content: `<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">${name}</string>
</resources>`,
                          },
                        ],
                      },
                    ],
                  },
                  {
                    id: generateId(),
                    name: "AndroidManifest.xml",
                    type: "file",
                    language: "xml",
                    path: "app/src/main/AndroidManifest.xml",
                    content: `<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <application
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.${name}">
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:label="@string/app_name"
            android:theme="@style/Theme.${name}">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>

</manifest>`,
                  },
                ],
              },
            ],
          },
          {
            id: generateId(),
            name: "build.gradle.kts",
            type: "file",
            language: "gradle",
            path: "app/build.gradle.kts",
            content: `plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "${packageName}"
    compileSdk = 34

    defaultConfig {
        applicationId = "${packageName}"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}`,
          },
        ],
      },
      {
        id: generateId(),
        name: "build.gradle.kts",
        type: "file",
        language: "gradle",
        path: "build.gradle.kts",
        content: `// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
}`,
      },
      {
        id: generateId(),
        name: "settings.gradle.kts",
        type: "file",
        language: "gradle",
        path: "settings.gradle.kts",
        content: `pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\\\.android.*")
                includeGroupByRegex("com\\\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "${name}"
include(":app")`,
      },
    ],
  };
}

export function AppProvider({ children }: { children: React.ReactNode }) {
  const [projects, setProjects] = useState<Project[]>([]);
  const [activeProject, setActiveProjectState] = useState<Project | null>(null);
  const [activeFile, setActiveFile] = useState<FileNode | null>(null);
  const [buildLogs, setBuildLogs] = useState<BuildLog[]>([]);
  const [isBuilding, setIsBuilding] = useState(false);
  const [openFiles, setOpenFiles] = useState<FileNode[]>([]);
  const [breakpoints, setBreakpoints] = useState<BreakPoint[]>([]);

  useEffect(() => {
    AsyncStorage.getItem("projects").then((data) => {
      if (data) {
        const parsed: Project[] = JSON.parse(data);
        setProjects(parsed);
      } else {
        const demo = createDefaultProject("MyApp", "com.example.myapp");
        setProjects([demo]);
        AsyncStorage.setItem("projects", JSON.stringify([demo]));
      }
    });
  }, []);

  const saveProjects = useCallback((ps: Project[]) => {
    setProjects(ps);
    AsyncStorage.setItem("projects", JSON.stringify(ps));
  }, []);

  const setActiveProject = useCallback(
    (project: Project | null) => {
      setActiveProjectState(project);
      setActiveFile(null);
      setOpenFiles([]);
      if (project) {
        setProjects((prev) =>
          prev.map((p) => (p.id === project.id ? project : p))
        );
      }
    },
    []
  );

  const createProject = useCallback(
    (name: string, packageName: string) => {
      const project = createDefaultProject(name, packageName);
      const updated = [...projects, project];
      saveProjects(updated);
      setActiveProjectState(project);
    },
    [projects, saveProjects]
  );

  const deleteProject = useCallback(
    (id: string) => {
      const updated = projects.filter((p) => p.id !== id);
      saveProjects(updated);
      if (activeProject?.id === id) {
        setActiveProjectState(null);
        setActiveFile(null);
        setOpenFiles([]);
      }
    },
    [projects, saveProjects, activeProject]
  );

  const updateFileContent = useCallback(
    (fileId: string, content: string) => {
      if (!activeProject) return;

      function updateNode(nodes: FileNode[]): FileNode[] {
        return nodes.map((n) => {
          if (n.id === fileId) return { ...n, content };
          if (n.children) return { ...n, children: updateNode(n.children) };
          return n;
        });
      }

      const updated: Project = {
        ...activeProject,
        files: updateNode(activeProject.files),
      };
      setActiveProjectState(updated);
      setProjects((prev) =>
        prev.map((p) => (p.id === activeProject.id ? updated : p))
      );
      AsyncStorage.setItem(
        "projects",
        JSON.stringify(
          projects.map((p) => (p.id === activeProject.id ? updated : p))
        )
      );

      if (activeFile?.id === fileId) {
        setActiveFile({ ...activeFile, content });
      }
      setOpenFiles((prev) =>
        prev.map((f) => (f.id === fileId ? { ...f, content } : f))
      );
    },
    [activeProject, activeFile, projects]
  );

  const addBuildLog = useCallback(
    (log: Omit<BuildLog, "id" | "timestamp">) => {
      const entry: BuildLog = {
        ...log,
        id: generateId(),
        timestamp: Date.now(),
      };
      setBuildLogs((prev) => [...prev, entry]);
    },
    []
  );

  const clearBuildLogs = useCallback(() => setBuildLogs([]), []);

  const openFile = useCallback((file: FileNode) => {
    setActiveFile(file);
    setOpenFiles((prev) => {
      if (prev.find((f) => f.id === file.id)) return prev;
      return [...prev, file];
    });
  }, []);

  const closeFile = useCallback(
    (fileId: string) => {
      setOpenFiles((prev) => {
        const updated = prev.filter((f) => f.id !== fileId);
        if (activeFile?.id === fileId) {
          setActiveFile(updated.length > 0 ? updated[updated.length - 1] : null);
        }
        return updated;
      });
    },
    [activeFile]
  );

  const toggleBreakpoint = useCallback((file: string, line: number) => {
    setBreakpoints((prev) => {
      const exists = prev.find((b) => b.file === file && b.line === line);
      if (exists) return prev.filter((b) => !(b.file === file && b.line === line));
      return [...prev, { file, line }];
    });
  }, []);

  return (
    <AppContext.Provider
      value={{
        projects,
        activeProject,
        activeFile,
        buildLogs,
        isBuilding,
        openFiles,
        breakpoints,
        setActiveProject,
        setActiveFile,
        createProject,
        deleteProject,
        updateFileContent,
        addBuildLog,
        clearBuildLogs,
        setIsBuilding,
        openFile,
        closeFile,
        toggleBreakpoint,
      }}
    >
      {children}
    </AppContext.Provider>
  );
}

export function useApp(): AppContextType {
  const ctx = useContext(AppContext);
  if (!ctx) throw new Error("useApp must be used within AppProvider");
  return ctx;
}
