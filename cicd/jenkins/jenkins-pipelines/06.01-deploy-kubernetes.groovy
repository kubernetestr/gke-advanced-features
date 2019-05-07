def label = "mypod-${UUID.randomUUID().toString()}"
podTemplate(label: label, containers: [
        containerTemplate(name: 'docker', image: 'docker', command: 'cat', ttyEnabled: true),
        containerTemplate(name: 'git', image: 'alpine/git', ttyEnabled: true, command: 'cat'),
        containerTemplate(name: 'gcloud', image: 'greenwall/gcloud-kubectl-helm', command: 'cat', ttyEnabled: true),
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
        stage('Ace CI') {
            //git credentialsId: 'acegitcredentials', branch:'master', url:'https://github.com/dstar55/docker-hello-world-spring-boot.git'
            git branch:'master', url:'https://github.com/acedemand/gke-advanced-features.git'
            container('git'){
                stage('Extract Git ID'){
                    sh "git rev-parse --short HEAD > .git/commit-id"
                    IMAGETAG = readFile('.git/commit-id').trim()
                }
            }

        container('maven') {
           sh """
                mvn clean package -f cicd/jenkins/docker-hello-world-spring-boot/pom.xml -Dmaven.test.skip=true -Duser.home=/var/maven
                cp ./cicd/jenkins/docker-hello-world-spring-boot/target/hello*.jar ./cicd/jenkins/docker-hello-world-spring-boot/data 
            """
        }
        container('docker') {
           sh """
                docker build -f cicd/jenkins/docker-hello-world-spring-boot/Dockerfile -t gcr.io/${PROJECT_ID}/spring-helloworld:latest .
                docker tag gcr.io/${PROJECT_ID}/spring-helloworld:latest gcr.io/${PROJECT_ID}/spring-helloworld:${IMAGETAG}
            """
        }

        stage('Ace CD') {
            container('gcpdocker'){
                sh """
                    gcloud auth activate-service-account jenkins-sa@${PROJECT_ID}.iam.gserviceaccount.com --key-file=/root/key.json --project=${PROJECT_ID}
                    gcloud docker -- push  gcr.io/${PROJECT_ID}/spring-helloworld:latest
                    gcloud docker -- push  gcr.io/${PROJECT_ID}/spring-helloworld:${IMAGETAG}
                 """
            }
            container('gcloud'){
                sh  "sed -i -e 's/#VERSION#/${IMAGETAG}/g' ./cicd/jenkins/docker-hello-world-spring-boot/k8s/canary/deployment.yaml"
                sh  "sed -i -e 's/#PROJECTID#/${PROJECT_ID}/g' ./cicd/jenkins/docker-hello-world-spring-boot/k8s/canary/deployment.yaml"
                //sh  "sed -i -e 's/#VERSION#/${IMAGETAG}/g' ./k8s/service.yaml"
            }
            container('gcloud'){
            sh """
                gcloud auth activate-service-account jenkins-sa@${PROJECT_ID}.iam.gserviceaccount.com --key-file=/root/key.json --project=${PROJECT_ID}
                gcloud container clusters get-credentials test-cluster --zone us-central1-a --project ${PROJECT_ID}
                kubectl get deployment
                kubectl apply -n frontend -f ./cicd/jenkins/docker-hello-world-spring-boot/k8s/canary/service.yaml
                kubectl apply -n frontend -f ./cicd/jenkins/docker-hello-world-spring-boot/k8s/canary/deployment.yaml
               """
            }
        }
      }
    }
}