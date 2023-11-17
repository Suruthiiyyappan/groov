#!groovy

def start(PIPELINE_PATH, SCM_USER, IS_MULE) {
     withEnv(["PATH+MVN=${tool 'mvn3'}/bin"]) {
          stage('Build') {  
                    if (isUnix()) {
                        if(!IS_MULE){
							echo 'Building dependency modules'
							//Deploying the jar as spring boot generates two jars one for mule and one for spring
							sh "mvn clean install"
                        }else{
                            echo 'not Building dependency modules'
                        }
                    } 
                    archiveArtifacts artifacts: '**/target/*.*ar', onlyIfSuccessful: true, fingerprint: true, allowEmptyArchive: true
                    archiveArtifacts artifacts: '**/target/*.zip', onlyIfSuccessful: true, fingerprint: true, allowEmptyArchive: true
    
          }
            // Run tests for the current code branch
            stage('Unit Test ') {
                echo "Archiving unit test results"
                step([$class: 'JUnitResultArchiver',allowEmptyResults: true,
                testResults: '**/target/surefire-reports/*.xml'])            

            }
def nexus() {
    // Read POM xml file using 'readMavenPom' step
    def pom = readMavenPom file: "pom.xml"

    // Find built artifact under the target folder
    def filesByGlob = findFiles(glob: "target/*.jar")

    // Print some info from the artifact found
    echo "${filesByGlob[0].name} ${filesByGlob[0].path} ${filesByGlob[0].directory} ${filesByGlob[0].length} ${filesByGlob[0].lastModified}"

    // Extract the path from the File found
    def artifactPath = filesByGlob[0].path

    // Assign to a boolean response verifying if the artifact exists
    def artifactExists = fileExists artifactPath

    if (artifactExists) {
        echo "*** File: ${artifactPath}, group: uk.co.danielbryant.djshopping, packaging: jar, version: 0.0.1-SNAPSHOT"

        nexusArtifactUploader(
            nexusVersion: "nexus3",
            protocol: "http",
            nexusUrl: "3.35.67.97:8081",
            groupId: "uk.co.danielbryant.djshopping",
            version: "0.0.1-SNAPSHOT",
            repository: "maven-snapshots",
            credentialsId: "nexus3",
            artifacts: [
                [
                    artifactId: "stockmanager",
                    classifier: '',
                    file: "target/stockmanager-0.0.1-SNAPSHOT.jar",
                    type: "jar"
                ]
            ]
        )
    }
}
			if(!IS_MULE){
				// Run Static Code Analysis
				stage('Static Code Analysis ') {  
				echo "Running static code analysis"
					if (isUnix()) {
						echo 'Building project'
                        withEnv(["PATH+MVN=${tool 'mvn3'}/bin"]){
						withSonarQubeEnv(credentialsId: 'sonar-alm-token', installationName: 'Ross-Sonar-NEWPR') {
                            def sonar_url = 'http://3.97.203.42:9000/'
                            def sonar_name = 'test'

                            sh """mvn clean verify sonar:sonar \\
                            -Dsonar.exclusions=pom.xml \\
                            -Dsonar.host.url=${sonar_url} \\
                            -Dsonar.projectName=test-${BUILD_NUMBER}"""
						}
                    }
					} else {
						echo 'Building project'
						bat "mvn sonar:sonar -Dsonar.login=1fe00a8d7708292f457b144bce028120bcd9b35d"
					}
				}
	
							// No need to occupy a node
				stage("Quality Gate"){
					//withCredentials([usernameColonPassword(credentialsId: 'SonarAccess', variable: 'Jenkins-Sonar')]) {
					////////////withSonarQubeEnv('Ross-SonarTesting') {
                    withEnv(["JAVA_HOME=${tool 'jdk11'}", "PATH+MVN=${tool 'mvn3'}/bin"]){
					withSonarQubeEnv(credentialsId: 'sonar-alm-token', installationName: 'Ross-Sonar-NEWPR') {
						try {
                            timeout(time: 1, unit: 'HOURS') { 
                                waitForQualityGate abortPipeline: true
                                script {
                                    def qg = waitForQualityGate()
                                    if (qg.status != 'OK') {
                                        error "Quality gate not OK: ${qg.status}"
                                    }
                                }
                            }
							echo "Quality Gate Successful"

						}catch (err) { // timeout reached
							echo err.toString()
							echo "Timeout occured due to Quality Gate failure"
							currentBuild.result = 'FAILURE'
							throw err
						}
					}	
                }
				}
			}else {
				echo "Skipping Static Code Analysis for API"
			}
				
            stage('Deploy to Dev') {
                if(IS_MULE){
                    echo "Deploying to Mule"
                withCredentials([usernamePassword(credentialsId: 'mule-deploy-id', passwordVariable: 'anypointPassword', usernameVariable: 'anypointUserName')]) {
                    if (isUnix()) {
                        echo 'Building project'
                        sh "if [[ `grep -c '<muleVersion>4' pom.xml` -ne 0 ]]; then echo 'Deploying Mule version 4.X' ; mvn ${MAVEN_OPTS} -Dconnected.app.clientId=ef5a62a2041645f882bf008955b63e38 -Dconnected.app.clientSecret=A0a6c35EbCd74A1B8DfC50D5B9395fe7 -DrepositoryId=my-anypoint-credentials -Denv=dev deploy -DskipTests -Dmuledeploy ; else echo 'Deploying Mule version 3.X' ; mvn ${MAVEN_OPTS} -Du=$anypointUserName -Dp=$anypointPassword -Denv=dev deploy ; fi"          

                    } else {
                       echo 'Building project'
                        bat "mvn -Dhttp.proxyHost=proxy.ros.com -Dhttp.proxyPort-80 -Du=$anypointUserName -Dp=anypointPassword -Denv=dev deploy"
                    }
                }
                }else{
                    echo "Skipping deployment"
                }              

            }

    }
}




/***
    Deploy to higher environment based on user input
**/
def deployToMule(env, IS_MULE){   
 def MAVEN_OPTS = '-B -Dmaven.test.failure.ignore=true -Dhttp.nonProxyHosts=*.ros.com -Dhttp.proxyHost=proxy.ros.com -Dhttp.proxyPort=80 -Dhttps.proxyHost=proxy.ros.com -Dhttps.proxyPort=443' 
                // Deploy application to server
            stage('Deploy to '+env) {
                withEnv(["JAVA_HOME=${tool 'jdk8u282'}", "PATH+MVN=${tool 'mvn3'}/bin"]) {

                if(IS_MULE){
                    echo "Deploying to Mule"
                withCredentials([usernamePassword(credentialsId: 'mule-deploy-id', passwordVariable: 'anypointPassword', usernameVariable: 'anypointUserName')]) {
                    echo "Deploying Services to AnyPoint platform"
                    if (isUnix()) {
                        echo 'Building project'
                        sh "if [[ `grep -c '<muleVersion>4' pom.xml` -ne 0 ]]; then echo 'Deploying Mule version 4.X' ; mvn ${MAVEN_OPTS} -Dconnected.app.clientId=ef5a62a2041645f882bf008955b63e38 -Dconnected.app.clientSecret=A0a6c35EbCd74A1B8DfC50D5B9395fe7 -DrepositoryId=my-anypoint-credentials -Denv=${env} deploy -DskipTests -Dmuledeploy ; else echo 'Deploying Mule version 3.X' ; mvn ${MAVEN_OPTS} -Du=$anypointUserName -Dp=$anypointPassword -Denv=${env} deploy ; fi"  

                    } else {
                        echo 'Building project'
                        bat "mvn -Dhttp.proxyHost=proxy.ros.com -Dhttp.proxyPort-80 -Du=$anypointUserName -Dp=anypointPassword -Denv=+"env+" deploy"
                    }
                }
                }else{
                    echo "Skipping deployment"
                }
                }              

            } 
}
return this
