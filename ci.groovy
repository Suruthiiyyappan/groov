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

def notifyStarted() {
    script {
        sh 'curl --user admin:3bea46c85aec47fea4fd719c20e2d856 --silent ${BUILD_URL}api/json | jq -r ".result"' >> result
        def BUILD_STATUS = result.getText();
        echo "${BUILD_STATUS}"

        emailext (
            to: 'suruthiiyappan@gmail.com',
            subject: "Build Status: ${BUILD_STATUS}",
            body: "Build Status: ${BUILD_STATUS}",
            recipientProviders: [[$class: 'DevelopersRecipientProvider']]
        )
    }
}

return this

// def notifyFailed() {

//   emailext (
//       to: 'suruthiiyappan@gmail.com',
//       subject: "Failed: Job ${BUILD_NUMBER}",
//       body: "Failed",
//       recipientProviders: [[$class: 'DevelopersRecipientProvider']]
//     )
// }



// https://www.jenkins.io/blog/2016/07/18/pipeline-notifications/
//  def createPipeline(PIPELINE_PATH, SCM_USER, IS_MULE) {
//     withEnv(["PATH+MAVEN=/opt/apache-maven-3.9.5/bin"]) {
//         stage('Build') {
//             if (isUnix()) {
//                 if (!IS_MULE) {
//                     echo 'Building dependency modules'
//                     // Deploying the jar as spring boot generates two jars, one for mule and one for spring
//                     sh "mvn clean install"
//                 } else {
//                     echo 'Not building dependency modules'
//                 }
//             }
//             archiveArtifacts artifacts: '**/target/*.*ar', onlyIfSuccessful: true, fingerprint: true, allowEmptyArchive: true
//             archiveArtifacts artifacts: '**/target/*.zip', onlyIfSuccessful: true, fingerprint: true, allowEmptyArchive: true
//         }
        
//         // Call the unittest function
//         unittest()
//     }
// }

// def unittest() {
//     stage('Unit Test') {
//         echo "Archiving unit test results"
//         step([$class: 'JUnitResultArchiver', allowEmptyResults: true, testResults: '**/target/surefire-reports/*.xml'])
//     }
// }

// return this
