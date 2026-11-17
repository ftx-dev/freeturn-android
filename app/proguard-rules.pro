# ── Stacktraces ───────────────────────────────────────────────────────────────
# Сохраняем имена файлов и номера строк для читаемых крэш-репортов
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ── JSch ──────────────────────────────────────────────────────────────────────
# Reflection создаёт package-private классы JSch: вызывающий код должен оставаться в том же пакете.
-keep class com.jcraft.jsch.** { *; }
-dontwarn com.jcraft.jsch.**

# ── Bouncy Castle ─────────────────────────────────────────────────────────────
-dontwarn org.bouncycastle.**
-dontwarn javax.naming.**

# ── Ядро (gomobile) ───────────────────────────────────────────────────────────
# Go зовёт Java по именам классов и методов через Seq, дефолтного правила на
# native <methods> мало: без keep release падает в рантайме на Seq.
-keep class com.freeturn.core.** { *; }
-keep class go.** { *; }

# ── Compile-only аннотации (errorprone/javax) не включены в runtime ───────────
-dontwarn com.google.errorprone.annotations.CanIgnoreReturnValue
-dontwarn com.google.errorprone.annotations.CheckReturnValue
-dontwarn com.google.errorprone.annotations.Immutable
-dontwarn com.google.errorprone.annotations.RestrictedApi
-dontwarn javax.annotation.Nullable
-dontwarn javax.annotation.concurrent.GuardedBy

# ── FreeTurnX: Shizuku ────────────────────────────────────────────────────────
# Сервис раздачи создаёт процесс Shizuku через рефлексию (конструктор по имени класса):
# без keep R8 вырезал конструкторы и методы, и сервис «не подключался» в release.
-keep class com.freeturn.app.tether.TetherUserService { *; }
-keep class com.freeturn.app.tether.ITetherService** { *; }
