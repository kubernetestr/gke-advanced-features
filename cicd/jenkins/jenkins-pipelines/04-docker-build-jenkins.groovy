def label = "mypod-${UUID.randomUUID().toString()}"
podTemplate(label: label, containers: [
        containerTemplate(name: 'docker', image: 'docker', command: 'cat', ttyEnabled: true),
        containerTemplate(name: 'git', image: 'alpine/git', ttyEnabled: true, command: 'cat'),
        containerTemplate(name: 'gcpdocker', image: 'paulwoelfel/docker-gcloud', ttyEnabled: true, command: 'cat'),
        containerTemplate(name: 'maven', image: 'maven:3.6.0-jdk-8-slim', command: 'cat', ttyEnabled: true),

    ],
    volumes: [
        hostPathVolume(mountPath: '/var/run/docker.sock', hostPath: '/var/run/docker.sock'),
        secretVolume(secretName: 'k8s-developer-sa', mountPath: '/root/')
        ],
    envVars: [
            envVar(key: 'MAVEN_CONFIG', value: '/var/maven/.m2 '),
        ],
    ){
    node(label) {
        def GIT_ID = ""
        def PROJECT_ID="inspired-bus-194216"
        stage('Ace GIT') {
            //git credentialsId: 'acegitcredentials', branch:'master', url:'https://github.com/dstar55/docker-hello-world-spring-boot.git'
            git branch:'master', url:'https://github.com/acedemand/gke-advanced-features.git'
            container('git'){
                stage('Extract Git ID'){
                    sh "git rev-parse --short HEAD > .git/commit-id"
                    IMAGETAG = readFile('.git/commit-id').trim()
                }
            }
        }
        stage('Build Source Code') {
            container('maven') {
               sh """
                    mvn clean package -f cicd/jenkins/docker-hello-world-spring-boot/pom.xml -Dmaven.test.skip=true -Duser.home=/var/maven
                    cp ./cicd/jenkins/docker-hello-world-spring-boot/target/hello*.jar ./cicd/jenkins/docker-hello-world-spring-boot/data 
                """
            }
        }

        stage('Build Image') {
            container('docker') {
               sh """
                    docker build -f cicd/jenkins/docker-hello-world-spring-boot/Dockerfile -t gcr.io/${PROJECT_ID}/spring-helloworld:latest .
                    docker tag gcr.io/${PROJECT_ID}/spring-helloworld:latest gcr.io/${PROJECT_ID}/spring-helloworld:${IMAGETAG}
                """
            }
        }
        stage('Push Image'){
            container('gcpdocker'){
                sh """
                    gcloud auth activate-service-account jenkins-sa@${PROJECT_ID}.iam.gserviceaccount.com --key-file=/root/key.json --project=${PROJECT_ID}
                    gcloud docker -- push  gcr.io/${PROJECT_ID}/spring-helloworld:latest
                    gcloud docker -- push  gcr.io/${PROJECT_ID}/spring-helloworld:${IMAGETAG}
                 """
            }
        }
      }
    }
