def repos = ["helse-sykepengebehandling", "helse-sykepengesoknadfilter", "helse-soknadsvalidator"]

repos.each {
    workflowJob("${it}") {
        environmentVariables {
            env('REPO_NAME', "${it}")
        }

        definition {
            cps {
                script(readFileFromWorkspace('deploy-pipeline.groovy'))
            }
        }
    }
}
