def repos = ["helse-sykepengebehandling", "helse-sykepengesoknadfilter", "helse-soknadsvalidator"]

repos.each {
    pipelineJob("${it}") {
        definition {
            cps {
                scriptFile = readFileFromWorkspace('deploy-pipeline.groovy').replace("${REPO_NAME}", "${it}")
                script(scriptFile)
            }
        }
    }
}
