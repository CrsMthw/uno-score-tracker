# Gson populates DTOs via reflection (Unsafe), bypassing Kotlin constructors. Keep the config DTOs
# and their members so a minified release build doesn't strip fields and crash on a real null.
# Domain models are normalised (non-null) from these in the mappers — see playbook §2.
-keep class com.crsmthw.unotracker.data.config.dto.** { *; }
-keepattributes Signature
-keepattributes *Annotation*

# Gson internals
-dontwarn sun.misc.**
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken
