node {
    def appId = 19726

    properties([pipelineTriggers([pollSCM('* * * * *')])])

    sh "kubectl config use-context preprod-fss"

    sh "git clone https://github.com/navikt/github-apps-support.git || (cd github-apps-support && git pull)"

    def cwd = sh(script: 'pwd', returnStdout: true).trim()

    withEnv(["PATH+GITHUB_APPS_SUPPORT=${cwd}/github-apps-support/bin"]) {
        stage("${REPO_NAME}") {
            dir("${REPO_NAME}") {
                def token
                withCredentials([file(credentialsId: 'teamHelseGithubApp', variable: 'privateKey')]) {
                    def jwt = sh(script: "generate-jwt.sh ${privateKey} ${appId}", returnStdout: true).trim()

                    withEnv(['HTTPS_PROXY=http://webproxy-utvikler.nav.no:8088', "JWT=${jwt}"]) {
                        token = sh(script: 'generate-installation-token.sh $JWT', returnStdout: true).trim()
                    }
                }

                def scmVars = checkout([
                        $class: 'GitSCM',
                        branches: [
                                [name: 'refs/tags/**']
                        ],
                        userRemoteConfigs: [
                                [url: "https://github.com/navikt/${REPO_NAME}.git"]
                        ]
                ])

                println "SCM vars:"
                println groovy.json.JsonOutput.toJson(scmVars);

                if (scmVars.GIT_PREVIOUS_COMMIT && scmVars.GIT_COMMIT == scmVars.GIT_PREVIOUS_COMMIT) {
                    println "Skipping because we have alreayd built this commit"
                } else {
                    if (fileExists('naiserator.yaml')) {
                        println "Skipping because naiserator.yaml does not exist"
                    } else {
                        def currentTag = sh(script: 'git describe --tags --abbrev=0', returnStdout: true).trim()
                        def ctx = sh(script: "kubectl config current-context", returnStdout: true).trim()

                        def response = createDeployment(token, "navikt/${REPO_NAME}", currentTag, ctx, "deploy ${REPO_NAME} to ${ctx}")
                        deploymentId = response.id
                        createDeploymentStatus(token, "navikt/${REPO_NAME}", deploymentId, "pending")

                        println "Deploying ${currentTag} ${REPO_NAME} to cluster ${ctx}"

                        def retval = sh script: "kubectl apply -f naiserator.yaml", returnStatus: true

                        createDeploymentStatus(token, "navikt/${REPO_NAME}", deploymentId, "in_progress")

                        if (retval != 0) {
                            println "Deploy not successful"
                            createDeploymentStatus(token, "navikt/${REPO_NAME}", deploymentId, "failure")
                        } else {
                            println "Deploy ok"
                            createDeploymentStatus(token, "navikt/${REPO_NAME}", deploymentId, "success")
                        }
                    }
                }
            }
        }
    }
}

// createDeployment("credentialId", "navikt/myrepo", commitSha, "production", "deploy to production")
// createDeployment("credentialId", "navikt/myrepo", "feature/branch", "qa", "deploy to qa")
def createDeployment(token, reposlug, ref, environment, description) {
    def postBody = [
            ref: ref,
            auto_merge: false,
            required_contexts: [],
            environment: environment,
            description: description
    ]

    def postBodyString = groovy.json.JsonOutput.toJson(postBody)

    withEnv(['HTTPS_PROXY=http://webproxy-utvikler.nav.no:8088']) {
        responseBody = sh(script: """
            curl -H 'Authorization: Bearer ${token}' \
                -H 'Content-Type: application/json' \
                -H 'Accept: application/vnd.github.ant-man-preview+json' \
                -X POST \
                -d '${postBodyString}' \
                https://api.github.com/repos/${reposlug}/deployments
        """, returnStdout: true).trim()

        def slurper = new groovy.json.JsonSlurperClassic()
        return slurper.parseText(responseBody);
    }
}

// createDeploymentStatus("credentialId", "navikt/myrepo", 12345, "pending")
// createDeploymentStatus("credentialId", "navikt/myrepo", 12345, "error")
// createDeploymentStatus("credentialId", "navikt/myrepo", 12345, "failure")
// createDeploymentStatus("credentialId", "navikt/myrepo", 12345, "success")
def createDeploymentStatus(token, reposlug, deployId, state) {
    def postBody = [
            state: state,
            log_url: env.BUILD_URL
    ]

    def postBodyString = groovy.json.JsonOutput.toJson(postBody)

    withEnv(['HTTPS_PROXY=http://webproxy-utvikler.nav.no:8088']) {
        responseBody = sh(script: """
            curl -H 'Authorization: Bearer ${token}' \
                -H 'Content-Type: application/json' \
                -H 'Accept: application/vnd.github.flash-preview+json' \
                -X POST \
                -d '${postBodyString}' \
                https://api.github.com/repos/${reposlug}/deployments/${deployId}/statuses
        """, returnStdout: true).trim()
        def slurper = new groovy.json.JsonSlurperClassic()
        return slurper.parseText(responseBody);
    }
}
