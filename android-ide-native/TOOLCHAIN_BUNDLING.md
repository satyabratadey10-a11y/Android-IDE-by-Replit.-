# Android IDE Native — Toolchain Binary Bundling Guide

This document explains how to bundle the real Android build tool binaries into
the IDE APK so they can be executed on-device.

---

## Directory Layout

All binaries go under `app/src/main/assets/`:

```
assets/
├── toolchain/
│   ├── arm64-v8a/          ← Pre-built ARM64 binaries
│   │   ├── aapt2
│   │   ├── d8
│   │   ├── zipalign
│   │   ├── aidl
│   │   ├── dexdump
│   │   ├── adb
│   │   ├── clang
│   │   ├── clang++
│   │   ├── lld
│   │   ├── llvm-ar
│   │   ├── llvm-strip
│   │   ├── llvm-objdump
│   │   ├── java
│   │   ├── javac
│   │   ├── jar
│   │   └── keytool
│   └── jars/
│       ├── r8.jar
│       ├── d8.jar          (optional, we prefer the native d8)
│       ├── bundletool.jar
│       ├── apksigner.jar
│       ├── kotlin-compiler.jar
│       └── kotlin-stdlib.jar
└── sdk/
    └── android.jar         ← e.g. platforms/android-34/android.jar
```

---

## Where to Get Each Binary

### From the Android SDK (`$ANDROID_HOME`)

| Binary     | Source path                                     |
|------------|-------------------------------------------------|
| `aapt2`    | `build-tools/34.0.0/aapt2`                     |
| `d8`       | `build-tools/34.0.0/d8`                         |
| `zipalign` | `build-tools/34.0.0/zipalign`                   |
| `aidl`     | `build-tools/34.0.0/aidl`                       |
| `dexdump`  | `build-tools/34.0.0/dexdump`                   |
| `adb`      | `platform-tools/adb`                            |
| `r8.jar`   | `build-tools/34.0.0/lib/r8.jar`                |
| `apksigner.jar` | `build-tools/34.0.0/lib/apksigner.jar`     |
| `android.jar`   | `platforms/android-34/android.jar`         |

> **Note:** The binaries in `build-tools/` are typically x86-64 Linux ELF
> binaries from the SDK manager. You **cannot** run these directly on ARM64
> Android. You must cross-compile them for `aarch64-linux-android`.

### Cross-Compiling for ARM64

The recommended approach is to use the pre-built binaries from the
[Termux project](https://github.com/termux/termux-packages), which provides
ARM64 versions of many tools:

```bash
# From an aarch64 Android device with Termux:
pkg install aapt2 gradle openjdk-17 llvm clang

# Then pull the binaries to your dev machine:
adb shell find /data/data/com.termux/files/usr/bin -name "aapt2" -o -name "adb" ...
adb pull <path>
```

Alternatively, cross-compile from source using the Android NDK:

```bash
# Example: cross-compile aapt2 for arm64
cmake \
  -DCMAKE_TOOLCHAIN_FILE=$NDK/build/cmake/android.toolchain.cmake \
  -DANDROID_ABI=arm64-v8a \
  -DANDROID_PLATFORM=android-26 \
  ...
```

### From the Android NDK (`$NDK`)

| Binary       | Source path                                                      |
|--------------|------------------------------------------------------------------|
| `clang`      | `toolchains/llvm/prebuilt/linux-x86_64/bin/aarch64-linux-android34-clang` |
| `clang++`    | `toolchains/llvm/prebuilt/linux-x86_64/bin/aarch64-linux-android34-clang++` |
| `lld`        | `toolchains/llvm/prebuilt/linux-x86_64/bin/lld`                |
| `llvm-ar`    | `toolchains/llvm/prebuilt/linux-x86_64/bin/llvm-ar`            |
| `llvm-strip` | `toolchains/llvm/prebuilt/linux-x86_64/bin/llvm-strip`         |
| `llvm-objdump` | `toolchains/llvm/prebuilt/linux-x86_64/bin/llvm-objdump`     |

> The NDK toolchain binaries are x86-64 ELF. For on-device execution,
> copy the `aarch64-linux-android34-clang` **wrapper** which is a shell
> script, or use the [prebuilt NDK ARM64 builds from LLVM releases](https://github.com/llvm/llvm-project/releases).

### Java / JDK

For on-device Java execution, use a self-contained JVM:

- **GraalVM native-image** compiled for ARM64 (smallest, fastest startup)
- **OpenJDK for Android** from Termux (`pkg install openjdk-17`)
- **Zing/Azul** mobile JVM

Bundle `java`, `javac`, `jar`, `keytool` from the chosen JDK.

### bundletool

Download the pre-built JAR from:
https://github.com/google/bundletool/releases

This is a pure-Java JAR — works with any bundled JVM.

### Kotlin compiler

Download `kotlin-compiler-*.zip` from:
https://github.com/JetBrains/kotlin/releases

Extract `kotlinc/lib/kotlin-compiler.jar` and `kotlinc/lib/kotlin-stdlib.jar`.

---

## Keeping Binaries Uncompressed

In `app/build.gradle.kts` the `packaging` block keeps all assets stored
uncompressed in the APK so they can be directly read (or mmap'd):

```kotlin
packaging {
    resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" }
}
```

For the native .so files, `extractNativeLibs="true"` in `AndroidManifest.xml`
ensures they are extracted to the filesystem on install.

---

## Extraction Flow (Runtime)

On first launch, `ToolchainManager.setup()`:
1. Reads each binary from `assets/toolchain/arm64-v8a/`
2. Copies it to `Context.filesDir/toolchain/bin/`
3. Calls `chmod +x` via `native_chmod_exec()` in the JNI layer
4. Sets the toolchain path for all native tool wrappers

Subsequent launches skip extraction if the files already exist.

---

## Security Considerations

- All extracted binaries live in the app's **private** data directory
  (`/data/data/com.androidide/files/`), which is not accessible to other apps.
- The IDE does not run as root. It can only invoke tools that work as a normal
  Android app process.
- Tool binaries should be verified with a SHA-256 checksum file bundled in
  `assets/toolchain/checksums.txt`.

---

## Minimum Android Version

- **minSdk 26** (Android 8.0) — required for `execv()` from app process
  without SELinux restrictions that block `fork()` from apps.
- Some features (USB debugging, ADB) require additional permissions granted
  by the device manufacturer.

---

## Testing the Toolchain

Once bundled, use the IDE's terminal to verify:

```
$ aapt2 version
$ d8 --version
$ adb version
$ java -version
$ clang --version
```

All should print version strings, not "Permission denied" or "not found".
