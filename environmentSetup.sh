
function copyEnvVarsToGradleProperties {
    echo "Its running......"
    echo $PATH
    # GRADLE_PROPERTIES="/Users/pprakash/Places/places-monitor-android/code/gradle.properties"
    # export GRADLE_PROPERTIES
    # echo "Gradle Properties should exist at $GRADLE_PROPERTIES"

    # if [ ! -f "$GRADLE_PROPERTIES" ]; then
    #     echo "Gradle Properties does not exist"
    # fi
     echo "Writing TEST_API_KEY to gradle.properties..."
     echo "TEST_API_KEY=value" >> $GRADLE_PROPERTIES
}
