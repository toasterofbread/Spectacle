-dontobfuscate
-keep class com.toasterofbread.spectacle.** { *; }

-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# From proguard-android-optimize.txt
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
-optimizationpasses 5
-allowaccessmodification
-dontpreverify
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-verbose
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}
