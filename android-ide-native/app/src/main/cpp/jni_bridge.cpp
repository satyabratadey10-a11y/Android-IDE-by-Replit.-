/**
 * jni_bridge.cpp
 *
 * JNI bridge exposing all native tool wrappers to Kotlin/Java.
 * Every public function here corresponds to a native method in Kotlin.
 */

#include <jni.h>
#include <string>
#include <vector>
#include <android/log.h>
#include "native_executor.h"
#include "tool_wrapper.h"
#include "file_utils.h"

#define LOG_TAG "AndroidIDE_JNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// Helper: Convert Java string to std::string
static std::string jstr(JNIEnv* env, jstring js) {
    if (!js) return "";
    const char* chars = env->GetStringUTFChars(js, nullptr);
    std::string s(chars);
    env->ReleaseStringUTFChars(js, chars);
    return s;
}

// Helper: Convert JNI result to Java string array [stdout, stderr, exitCode]
static jobjectArray make_result(JNIEnv* env, const exec_result_t& r, int code) {
    jclass str_class = env->FindClass("java/lang/String");
    jobjectArray arr = env->NewObjectArray(3, str_class, nullptr);
    env->SetObjectArrayElement(arr, 0, env->NewStringUTF(r.stdout_buf));
    env->SetObjectArrayElement(arr, 1, env->NewStringUTF(r.stderr_buf));
    env->SetObjectArrayElement(arr, 2, env->NewStringUTF(std::to_string(code).c_str()));
    return arr;
}

extern "C" {

// -------------------------------------------------------
// Toolchain setup
// -------------------------------------------------------

JNIEXPORT void JNICALL
Java_com_androidide_jni_NativeBridge_setToolchainDir(
    JNIEnv* env, jobject, jstring dir)
{
    tool_set_toolchain_dir(jstr(env, dir).c_str());
}

JNIEXPORT jint JNICALL
Java_com_androidide_jni_NativeBridge_chmodExec(
    JNIEnv* env, jobject, jstring path)
{
    return native_chmod_exec(jstr(env, path).c_str());
}

// -------------------------------------------------------
// Generic command execution (for terminal)
// -------------------------------------------------------

JNIEXPORT jobjectArray JNICALL
Java_com_androidide_jni_NativeBridge_execCommand(
    JNIEnv* env, jobject,
    jstring binary,
    jobjectArray argv_arr,
    jstring working_dir)
{
    std::string bin = jstr(env, binary);
    std::string wd  = jstr(env, working_dir);

    int argc = env->GetArrayLength(argv_arr);
    std::vector<std::string> argv_strs(argc);
    std::vector<const char*> argv_ptrs(argc);
    for (int i = 0; i < argc; i++) {
        argv_strs[i] = jstr(env, (jstring)env->GetObjectArrayElement(argv_arr, i));
        argv_ptrs[i] = argv_strs[i].c_str();
    }

    exec_result_t result = {};
    int code = native_exec(
        bin.c_str(),
        argv_ptrs.data(), argc,
        wd.empty() ? nullptr : wd.c_str(),
        result.stdout_buf, sizeof(result.stdout_buf),
        result.stderr_buf, sizeof(result.stderr_buf)
    );
    return make_result(env, result, code);
}

// -------------------------------------------------------
// AAPT2
// -------------------------------------------------------

JNIEXPORT jobjectArray JNICALL
Java_com_androidide_jni_NativeBridge_aapt2Compile(
    JNIEnv* env, jobject,
    jstring input_dir, jstring output_dir)
{
    exec_result_t r = {};
    int code = tool_aapt2_compile(
        jstr(env, input_dir).c_str(),
        jstr(env, output_dir).c_str(),
        &r);
    return make_result(env, r, code);
}

JNIEXPORT jobjectArray JNICALL
Java_com_androidide_jni_NativeBridge_aapt2Link(
    JNIEnv* env, jobject,
    jstring manifest, jstring compiled_res,
    jstring android_jar, jstring output_apk)
{
    exec_result_t r = {};
    int code = tool_aapt2_link(
        jstr(env, manifest).c_str(),
        jstr(env, compiled_res).c_str(),
        jstr(env, android_jar).c_str(),
        jstr(env, output_apk).c_str(),
        &r);
    return make_result(env, r, code);
}

// -------------------------------------------------------
// D8
// -------------------------------------------------------

JNIEXPORT jobjectArray JNICALL
Java_com_androidide_jni_NativeBridge_d8Compile(
    JNIEnv* env, jobject,
    jstring classes_jar, jstring output_dir,
    jstring min_api, jboolean release_mode)
{
    exec_result_t r = {};
    int code = tool_d8(
        jstr(env, classes_jar).c_str(),
        jstr(env, output_dir).c_str(),
        jstr(env, min_api).c_str(),
        (bool)release_mode,
        &r);
    return make_result(env, r, code);
}

// -------------------------------------------------------
// R8
// -------------------------------------------------------

JNIEXPORT jobjectArray JNICALL
Java_com_androidide_jni_NativeBridge_r8Optimize(
    JNIEnv* env, jobject,
    jstring classes_jar, jstring output_dir,
    jstring android_jar, jstring proguard_rules, jstring min_api)
{
    exec_result_t r = {};
    int code = tool_r8(
        jstr(env, classes_jar).c_str(),
        jstr(env, output_dir).c_str(),
        jstr(env, android_jar).c_str(),
        jstr(env, proguard_rules).c_str(),
        jstr(env, min_api).c_str(),
        &r);
    return make_result(env, r, code);
}

// -------------------------------------------------------
// zipalign
// -------------------------------------------------------

JNIEXPORT jobjectArray JNICALL
Java_com_androidide_jni_NativeBridge_zipalign(
    JNIEnv* env, jobject,
    jstring input_apk, jstring output_apk)
{
    exec_result_t r = {};
    int code = tool_zipalign(
        jstr(env, input_apk).c_str(),
        jstr(env, output_apk).c_str(),
        &r);
    return make_result(env, r, code);
}

// -------------------------------------------------------
// apksigner
// -------------------------------------------------------

JNIEXPORT jobjectArray JNICALL
Java_com_androidide_jni_NativeBridge_apksignerSign(
    JNIEnv* env, jobject,
    jstring apk_path, jstring keystore_path,
    jstring key_alias, jstring ks_pass, jstring key_pass)
{
    exec_result_t r = {};
    int code = tool_apksigner_sign(
        jstr(env, apk_path).c_str(),
        jstr(env, keystore_path).c_str(),
        jstr(env, key_alias).c_str(),
        jstr(env, ks_pass).c_str(),
        jstr(env, key_pass).c_str(),
        &r);
    return make_result(env, r, code);
}

JNIEXPORT jobjectArray JNICALL
Java_com_androidide_jni_NativeBridge_apksignerVerify(
    JNIEnv* env, jobject, jstring apk_path)
{
    exec_result_t r = {};
    int code = tool_apksigner_verify(jstr(env, apk_path).c_str(), &r);
    return make_result(env, r, code);
}

// -------------------------------------------------------
// keytool
// -------------------------------------------------------

JNIEXPORT jobjectArray JNICALL
Java_com_androidide_jni_NativeBridge_keytoolGenKey(
    JNIEnv* env, jobject,
    jstring keystore_path, jstring alias,
    jstring storepass, jstring keypass, jstring dname)
{
    exec_result_t r = {};
    int code = tool_keytool_genkey(
        jstr(env, keystore_path).c_str(),
        jstr(env, alias).c_str(),
        jstr(env, storepass).c_str(),
        jstr(env, keypass).c_str(),
        jstr(env, dname).c_str(),
        &r);
    return make_result(env, r, code);
}

// -------------------------------------------------------
// clang / lld
// -------------------------------------------------------

JNIEXPORT jobjectArray JNICALL
Java_com_androidide_jni_NativeBridge_clangCompile(
    JNIEnv* env, jobject,
    jstring source_file, jstring output_obj,
    jstring sysroot, jstring target_triple)
{
    exec_result_t r = {};
    int code = tool_clang_compile(
        jstr(env, source_file).c_str(),
        jstr(env, output_obj).c_str(),
        jstr(env, sysroot).c_str(),
        jstr(env, target_triple).c_str(),
        nullptr, 0, &r);
    return make_result(env, r, code);
}

// -------------------------------------------------------
// AIDL
// -------------------------------------------------------

JNIEXPORT jobjectArray JNICALL
Java_com_androidide_jni_NativeBridge_aidlCompile(
    JNIEnv* env, jobject,
    jstring aidl_file, jstring include_dir,
    jstring output_dir, jboolean cpp_mode)
{
    exec_result_t r = {};
    int code = tool_aidl(
        jstr(env, aidl_file).c_str(),
        jstr(env, include_dir).c_str(),
        jstr(env, output_dir).c_str(),
        (bool)cpp_mode, &r);
    return make_result(env, r, code);
}

// -------------------------------------------------------
// bundletool
// -------------------------------------------------------

JNIEXPORT jobjectArray JNICALL
Java_com_androidide_jni_NativeBridge_bundletoolBuild(
    JNIEnv* env, jobject,
    jstring bundletool_jar, jstring base_zip, jstring output_aab)
{
    exec_result_t r = {};
    int code = tool_bundletool_build(
        jstr(env, bundletool_jar).c_str(),
        jstr(env, base_zip).c_str(),
        jstr(env, output_aab).c_str(),
        &r);
    return make_result(env, r, code);
}

// -------------------------------------------------------
// dexdump
// -------------------------------------------------------

JNIEXPORT jobjectArray JNICALL
Java_com_androidide_jni_NativeBridge_dexdump(
    JNIEnv* env, jobject, jstring dex_file)
{
    exec_result_t r = {};
    int code = tool_dexdump(jstr(env, dex_file).c_str(), &r);
    return make_result(env, r, code);
}

// -------------------------------------------------------
// objdump
// -------------------------------------------------------

JNIEXPORT jobjectArray JNICALL
Java_com_androidide_jni_NativeBridge_objdump(
    JNIEnv* env, jobject, jstring binary, jboolean disassemble)
{
    exec_result_t r = {};
    int code = tool_objdump(jstr(env, binary).c_str(), (bool)disassemble, &r);
    return make_result(env, r, code);
}

// -------------------------------------------------------
// adb
// -------------------------------------------------------

JNIEXPORT jobjectArray JNICALL
Java_com_androidide_jni_NativeBridge_adbCommand(
    JNIEnv* env, jobject, jobjectArray args_arr)
{
    int argc = env->GetArrayLength(args_arr);
    std::vector<std::string> strs(argc);
    std::vector<const char*> ptrs(argc);
    for (int i = 0; i < argc; i++) {
        strs[i] = jstr(env, (jstring)env->GetObjectArrayElement(args_arr, i));
        ptrs[i] = strs[i].c_str();
    }
    exec_result_t r = {};
    int code = tool_adb(ptrs.data(), argc, &r);
    return make_result(env, r, code);
}

// -------------------------------------------------------
// File utilities
// -------------------------------------------------------

JNIEXPORT jboolean JNICALL
Java_com_androidide_jni_NativeBridge_extractAsset(
    JNIEnv* env, jobject,
    jstring asset_path, jstring output_path)
{
    // The actual extraction from AAssetManager is done in Kotlin.
    // This JNI call just sets the executable bit.
    return (jboolean)(native_chmod_exec(jstr(env, output_path).c_str()) == 0);
}

JNIEXPORT jlong JNICALL
Java_com_androidide_jni_NativeBridge_getFileSize(
    JNIEnv* env, jobject, jstring path)
{
    return file_get_size(jstr(env, path).c_str());
}

} // extern "C"
