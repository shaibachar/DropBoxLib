#!/usr/bin/env groovy

node {
  jdk = tool name: 'JDK11'
    env.JAVA_HOME = "${jdk}"

      echo "jdk installation path is: ${jdk}"

        // next 2 are equivalents
    sh "${jdk}/bin/java -version"

    // note that simple quote strings are not evaluated by Groovy
     // substitution is done by shell script using environment
    sh '$JAVA_HOME/bin/java -version'

    stage('checkout') {
        checkout scm
    }

    stage('check java') {
        sh "java -version"
    }

    stage('clean') {
        sh "chmod +x mvnw"
        sh "./mvnw clean"
    }

    stage('backend tests') {
        try {
            sh "./mvnw test"
        } catch(err) {
            throw err
        } finally {
//            junit '**/target/surefire-reports/TEST-*.xml'
        }
    }

    stage('packaging') {
        sh "./mvnw verify -Pprod -DskipTests"
	sh "./mvnw install -Pprod -DskipTests"
    }

}
