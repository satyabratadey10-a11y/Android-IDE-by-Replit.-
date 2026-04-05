package com.androidide.project

import android.util.Log
import com.androidide.toolchain.ToolchainManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/**
 * ProjectManager.kt
 *
 * Creates, loads, lists, and deletes Android projects stored in the
 * app's private files directory.
 */
class ProjectManager(private val toolchain: ToolchainManager) {

    companion object {
        private const val TAG = "ProjectManager"
    }

    /** List all projects in the projects directory. */
    suspend fun listProjects(): List<AndroidProject> = withContext(Dispatchers.IO) {
        toolchain.projectsDir.listFiles()
            ?.filter { it.isDirectory }
            ?.mapNotNull { loadProject(it) }
            ?: emptyList()
    }

    /** Create a new Android project with default source scaffold. */
    suspend fun createProject(
        name: String,
        packageName: String,
        minSdk: Int = 24,
        targetSdk: Int = 34,
        useKotlin: Boolean = true,
        useNdk: Boolean = false
    ): AndroidProject = withContext(Dispatchers.IO) {
        val id = UUID.randomUUID().toString().substring(0, 8)
        val projectDir = File(toolchain.projectsDir, name.replace(" ", "_"))
        projectDir.mkdirs()

        val project = AndroidProject(
            id = id,
            name = name,
            packageName = packageName,
            dir = projectDir,
            minSdk = minSdk,
            targetSdk = targetSdk,
            useKotlin = useKotlin,
            useNdk = useNdk
        )

        scaffoldProject(project)
        saveProjectMeta(project)

        Log.i(TAG, "Created project: $name at ${projectDir.absolutePath}")
        project
    }

    /** Delete a project and all its files. */
    suspend fun deleteProject(project: AndroidProject) = withContext(Dispatchers.IO) {
        project.dir.deleteRecursively()
        Log.i(TAG, "Deleted project: ${project.name}")
    }

    // ---- Internal helpers ----

    private fun loadProject(dir: File): AndroidProject? {
        val metaFile = File(dir, ".androidide")
        if (!metaFile.exists()) return null
        return try {
            val lines = metaFile.readLines().associate { line ->
                val kv = line.split("=", limit = 2)
                if (kv.size == 2) kv[0].trim() to kv[1].trim() else "" to ""
            }
            AndroidProject(
                id = lines["id"] ?: dir.name,
                name = lines["name"] ?: dir.name,
                packageName = lines["packageName"] ?: "com.example.app",
                dir = dir,
                minSdk = lines["minSdk"]?.toIntOrNull() ?: 24,
                targetSdk = lines["targetSdk"]?.toIntOrNull() ?: 34,
                useKotlin = lines["useKotlin"]?.toBoolean() ?: true,
                useNdk = lines["useNdk"]?.toBoolean() ?: false
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load project meta from $dir: ${e.message}")
            null
        }
    }

    private fun saveProjectMeta(project: AndroidProject) {
        File(project.dir, ".androidide").writeText("""
            id=${project.id}
            name=${project.name}
            packageName=${project.packageName}
            minSdk=${project.minSdk}
            targetSdk=${project.targetSdk}
            useKotlin=${project.useKotlin}
            useNdk=${project.useNdk}
        """.trimIndent())
    }

    private fun scaffoldProject(project: AndroidProject) {
        // Directory structure
        project.javaDir.mkdirs()
        File(project.resDir, "layout").mkdirs()
        File(project.resDir, "values").mkdirs()
        File(project.resDir, "drawable").mkdirs()
        File(project.resDir, "mipmap-hdpi").mkdirs()

        // AndroidManifest.xml
        project.manifestFile.writeText("""
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="${project.packageName}">

    <application
        android:allowBackup="true"
        android:label="@string/app_name"
        android:theme="@style/AppTheme">

        <activity android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

    </application>
</manifest>
        """.trimIndent())

        // Main activity
        val packagePath = project.packageName.replace(".", "/")
        val activityDir = File(project.javaDir, packagePath)
        activityDir.mkdirs()

        if (project.useKotlin) {
            File(activityDir, "MainActivity.kt").writeText("""
package ${project.packageName}

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Greeting("Android")
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(text = "Hello, \$name!")
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    MaterialTheme {
        Greeting("Android")
    }
}
            """.trimIndent())
        } else {
            File(activityDir, "MainActivity.java").writeText("""
package ${project.packageName};

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TextView tv = new TextView(this);
        tv.setText("Hello, Android!");
        setContentView(tv);
    }
}
            """.trimIndent())
        }

        // res/values/strings.xml
        File(project.resDir, "values/strings.xml").writeText("""
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">${project.name}</string>
</resources>
        """.trimIndent())

        // res/values/styles.xml
        File(project.resDir, "values/themes.xml").writeText("""
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="AppTheme" parent="android:Theme.Material.Light.NoActionBar">
    </style>
</resources>
        """.trimIndent())

        // Proguard rules
        File(project.dir, "proguard-rules.pro").writeText("""
# Add project specific ProGuard rules here.
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
        """.trimIndent())

        // NDK scaffold if needed
        if (project.useNdk) {
            val cppDir = File(project.srcDir, "cpp")
            cppDir.mkdirs()
            File(cppDir, "native-lib.cpp").writeText("""
#include <jni.h>
#include <string>

extern "C" JNIEXPORT jstring JNICALL
Java_${project.packageName.replace(".", "_")}_MainActivity_stringFromJNI(
        JNIEnv* env,
        jobject /* this */) {
    std::string hello = "Hello from C++!";
    return env->NewStringUTF(hello.c_str());
}
            """.trimIndent())

            File(cppDir, "CMakeLists.txt").writeText("""
cmake_minimum_required(VERSION 3.22.1)
project("${project.name.lowercase().replace(" ", "_")}")
add_library(native-lib SHARED native-lib.cpp)
find_library(log-lib log)
target_link_libraries(native-lib \${"$"}{log-lib})
            """.trimIndent())
        }
    }
}
