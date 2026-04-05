/**
 * native_executor.cpp
 *
 * Low-level native process execution engine.
 * Spawns child processes, captures stdout/stderr, returns exit codes.
 * This is the heart of all real tool invocations.
 */

#include "native_executor.h"
#include <android/log.h>
#include <unistd.h>
#include <sys/wait.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <errno.h>
#include <string.h>
#include <stdlib.h>
#include <vector>
#include <string>
#include <sstream>

#define LOG_TAG "AndroidIDE_Native"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// Set executable bit on a file
int native_chmod_exec(const char* path) {
    struct stat st;
    if (stat(path, &st) != 0) {
        LOGE("chmod_exec: stat failed for %s: %s", path, strerror(errno));
        return -1;
    }
    mode_t mode = st.st_mode | S_IXUSR | S_IXGRP;
    if (chmod(path, mode) != 0) {
        LOGE("chmod_exec: chmod failed for %s: %s", path, strerror(errno));
        return -1;
    }
    LOGI("chmod_exec: set +x on %s", path);
    return 0;
}

// Execute a command with arguments, capture all output.
// Returns exit code. stdout+stderr written to out_stdout and out_stderr.
int native_exec(
    const char* binary,
    const char** argv,
    int argc,
    const char* working_dir,
    char* out_stdout,
    size_t stdout_size,
    char* out_stderr,
    size_t stderr_size
) {
    int stdout_pipe[2];
    int stderr_pipe[2];

    if (pipe(stdout_pipe) != 0 || pipe(stderr_pipe) != 0) {
        LOGE("native_exec: pipe() failed: %s", strerror(errno));
        return -1;
    }

    pid_t pid = fork();
    if (pid < 0) {
        LOGE("native_exec: fork() failed: %s", strerror(errno));
        return -1;
    }

    if (pid == 0) {
        // Child process
        close(stdout_pipe[0]);
        close(stderr_pipe[0]);
        dup2(stdout_pipe[1], STDOUT_FILENO);
        dup2(stderr_pipe[1], STDERR_FILENO);
        close(stdout_pipe[1]);
        close(stderr_pipe[1]);

        if (working_dir && chdir(working_dir) != 0) {
            fprintf(stderr, "chdir failed: %s\n", strerror(errno));
            _exit(127);
        }

        // Build argv array for execv
        std::vector<char*> args;
        args.push_back(const_cast<char*>(binary));
        for (int i = 0; i < argc; i++) {
            args.push_back(const_cast<char*>(argv[i]));
        }
        args.push_back(nullptr);

        execv(binary, args.data());
        fprintf(stderr, "execv(%s) failed: %s\n", binary, strerror(errno));
        _exit(126);
    }

    // Parent process
    close(stdout_pipe[1]);
    close(stderr_pipe[1]);

    // Read stdout
    ssize_t n;
    size_t stdout_pos = 0;
    while ((n = read(stdout_pipe[0], out_stdout + stdout_pos,
                     stdout_size - stdout_pos - 1)) > 0) {
        stdout_pos += n;
        if (stdout_pos >= stdout_size - 1) break;
    }
    out_stdout[stdout_pos] = '\0';
    close(stdout_pipe[0]);

    // Read stderr
    size_t stderr_pos = 0;
    while ((n = read(stderr_pipe[0], out_stderr + stderr_pos,
                     stderr_size - stderr_pos - 1)) > 0) {
        stderr_pos += n;
        if (stderr_pos >= stderr_size - 1) break;
    }
    out_stderr[stderr_pos] = '\0';
    close(stderr_pipe[0]);

    int status = 0;
    waitpid(pid, &status, 0);

    int exit_code = WIFEXITED(status) ? WEXITSTATUS(status) : -1;
    LOGI("native_exec: %s exited with code %d", binary, exit_code);
    return exit_code;
}

// Streaming version: invokes callback for each chunk of output
int native_exec_streaming(
    const char* binary,
    const char** argv,
    int argc,
    const char* working_dir,
    output_callback_t callback,
    void* user_data
) {
    int stdout_pipe[2];
    int stderr_pipe[2];

    if (pipe(stdout_pipe) != 0 || pipe(stderr_pipe) != 0) {
        return -1;
    }

    // Set pipes non-blocking for multiplexed reads
    fcntl(stdout_pipe[0], F_SETFL, O_NONBLOCK);
    fcntl(stderr_pipe[0], F_SETFL, O_NONBLOCK);

    pid_t pid = fork();
    if (pid < 0) return -1;

    if (pid == 0) {
        close(stdout_pipe[0]);
        close(stderr_pipe[0]);
        dup2(stdout_pipe[1], STDOUT_FILENO);
        dup2(stderr_pipe[1], STDERR_FILENO);
        close(stdout_pipe[1]);
        close(stderr_pipe[1]);

        if (working_dir) chdir(working_dir);

        std::vector<char*> args;
        args.push_back(const_cast<char*>(binary));
        for (int i = 0; i < argc; i++) {
            args.push_back(const_cast<char*>(argv[i]));
        }
        args.push_back(nullptr);

        execv(binary, args.data());
        _exit(126);
    }

    close(stdout_pipe[1]);
    close(stderr_pipe[1]);

    char buf[4096];
    bool running = true;
    while (running) {
        int status;
        pid_t result = waitpid(pid, &status, WNOHANG);
        if (result == pid) running = false;

        ssize_t n = read(stdout_pipe[0], buf, sizeof(buf) - 1);
        if (n > 0) {
            buf[n] = '\0';
            if (callback) callback(buf, 0, user_data); // 0 = stdout
        }

        n = read(stderr_pipe[0], buf, sizeof(buf) - 1);
        if (n > 0) {
            buf[n] = '\0';
            if (callback) callback(buf, 1, user_data); // 1 = stderr
        }

        if (running) usleep(10000); // 10ms poll
    }

    // Drain remaining output
    ssize_t n;
    while ((n = read(stdout_pipe[0], buf, sizeof(buf) - 1)) > 0) {
        buf[n] = '\0';
        if (callback) callback(buf, 0, user_data);
    }
    while ((n = read(stderr_pipe[0], buf, sizeof(buf) - 1)) > 0) {
        buf[n] = '\0';
        if (callback) callback(buf, 1, user_data);
    }

    close(stdout_pipe[0]);
    close(stderr_pipe[0]);

    int status = 0;
    waitpid(pid, &status, WNOHANG);
    return WIFEXITED(status) ? WEXITSTATUS(status) : 0;
}
