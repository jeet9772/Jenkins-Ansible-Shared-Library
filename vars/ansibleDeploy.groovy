def call(Map config = [:]) {

    stage('Clone') {
        git(
            branch: config.gitBranch ?: 'main',
            url: config.gitUrl
        )
    }

    def props = readProperties file: config.configFile

    def slackChannel = props.SLACK_CHANNEL_NAME
    def environment = props.ENVIRONMENT
    def codeBasePath = props.CODE_BASE_PATH
    def actionMessage = props.ACTION_MESSAGE
    def keepApprovalStage = props.KEEP_APPROVAL_STAGE.toBoolean()

    if (keepApprovalStage) {
        stage('User Approval') {
            input(
                message: "Approve Redis deployment to ${environment}?",
                ok: 'Deploy'
            )
        }
    }

    stage('Playbook Execution') {
        sh """
            ansible-playbook \
            -i '${config.inventory}' \
            '${config.playbook}' \
            -e environment='${environment}' \
            -e code_base_path='${codeBasePath}'
        """
    }

    stage('Notification') {
        slackSend(
            channel: slackChannel,
            message: actionMessage
        )
    }
}
