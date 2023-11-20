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
        buildstatus()

    }
}

def unittest() {
    stage("unit test") {
        echo "Archiving unit test results"
        step([$class: 'JUnitResultArchiver', allowEmptyResults: true, testResults: '**/target/surefire-reports/*.xml'])
    }
}

def buildstatus() {
    script {
    post {
        failure {
            mail body: "Check console output of User-Registration at ${BUILD_URL}/console to find the error.", 
                    to: "${EMAIL_TO}", 
                    subject: "Build failed in Jenkins: #$BUILD_NUMBER"
        }
        success {
            mail body: "Check console output of User-Registration at ${env.BUILD_URL}/console to view the results.", 
                    to: "${EMAIL_TO}", 
                    subject: "Jenkins build is succeed: #$BUILD_NUMBER"
        }
    }
    }
}

return this
