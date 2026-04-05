#pragma once
#include <stddef.h>

#ifdef __cplusplus
extern "C" {
#endif

// Callback type for streaming output: text, stream (0=stdout 1=stderr), user_data
typedef void (*output_callback_t)(const char* text, int stream, void* user_data);

int native_chmod_exec(const char* path);

int native_exec(
    const char* binary,
    const char** argv,
    int argc,
    const char* working_dir,
    char* out_stdout,
    size_t stdout_size,
    char* out_stderr,
    size_t stderr_size
);

int native_exec_streaming(
    const char* binary,
    const char** argv,
    int argc,
    const char* working_dir,
    output_callback_t callback,
    void* user_data
);

#ifdef __cplusplus
}
#endif
