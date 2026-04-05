/**
 * tool_wrapper.cpp
 *
 * High-level wrappers for each Android build tool.
 * Each function constructs the correct argv array and calls native_exec.
 */

#include "tool_wrapper.h"
#include "native_executor.h"
#include <string.h>
#include <stdlib.h>
#include <android/log.h>

#define LOG_TAG "AndroidIDE_Tools"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

static char g_toolchain_dir[1024] = {0};

void tool_set_toolchain_dir(const char* dir) {
    strncpy(g_toolchain_dir, dir, sizeof(g_toolchain_dir) - 1);
}

static std::string tool_path(const char* name) {
    return std::string(g_toolchain_dir) + "/" + name;
}

// ============================================================
// AAPT2 — Android Asset Packaging Tool 2
// ============================================================

int tool_aapt2_compile(
    const char* input_dir,
    const char* output_dir,
    exec_result_t* result
) {
    std::string bin = tool_path("aapt2");
    const char* argv[] = {
        "compile",
        "--dir", input_dir,
        "-o", output_dir
    };
    return native_exec(bin.c_str(), argv, 5, nullptr,
                       result->stdout_buf, sizeof(result->stdout_buf),
                       result->stderr_buf, sizeof(result->stderr_buf));
}

int tool_aapt2_link(
    const char* manifest,
    const char* compiled_res,
    const char* android_jar,
    const char* output_apk,
    exec_result_t* result
) {
    std::string bin = tool_path("aapt2");
    const char* argv[] = {
        "link",
        "--proto-format",
        "-o", output_apk,
        "-I", android_jar,
        "--manifest", manifest,
        "-R", compiled_res,
        "--auto-add-overlay"
    };
    return native_exec(bin.c_str(), argv, 10, nullptr,
                       result->stdout_buf, sizeof(result->stdout_buf),
                       result->stderr_buf, sizeof(result->stderr_buf));
}

// ============================================================
// D8 — DEX compiler (replaces dx)
// ============================================================

int tool_d8(
    const char* classes_jar,
    const char* output_dir,
    const char* min_api,
    bool release_mode,
    exec_result_t* result
) {
    std::string bin = tool_path("d8");
    const char* argv[16];
    int argc = 0;
    argv[argc++] = "--output";
    argv[argc++] = output_dir;
    argv[argc++] = "--min-api";
    argv[argc++] = min_api;
    if (release_mode) argv[argc++] = "--release";
    else argv[argc++] = "--debug";
    argv[argc++] = classes_jar;

    return native_exec(bin.c_str(), argv, argc, nullptr,
                       result->stdout_buf, sizeof(result->stdout_buf),
                       result->stderr_buf, sizeof(result->stderr_buf));
}

// ============================================================
// R8 — Shrinker/Optimizer
// ============================================================

int tool_r8(
    const char* classes_jar,
    const char* output_dir,
    const char* android_jar,
    const char* proguard_rules,
    const char* min_api,
    exec_result_t* result
) {
    std::string bin = tool_path("r8");
    const char* argv[] = {
        "--output", output_dir,
        "--lib", android_jar,
        "--pg-conf", proguard_rules,
        "--min-api", min_api,
        "--release",
        classes_jar
    };
    return native_exec(bin.c_str(), argv, 10, nullptr,
                       result->stdout_buf, sizeof(result->stdout_buf),
                       result->stderr_buf, sizeof(result->stderr_buf));
}

// ============================================================
// zipalign — APK alignment
// ============================================================

int tool_zipalign(
    const char* input_apk,
    const char* output_apk,
    exec_result_t* result
) {
    std::string bin = tool_path("zipalign");
    const char* argv[] = { "-v", "-f", "4", input_apk, output_apk };
    return native_exec(bin.c_str(), argv, 5, nullptr,
                       result->stdout_buf, sizeof(result->stdout_buf),
                       result->stderr_buf, sizeof(result->stderr_buf));
}

// ============================================================
// apksigner — APK signing
// ============================================================

int tool_apksigner_sign(
    const char* apk_path,
    const char* keystore_path,
    const char* key_alias,
    const char* ks_pass,
    const char* key_pass,
    exec_result_t* result
) {
    std::string bin = tool_path("apksigner");
    const char* argv[] = {
        "sign",
        "--ks", keystore_path,
        "--ks-key-alias", key_alias,
        "--ks-pass", ks_pass,
        "--key-pass", key_pass,
        "--out", apk_path,
        apk_path
    };
    return native_exec(bin.c_str(), argv, 12, nullptr,
                       result->stdout_buf, sizeof(result->stdout_buf),
                       result->stderr_buf, sizeof(result->stderr_buf));
}

int tool_apksigner_verify(
    const char* apk_path,
    exec_result_t* result
) {
    std::string bin = tool_path("apksigner");
    const char* argv[] = { "verify", "--verbose", apk_path };
    return native_exec(bin.c_str(), argv, 3, nullptr,
                       result->stdout_buf, sizeof(result->stdout_buf),
                       result->stderr_buf, sizeof(result->stderr_buf));
}

// ============================================================
// keytool — Java keystore management
// ============================================================

int tool_keytool_genkey(
    const char* keystore_path,
    const char* alias,
    const char* storepass,
    const char* keypass,
    const char* dname,
    exec_result_t* result
) {
    std::string bin = tool_path("keytool");
    const char* argv[] = {
        "-genkeypair",
        "-keystore", keystore_path,
        "-alias", alias,
        "-storepass", storepass,
        "-keypass", keypass,
        "-keyalg", "RSA",
        "-keysize", "2048",
        "-validity", "10000",
        "-dname", dname
    };
    return native_exec(bin.c_str(), argv, 16, nullptr,
                       result->stdout_buf, sizeof(result->stdout_buf),
                       result->stderr_buf, sizeof(result->stderr_buf));
}

// ============================================================
// clang — C/C++ compiler (NDK)
// ============================================================

int tool_clang_compile(
    const char* source_file,
    const char* output_obj,
    const char* sysroot,
    const char* target_triple,
    const char** extra_flags,
    int n_extra_flags,
    exec_result_t* result
) {
    std::string bin = tool_path("clang");
    const char* fixed_argv[] = {
        "-c", source_file,
        "-o", output_obj,
        "--sysroot", sysroot,
        "-target", target_triple,
        "-fPIC"
    };
    int fixed_argc = 9;
    std::vector<const char*> argv(fixed_argv, fixed_argv + fixed_argc);
    for (int i = 0; i < n_extra_flags; i++) argv.push_back(extra_flags[i]);

    return native_exec(bin.c_str(), argv.data(), (int)argv.size(), nullptr,
                       result->stdout_buf, sizeof(result->stdout_buf),
                       result->stderr_buf, sizeof(result->stderr_buf));
}

// ============================================================
// lld (via clang) — linker
// ============================================================

int tool_lld_link(
    const char** obj_files,
    int n_objs,
    const char* output_so,
    const char* sysroot,
    const char* target_triple,
    exec_result_t* result
) {
    std::string bin = tool_path("clang");
    std::vector<const char*> argv;
    argv.push_back("-fuse-ld=lld");
    argv.push_back("-shared");
    argv.push_back("-o"); argv.push_back(output_so);
    argv.push_back("--sysroot"); argv.push_back(sysroot);
    argv.push_back("-target"); argv.push_back(target_triple);
    for (int i = 0; i < n_objs; i++) argv.push_back(obj_files[i]);

    return native_exec(bin.c_str(), argv.data(), (int)argv.size(), nullptr,
                       result->stdout_buf, sizeof(result->stdout_buf),
                       result->stderr_buf, sizeof(result->stderr_buf));
}

// ============================================================
// AIDL — Interface Definition Language compiler
// ============================================================

int tool_aidl(
    const char* aidl_file,
    const char* include_dir,
    const char* output_dir,
    bool cpp_mode,
    exec_result_t* result
) {
    std::string bin = tool_path("aidl");
    const char* argv[16];
    int argc = 0;
    if (cpp_mode) argv[argc++] = "--lang=cpp";
    else argv[argc++] = "--lang=java";
    argv[argc++] = "-I"; argv[argc++] = include_dir;
    argv[argc++] = "-o"; argv[argc++] = output_dir;
    argv[argc++] = aidl_file;

    return native_exec(bin.c_str(), argv, argc, nullptr,
                       result->stdout_buf, sizeof(result->stdout_buf),
                       result->stderr_buf, sizeof(result->stderr_buf));
}

// ============================================================
// bundletool — AAB packaging
// ============================================================

int tool_bundletool_build(
    const char* bundletool_jar,
    const char* base_zip,
    const char* output_aab,
    exec_result_t* result
) {
    std::string java_bin = tool_path("java");
    const char* argv[] = {
        "-jar", bundletool_jar,
        "build-bundle",
        "--modules", base_zip,
        "--output", output_aab
    };
    return native_exec(java_bin.c_str(), argv, 7, nullptr,
                       result->stdout_buf, sizeof(result->stdout_buf),
                       result->stderr_buf, sizeof(result->stderr_buf));
}

int tool_bundletool_extract_apks(
    const char* bundletool_jar,
    const char* aab_path,
    const char* output_dir,
    const char* keystore_path,
    const char* ks_alias,
    const char* ks_pass,
    exec_result_t* result
) {
    std::string java_bin = tool_path("java");
    const char* argv[] = {
        "-jar", bundletool_jar,
        "build-apks",
        "--bundle", aab_path,
        "--output", output_dir,
        "--ks", keystore_path,
        "--ks-key-alias", ks_alias,
        "--ks-pass", ks_pass,
        "--mode=universal"
    };
    return native_exec(java_bin.c_str(), argv, 14, nullptr,
                       result->stdout_buf, sizeof(result->stdout_buf),
                       result->stderr_buf, sizeof(result->stderr_buf));
}

// ============================================================
// strip — symbol stripper
// ============================================================

int tool_strip(
    const char* input_so,
    const char* output_so,
    exec_result_t* result
) {
    std::string bin = tool_path("llvm-strip");
    const char* argv[] = { "--strip-unneeded", "-o", output_so, input_so };
    return native_exec(bin.c_str(), argv, 4, nullptr,
                       result->stdout_buf, sizeof(result->stdout_buf),
                       result->stderr_buf, sizeof(result->stderr_buf));
}

// ============================================================
// ar — archive tool (static libs)
// ============================================================

int tool_ar_create(
    const char* output_lib,
    const char** obj_files,
    int n_objs,
    exec_result_t* result
) {
    std::string bin = tool_path("llvm-ar");
    std::vector<const char*> argv;
    argv.push_back("rcs");
    argv.push_back(output_lib);
    for (int i = 0; i < n_objs; i++) argv.push_back(obj_files[i]);

    return native_exec(bin.c_str(), argv.data(), (int)argv.size(), nullptr,
                       result->stdout_buf, sizeof(result->stdout_buf),
                       result->stderr_buf, sizeof(result->stderr_buf));
}

// ============================================================
// objdump — object file disassembly
// ============================================================

int tool_objdump(
    const char* binary,
    bool disassemble,
    exec_result_t* result
) {
    std::string bin = tool_path("llvm-objdump");
    const char* argv[] = { disassemble ? "-d" : "-h", binary };
    return native_exec(bin.c_str(), argv, 2, nullptr,
                       result->stdout_buf, sizeof(result->stdout_buf),
                       result->stderr_buf, sizeof(result->stderr_buf));
}

// ============================================================
// dexdump — DEX file dump
// ============================================================

int tool_dexdump(
    const char* dex_file,
    exec_result_t* result
) {
    std::string bin = tool_path("dexdump");
    const char* argv[] = { "-d", dex_file };
    return native_exec(bin.c_str(), argv, 2, nullptr,
                       result->stdout_buf, sizeof(result->stdout_buf),
                       result->stderr_buf, sizeof(result->stderr_buf));
}

// ============================================================
// lint — code quality
// ============================================================

int tool_lint(
    const char* project_dir,
    const char* output_xml,
    exec_result_t* result
) {
    std::string bin = tool_path("lint");
    const char* argv[] = { "--xml", output_xml, project_dir };
    return native_exec(bin.c_str(), argv, 3, nullptr,
                       result->stdout_buf, sizeof(result->stdout_buf),
                       result->stderr_buf, sizeof(result->stderr_buf));
}

// ============================================================
// adb — Android Debug Bridge
// ============================================================

int tool_adb(
    const char** args,
    int argc,
    exec_result_t* result
) {
    std::string bin = tool_path("adb");
    return native_exec(bin.c_str(), args, argc, nullptr,
                       result->stdout_buf, sizeof(result->stdout_buf),
                       result->stderr_buf, sizeof(result->stderr_buf));
}
