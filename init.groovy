def repos = ["helse-sykepengebehandling", "helse-sykepengesoknadfilter", "helse-soknadsvalidator"]

repos.each {
    pipelineJob("${it}") {
        parameters {
            stringParam("REPO_NAME", "${it}")
        }

        definition {
            cps {
                script(readFileFromWorkspace('deploy-pipeline.groovy'))
            }
        }
    }
}
