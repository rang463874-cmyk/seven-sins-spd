#!/bin/bash
# Compile-check the game without Gradle, for sandboxes where Maven/Google repos are blocked
# but GitHub is reachable. Fetches libGDX / gdx-controllers / JSON-java sources once,
# builds them into a jar, then compiles SPD-classes + services + core against it.
#
#   bash tools/compilecheck/check.sh          # prints javac errors, then "exit: N"
#
# Work files live in $WORK (default ~/spd-compilecheck). On a normal PC just use
# Android Studio or ./gradlew instead.
set -e
SPD=$(cd "$(dirname "$0")/../.." && pwd)
WORK=${WORK:-$HOME/spd-compilecheck}
mkdir -p "$WORK" && cd "$WORK"

if [ ! -f libs/deps.jar ]; then
  [ -d gdxsrc ] || { git clone -q --depth 1 --branch 1.14.0 --filter=blob:none --sparse https://github.com/libgdx/libgdx.git gdxsrc
                     (cd gdxsrc && git sparse-checkout set gdx/src extensions/gdx-freetype/src); }
  [ -d gdxctl ] || git clone -q --depth 1 --branch 2.2.4 https://github.com/libgdx/gdx-controllers.git gdxctl
  [ -d jsonsrc ] || git clone -q --depth 1 --branch 20170516 https://github.com/stleary/JSON-java.git jsonsrc

  # gdx-jnigen-loader classes that libGDX references; stubs are enough for compiling
  mkdir -p stubs/com/badlogic/gdx/utils jsonpkg/org/json libs
  cat > stubs/com/badlogic/gdx/utils/Os.java <<'EOF'
package com.badlogic.gdx.utils;
public enum Os { Windows, Linux, MacOsX, Android, IOS }
EOF
  cat > stubs/com/badlogic/gdx/utils/SharedLibraryLoader.java <<'EOF'
package com.badlogic.gdx.utils;
public class SharedLibraryLoader {
  public static Os os = Os.Linux; public static boolean is64Bit = true; public static boolean isARM = false;
  public SharedLibraryLoader(){} public SharedLibraryLoader(String s){}
  public void load(String s){} public static synchronized void setLoaded(String s){} public static synchronized boolean isLoaded(String s){return true;}
  public String mapLibraryName(String s){return s;}
}
EOF
  cp jsonsrc/*.java jsonpkg/org/json/
  { find gdxsrc/gdx/src gdxsrc/extensions/gdx-freetype/src gdxctl/gdx-controllers-core/src jsonpkg stubs -name '*.java'; } > libsrcs.txt
  rm -rf libcls && mkdir libcls
  javac -nowarn -proc:none -encoding UTF-8 -d libcls @libsrcs.txt 2>&1 | grep -E "error" | head -20 || true
  (cd libcls && jar cf ../libs/deps.jar .)
fi

rm -rf spdcls && mkdir spdcls
{ find "$SPD/SPD-classes/src/main/java" "$SPD/services/src/main/java" "$SPD/core/src/main/java" -name '*.java'; } > spdsrcs.txt
javac -nowarn -proc:none -encoding UTF-8 -Xmaxerrs 200 -cp libs/deps.jar -d spdcls @spdsrcs.txt 2>&1 | grep -v "^Note:\|JAVA_TOOL" | head -80
echo "exit: ${PIPESTATUS[0]}"
echo "classes: $WORK/spdcls   deps: $WORK/libs/deps.jar"
