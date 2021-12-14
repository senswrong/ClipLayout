#指定class模糊字典
-classobfuscationdictionary '/Users/sensyang/Library/Android/sdk/tools/proguard/dic-class.txt'
-repackageclasses default
-optimizationpasses 10
-keepattributes Exceptions,InnerClasses,Signature,Deprecated,*Annotation*,EnclosingMethod
# 混淆时所采用的算法
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*

-keepclasseswithmembernames class * {native <methods>;} # 保持 native 方法不被混淆
-keepclassmembers enum * {                  # 保持枚举 enum 类不被混淆
  public static **[] values();
  public static ** valueOf(java.lang.String);
}
-keep class * extends java.lang.Enum { *; }
-keep interface * extends java.lang.Enum { *; }
-keep class * extends java.lang.annotation.Annotation { *; }
-keep interface * extends java.lang.annotation.Annotation { *; }
-dontwarn android.support.**
-ignorewarnings
############################## Log ################################
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int d(...);
    public static int e(...);
}
-assumenosideeffects class java.io.PrintStream {
      public *** println(...);
      public *** print(...);
}
##############################  bean  ################################
-keepclassmembernames class * implements android.os.Parcelable{
    public static final android.os.Parcelable$Creator *;
}
##############################  okhttp3  ################################
-dontwarn javax.annotation.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase
-dontwarn org.codehaus.mojo.animal_sniffer.*
-dontwarn okhttp3.internal.platform.ConscryptPlatform
-keep class job.**{*;}
#######databinding
-keepclassmembers class * extends androidx.viewbinding.ViewBinding {<methods>;}