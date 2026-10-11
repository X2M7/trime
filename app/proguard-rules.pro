# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# disable obfuscation
-dontobfuscate

# Keep JNI interface
-keep class com.osfans.trime.core.* { *; }

# The separately compiled acceptance APK calls these production entry points.
# Preserve their JVM signatures while the remaining Release code stays optimized.
-keepclassmembers class com.osfans.trime.daemon.RimeDaemon {
    public static final com.osfans.trime.daemon.RimeDaemon INSTANCE;
    private com.osfans.trime.core.Rime getRealRime();
    public kotlinx.coroutines.flow.StateFlow getEngineState$app();
    public com.osfans.trime.daemon.RimeSession getFirstSessionOrNull();
    public com.osfans.trime.daemon.RimeSession createSession(java.lang.String);
    public void destroySession(java.lang.String);
}
-keep interface com.osfans.trime.daemon.RimeSession {
    public java.lang.Object run(kotlin.jvm.functions.Function2);
    public java.lang.Object runOnReady(kotlin.jvm.functions.Function2, kotlin.coroutines.Continuation);
}
-keepclassmembers class com.osfans.trime.data.base.DataManager {
    public static final com.osfans.trime.data.base.DataManager INSTANCE;
    public java.io.File getUserDataDir();
    public java.io.File getSharedDataDir();
}
-keepclassmembers class com.osfans.trime.data.prefs.AppPrefs {
    public static final com.osfans.trime.data.prefs.AppPrefs$Companion Companion;
    public com.osfans.trime.data.prefs.AppPrefs$Candidates getCandidates();
    public com.osfans.trime.data.prefs.AppPrefs$General getGeneral();
}
-keepclassmembers class com.osfans.trime.data.prefs.AppPrefs$Companion {
    public com.osfans.trime.data.prefs.AppPrefs defaultInstance();
}
-keepclassmembers class com.osfans.trime.data.prefs.AppPrefs$Candidates {
    public com.osfans.trime.data.prefs.PreferenceDelegate getNextWordPrediction();
}
-keepclassmembers class com.osfans.trime.data.prefs.AppPrefs$General {
    public com.osfans.trime.data.prefs.PreferenceDelegate$SerializableDelegate getInlinePreeditMode();
}
-keepclassmembers class com.osfans.trime.data.prefs.PreferenceDelegate {
    public android.content.SharedPreferences getSharedPreferences();
    public java.lang.String getKey();
    public java.lang.Object getValue();
    public void setValue(java.lang.Object);
}
-keepclassmembers class com.osfans.trime.data.prefs.PreferenceDelegate$SerializableDelegate {
    public java.lang.Object getValue();
    public void setValue(java.lang.Object);
}
-keepclassmembers enum com.osfans.trime.ime.core.InlinePreeditMode {
    public static kotlin.enums.EnumEntries getEntries();
}

# Use kotlinx-coroutines' bundled consumer rules for its Android providers.
# Extra keep rules for service types block R8's ServiceLoader rewrite into direct
# provider construction. Preserve service descriptors as R8 inputs; do not remove
# them from packaging before verifying the optimized dispatcher/handler code.

# remove kotlin null checks
-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
    static void checkNotNull(...);
    static void checkExpressionValueIsNotNull(...);
    static void checkNotNullExpressionValue(...);
    static void checkReturnedValueIsNotNull(...);
    static void checkFieldIsNotNull(...);
    static void checkParameterIsNotNull(...);
    static void checkNotNullParameter(...);
}

# Uncomment this to preserve the line number information for
# debugging stack traces.
-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile
