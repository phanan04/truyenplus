"""Compile real Android startup classes with Firebase/Android fakes to check missing config."""
from pathlib import Path
import os
import subprocess
import tempfile

root = Path(__file__).resolve().parents[1]
cache = Path.home() / '.gradle/caches/modules-2/files-2.1'
def jar(group, name, version=None):
    folder = cache / group / name
    matches = sorted((folder / version).glob('*/*.jar')) if version else sorted(folder.glob('*/*/*.jar'))
    if not matches:
        raise SystemExit('Missing cached compiler library: ' + name)
    return str(matches[-1])
stdlib = jar('org.jetbrains.kotlin', 'kotlin-stdlib', '2.2.21')
classpath = os.pathsep.join([jar('org.jetbrains.kotlin', 'kotlin-compiler-embeddable', '2.2.21'), stdlib,
    jar('org.jetbrains.kotlin', 'kotlin-reflect', '2.2.21'), jar('org.jetbrains.kotlin', 'kotlin-script-runtime', '2.2.21'),
    jar('org.jetbrains', 'annotations'), jar('org.jetbrains.kotlinx', 'kotlinx-coroutines-core-jvm')])
java = Path(os.environ['JAVA_HOME']) / 'bin/java'
fakes = {
'Application.kt': 'package android.app; class Application',
'BuildConfig.kt': 'package org.ireader.app; object BuildConfig { var DEBUG = true }',
'FirebaseApp.kt': '''package com.google.firebase
import android.app.Application
object FirebaseApp {
 var configured = false
 fun initializeApp(context: Application): FirebaseApp? = if (configured) this else null
}''',
'FirebaseAnalytics.kt': '''package com.google.firebase.analytics
import android.app.Application
import com.google.firebase.FirebaseApp
object FirebaseAnalytics {
 var collection: Boolean? = null
 fun getInstance(context: Application): FirebaseAnalytics {
  check(FirebaseApp.configured) { "Firebase unavailable" }; return this
 }
 fun setAnalyticsCollectionEnabled(enabled: Boolean) { collection = enabled }
}''',
'FirebaseCrashlytics.kt': '''package com.google.firebase.crashlytics
import com.google.firebase.FirebaseApp
object FirebaseCrashlytics {
 var collection: Boolean? = null
 fun getInstance(): FirebaseCrashlytics {
  check(FirebaseApp.configured) { "Default FirebaseApp is not initialized" }; return this
 }
 fun setCrashlyticsCollectionEnabled(enabled: Boolean) { collection = enabled }
}''',
'AppExceptionHandler.kt': '''package org.ireader.app.initiators
import android.app.Application
class AppExceptionHandler(a: Thread.UncaughtExceptionHandler, b: Thread.UncaughtExceptionHandler, c: Application): Thread.UncaughtExceptionHandler {
 override fun uncaughtException(t: Thread, e: Throwable) {}
}''',
'Test.kt': '''import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import org.ireader.app.BuildConfig
import org.ireader.app.initiators.*
fun main() {
 val original = Thread.getDefaultUncaughtExceptionHandler()
 try {
  for (debug in listOf(true, false)) {
   BuildConfig.DEBUG = debug
   FirebaseApp.configured = false
   FirebaseAnalytics.collection = null
   FirebaseCrashlytics.collection = null
   val fallback = Thread.UncaughtExceptionHandler { _, _ -> }
   Thread.setDefaultUncaughtExceptionHandler(fallback)
   // Koin constructs CrashHandler before FirebaseInitializer.
   CrashHandler(Application())
   FirebaseInitializer(Application())
   check(Thread.getDefaultUncaughtExceptionHandler() === fallback)
   check(FirebaseAnalytics.collection == null && FirebaseCrashlytics.collection == null)
   println("PASS missing Firebase config; debug=$debug; startup and fallback handler preserved")
   FirebaseApp.configured = true
   FirebaseInitializer(Application())
   check(FirebaseAnalytics.collection == !debug && FirebaseCrashlytics.collection == !debug)
   CrashHandler(Application())
   check(debug || Thread.getDefaultUncaughtExceptionHandler() is AppExceptionHandler)
   println("PASS configured Firebase; debug=$debug; collection policy preserved")
  }
 } finally { Thread.setDefaultUncaughtExceptionHandler(original) }
}'''
}
for flavor in ('standard', 'dev'):
    with tempfile.TemporaryDirectory(prefix='truyenplus-firebase-') as tmp:
        output = Path(tmp)
        sources = []
        for name, text in fakes.items():
            path = output / name
            path.write_text(text)
            sources.append(str(path))
        sources += [str(root / f'android/src/{flavor}/java/org/ireader/app/initiators/{name}.kt')
                    for name in ('FirebaseInitializer', 'CrashHandler')]
        subprocess.run([str(java), '-cp', classpath, 'org.jetbrains.kotlin.cli.jvm.K2JVMCompiler',
                        '-no-stdlib', '-no-reflect', '-classpath', stdlib,
                        '-d', str(output / 'classes'), *sources], check=True, capture_output=True)
        print('Checking flavor:', flavor, flush=True)
        subprocess.run([str(java), '-cp', str(output / 'classes') + os.pathsep + stdlib, 'TestKt'], check=True)
