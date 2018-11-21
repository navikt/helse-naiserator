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

    stage("preprod") {
        sh "kubectl config use-context preprod-fss"
        sh "find ./preprod -name naiserator.yaml -print0 | xargs -0 -n1 kubectl apply -f"
    }

    stage("prod") {
        sh "kubectl config use-context prod-fss"
        sh "find ./prod -name naiserator.yaml -print0 | xargs -0 -n1 kubectl apply -f"
    }
}

