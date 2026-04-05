#pragma once
#include <string>
#include <vector>

struct exec_result_t {
    char stdout_buf[65536];
    char stderr_buf[65536];
    int exit_code;
};

void tool_set_toolchain_dir(const char* dir);

int tool_aapt2_compile(const char* input_dir, const char* output_dir, exec_result_t* result);
int tool_aapt2_link(const char* manifest, const char* compiled_res, const char* android_jar, const char* output_apk, exec_result_t* result);
int tool_d8(const char* classes_jar, const char* output_dir, const char* min_api, bool release_mode, exec_result_t* result);
int tool_r8(const char* classes_jar, const char* output_dir, const char* android_jar, const char* proguard_rules, const char* min_api, exec_result_t* result);
int tool_zipalign(const char* input_apk, const char* output_apk, exec_result_t* result);
int tool_apksigner_sign(const char* apk_path, const char* keystore_path, const char* key_alias, const char* ks_pass, const char* key_pass, exec_result_t* result);
int tool_apksigner_verify(const char* apk_path, exec_result_t* result);
int tool_keytool_genkey(const char* keystore_path, const char* alias, const char* storepass, const char* keypass, const char* dname, exec_result_t* result);
int tool_clang_compile(const char* source_file, const char* output_obj, const char* sysroot, const char* target_triple, const char** extra_flags, int n_extra_flags, exec_result_t* result);
int tool_lld_link(const char** obj_files, int n_objs, const char* output_so, const char* sysroot, const char* target_triple, exec_result_t* result);
int tool_aidl(const char* aidl_file, const char* include_dir, const char* output_dir, bool cpp_mode, exec_result_t* result);
int tool_bundletool_build(const char* bundletool_jar, const char* base_zip, const char* output_aab, exec_result_t* result);
int tool_bundletool_extract_apks(const char* bundletool_jar, const char* aab_path, const char* output_dir, const char* keystore_path, const char* ks_alias, const char* ks_pass, exec_result_t* result);
int tool_strip(const char* input_so, const char* output_so, exec_result_t* result);
int tool_ar_create(const char* output_lib, const char** obj_files, int n_objs, exec_result_t* result);
int tool_objdump(const char* binary, bool disassemble, exec_result_t* result);
int tool_dexdump(const char* dex_file, exec_result_t* result);
int tool_lint(const char* project_dir, const char* output_xml, exec_result_t* result);
int tool_adb(const char** args, int argc, exec_result_t* result);
