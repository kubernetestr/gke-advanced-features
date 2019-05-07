def label = "mypod-${UUID.randomUUID().toString()}"
podTemplate(label: label, containers: [
        containerTemplate(name: 'docker', image: 'docker', command: 'cat', ttyEnabled: true)
    ],
    volumes: [
        hostPathVolume(mountPath: '/var/run/docker.sock', hostPath: '/var/run/docker.sock')
        ],
    ){
    node(label) {
        stage('Ace Docker') {

            container('docker') {
               sh """
                    docker ps -a
                    docker images
                    dokcer ps
                """
            }
      }
    }
}
