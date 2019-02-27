node {
    properties([pipelineTriggers([pollSCM('* * * * *')])])

    stage("checkout") {
        def scmVars = checkout([
                $class           : 'GitSCM',
                branches         : [
                        [name: '*/master']
                ],
                userRemoteConfigs: [
                        [url: "ssh://git@github.com/navikt/helse-iac.git"]
                ]
        ])

        println "SCM vars:"
        println groovy.json.JsonOutput.toJson(scmVars)
    }
    stage("preprod-sbs") {
        sh "kubectl config use-context preprod-sbs"
        sh "find ./preprod -name naiserator-sbs.yaml -print0 | xargs -0 -n1 kubectl apply -f"
        sh "find ./preprod -name alerts-sbs.yaml -exec kubectl -f {} \\;"
    }

    stage("prod-sbs") {
        sh "kubectl config use-context prod-sbs"
        sh "find ./prod -name naiserator-sbs.yaml -print0 | xargs -0 -n1 kubectl apply -f"
        sh "find ./prod -name alerts-sbs.yaml -exec kubectl -f {} \\;"
    }

    stage("preprod-fss") {
        sh "kubectl config use-context preprod-fss"
        sh "find ./preprod -name naiserator.yaml -print0 | xargs -0 -n1 kubectl apply -f"
        sh "find ./preprod -name alerts.yaml -exec kubectl -f {} \\;"
    }

    stage("prod-fss") {
        sh "kubectl config use-context prod-fss"
        sh "find ./prod -name naiserator.yaml -print0 | xargs -0 -n1 kubectl apply -f"
        sh "find ./prod -name alerts.yaml -exec kubectl -f {} \\;"
    }
}
