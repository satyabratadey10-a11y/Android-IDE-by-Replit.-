#pragma once
#include <stdbool.h>

#ifdef __cplusplus
extern "C" {
#endif

int file_mkdir_p(const char* path);
long long file_get_size(const char* path);
bool file_exists(const char* path);
bool file_is_executable(const char* path);

#ifdef __cplusplus
}
#endif
