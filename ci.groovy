#!groovy

def createPipeline(PIPELINE_PATH, IS_MULE) {
    withEnv(["PATH+MAVEN=/opt/apache-maven-3.9.5/bin"]) {
        stage('Build') {
            if (isUnix()) {
                if (!IS_MULE) {
                    echo 'Building dependency modules'
                    // Deploying the jar as spring boot generates two jars, one for mule and one for spring
                    sh "${mvnCmd} clean install"
                } else {
                    echo 'Not building dependency modules'
                }
            }
            archiveArtifacts artifacts: '**/target/*.*ar', onlyIfSuccessful: true, fingerprint: true, allowEmptyArchive: true
            archiveArtifacts artifacts: '**/target/*.zip', onlyIfSuccessful: true, fingerprint: true, allowEmptyArchive: true
        }

        // Run tests for the current code branch
        stage('Unit Test ') {
            echo "Archiving unit test results"
            step([$class: 'JUnitResultArchiver', allowEmptyResults: true, testResults: '**/target/surefire-reports/*.xml'])
        }
    }
}
