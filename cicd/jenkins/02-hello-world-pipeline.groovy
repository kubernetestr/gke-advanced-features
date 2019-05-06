def label = "mypod-${UUID.randomUUID().toString()}"
podTemplate(label: label) {
    node(label) {
        stage('Run shell') {
            slackSend color: 'Blue', message: 'Hello World'
            sh 'echo hello world'
        }
    }
}
