
function copyEnvVarsToGradleProperties {
    echo "Its running......"
    echo $HOME
    GRADLE_PROPERTIES=$HOME"/code/code/gradle.properties"
    export GRADLE_PROPERTIES
    echo "Gradle Properties should exist at $GRADLE_PROPERTIES"

    if [ ! -f "$GRADLE_PROPERTIES" ]; then
        echo "Gradle Properties does not exist"
    fi
     echo "Writing TEST_API_KEY to gradle.properties..."
     echo "Wrtiting artifactoryUrl=$artifactoryUrl.."
     echo "artifactoryUrl=$artifactoryUrl" >> $GRADLE_PROPERTIES
     echo "artifactoryContextUrl=$artifactoryContextUrl" >> $GRADLE_PROPERTIES
     echo "artifactorySnapshotUrl=$artifactorySnapshotUrl" >> $GRADLE_PROPERTIES
     echo "artifactoryRepoKey=$artifactoryRepoKey" >> $GRADLE_PROPERTIES
     echo "artifactoryUserName=$artifactoryUserName" >> $GRADLE_PROPERTIES
     echo "artifactoryPassword=$artifactoryPassword" >> $GRADLE_PROPERTIES
}
