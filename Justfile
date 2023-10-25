clean:
  clojure -T:build clean

build: clean
  clojure -T:build uber

native-image:
  $GRAALVM_HOME/bin/native-image \
    -jar target/cli.jar \
    --no-fallback \
    --enable-preview \
    --features=clj_easy.graal_build_time.InitClojureClasses \
    -H:Name=target/cli \
    -H:ReflectionConfigurationFiles=./graal/reflect-config.json \
    -H:+ReportUnsupportedElementsAtRuntime \
    -H:+ReportExceptionStackTraces

build-native: build native-image

build-and-run: build
  java --enable-preview -jar target/cli.jar

run *args: 
  clojure -m k16.kdev.cli {{args}}
