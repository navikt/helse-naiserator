def repos = ["helse-sykepengebehandling", "helse-sykepengesoknadfilter", "helse-soknadsvalidator"]

repos.each {
    REPO_NAME="${it}"
    pipelineJob("${REPO_NAME}") {
        definition {
            cps {
                scriptFile = readFileFromWorkspace('deploy-pipeline.groovy').replace('${REPO_NAME}', "${REPO_NAME}")
                script(scriptFile)
            }
        }
    }
}
