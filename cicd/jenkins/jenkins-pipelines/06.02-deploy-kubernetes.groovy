def label = "mypod-${UUID.randomUUID().toString()}"
podTemplate(label: label, containers: [
        containerTemplate(name: 'git', image: 'alpine/git', ttyEnabled: true, command: 'cat'),
        containerTemplate(name: 'gcloud', image: 'greenwall/gcloud-kubectl-helm', command: 'cat', ttyEnabled: true)
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

        stage('Road To New Version') {
            git branch:'master', url:'https://github.com/acedemand/gke-advanced-features.git'
            container('git'){
                stage('Extract Git ID'){
                    sh "git rev-parse --short HEAD > .git/commit-id"
                    IMAGETAG = readFile('.git/commit-id').trim()
                    DEPLOYMENTNAME="frontend-${IMAGETAG}"
                }
            }

            container('gcloud'){
            sh """
                apk update
                apk add jq
                gcloud auth activate-service-account jenkins-sa@${PROJECT_ID}.iam.gserviceaccount.com --key-file=/root/key.json --project=${PROJECT_ID}
                gcloud container clusters get-credentials test-cluster --zone us-central1-a --project ${PROJECT_ID}
                kubectl get deployment -n frontend
                kubectl scale deployment -n frontend --replicas=3 $DEPLOYMENTNAME
                READY=\$(kubectl get deploy -n frontend $DEPLOYMENTNAME -o json | jq '.status.conditions[] | select(.reason == "MinimumReplicasAvailable") | .status' | tr -d '"')
                while [[ "\$READY" != "True" ]]; do
                    READY=\$(kubectl get deploy -n frontend $DEPLOYMENTNAME -o json | jq '.status.conditions[] | select(.reason == "MinimumReplicasAvailable") | .status' | tr -d '"')
                    sleep 5
                done
                kubectl delete -n frontend deployment \$(kubectl get deployments -n frontend  --sort-by=.metadata.creationTimestamp   -o jsonpath="{.items[0].metadata.name}")
               """
            }
        }
      }
    }