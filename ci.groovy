#!groovy

def createPipeline(PIPELINE_PATH, SCM_USER, IS_MULE) {
    withEnv(["PATH+MAVEN=/opt/apache-maven-3.9.5/bin"]) {
        script {
            if (isUnix()) {
                if (!IS_MULE) {
                    echo 'Building dependency modules'
                    // Deploying the jar as spring boot generates two jars, one for mule and one for spring
                    sh "mvn clean install"
                } else {
                    echo 'Not building dependency modules'
                }
            }
            archiveArtifacts artifacts: '**/target/*.*ar', onlyIfSuccessful: true, fingerprint: true, allowEmptyArchive: true
            archiveArtifacts artifacts: '**/target/*.zip', onlyIfSuccessful: true, fingerprint: true, allowEmptyArchive: true
        }
        unittest()

    }
}

def unittest() {
    stage("unit test") {
        echo "Archiving unit test results"
        step([$class: 'JUnitResultArchiver', allowEmptyResults: true, testResults: '**/target/surefire-reports/*.xml'])
    }
}

def notifyFailed() {
  // send to email
  emailext (
            to: "suruthiiyappan@gmail.com",
            subject: "Failed: Job",
            body: "$BUILD_NUMBER",
            recipientProviders: [[$class: 'DevelopersRecipientProvider']]
    )
}

return this
