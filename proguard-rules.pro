-dontobfuscate
-keepattributes LineNumberTable,SourceFile,RuntimeVisibleAnnotations,AnnotationDefault,InnerClasses,EnclosingMethod

-keepclassmembers class * extends com.stormtroopercs.materialreader.io.WritableObject {
	*;
}

-keepclassmembers class * extends com.stormtroopercs.materialreader.jsonwrap.JsonObject$JsonDeserializable {
	*;
}

-keepclassmembers class com.stormtroopercs.materialreader.R { *; }
-keepclassmembers class com.stormtroopercs.materialreader.R$xml {	*; }
-keepclassmembers class com.stormtroopercs.materialreader.R$string {	*; }

-keepclassmembers class com.github.luben.zstd.* {
	*;
}

-if @kotlinx.serialization.Serializable class **
{
    static **$* *;
}

-keepnames class <1>$$serializer { # -keepnames suffices; class is kept when serializer() is kept.
    static <1>$$serializer INSTANCE;
}

# Needed for instrumented tests for some reason
-keep class com.google.common.util.concurrent.ListenableFuture { *; }
