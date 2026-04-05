/**
 * file_utils.cpp — Native file I/O helpers
 */
#include "file_utils.h"
#include <sys/stat.h>
#include <dirent.h>
#include <string.h>
#include <errno.h>
#include <unistd.h>
#include <android/log.h>

#define LOG_TAG "AndroidIDE_FileUtils"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

int file_mkdir_p(const char* path) {
    char tmp[4096];
    strncpy(tmp, path, sizeof(tmp) - 1);
    size_t len = strlen(tmp);
    if (tmp[len - 1] == '/') tmp[len - 1] = '\0';

    for (char* p = tmp + 1; *p; p++) {
        if (*p == '/') {
            *p = '\0';
            if (mkdir(tmp, 0755) != 0 && errno != EEXIST) {
                LOGE("mkdir_p: failed at %s: %s", tmp, strerror(errno));
                return -1;
            }
            *p = '/';
        }
    }
    if (mkdir(tmp, 0755) != 0 && errno != EEXIST) {
        LOGE("mkdir_p: failed at %s: %s", tmp, strerror(errno));
        return -1;
    }
    return 0;
}

long long file_get_size(const char* path) {
    struct stat st;
    if (stat(path, &st) != 0) return -1;
    return (long long)st.st_size;
}

bool file_exists(const char* path) {
    return access(path, F_OK) == 0;
}

bool file_is_executable(const char* path) {
    return access(path, X_OK) == 0;
}
