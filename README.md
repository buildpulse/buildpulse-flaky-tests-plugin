
# Jenkins for BuildPulse [![Jenkins license](https://img.shields.io/badge/license-MIT-blue.svg)](./LICENSE)
[BuildPulse](https://buildpulse.io/products/flaky-tests) is a CI observability and optimization platform that helps you find and fix flaky tests instantly, as well as help cut test times in half.

This Jenkins plugin works with our free tier (+ others), and allows you to easily connect your Jenkins CI workflows to BuildPulse.

## Usage

1. Locate the BuildPulse credentials for your account at [buildpulse.io][]
2. In your Jenkins settings, create a [username and password credential](https://www.jenkins.io/doc/book/using/using-credentials/) with the ID:`buildpulse-credentials`, username:`BUILDPULSE_ACCESS_KEY_ID` and password:`BUILDPULSE_SECRET_ACCESS_KEY` (values can be found in the BuildPulse Dashboard)
3. Populate the `BUILDPULSE_ACCOUNT_ID` and `BUILDPULSE_REPOSITORY_ID` in your Jenkinsfile (values can be found in the BuildPulse Dashboard)
4. Add a step to your Jenkins workflow to use this plugin to send your test results to BuildPulse:
```
post {
  always {
    withCredentials([usernamePassword(credentialsId: 'buildpulse-credentials', usernameVariable: 'BUILDPULSE_ACCESS_KEY_ID', passwordVariable: 'BUILDPULSE_SECRET_ACCESS_KEY')]) {
      buildpulseStep accountId: env.BUILDPULSE_ACCOUNT_ID, repositoryId: env.BUILDPULSE_REPOSITORY_ID, path: "/spec/reports", key: env.BUILDPULSE_ACCESS_KEY_ID, secret: env.BUILDPULSE_SECRET_ACCESS_KEY, commit: env.GIT_COMMIT, branch: env.GIT_BRANCH
    }
  }
}
```
## Inputs

### `account`

**Required** The unique numeric identifier for the BuildPulse account that owns the repository.

### `repository`

**Required** The unique numeric identifier for the repository being built.

### `path`

**Required** The path to the XML file(s) for the test results. Can be a directory (e.g., `test/reports`), a single file (e.g., `reports/junit.xml`), or a glob (e.g., `app/*/results/*.xml`).

### `keyCredentialId`

**Required** The Jenkins Credential (Username and Password) ID. For the credential, set the username to the value of `BUILDPULSE_ACCESS_KEY_ID` in the dashboard. Set the password to the value of `BUILDPULSE_SECRET_ACCESS_KEY` in the dashboard.

### `commit`

**Required** The SHA for the commit that produced the test results.

If your workflow checks out a _different_ commit than the commit that triggered the workflow, then use this input to specify the commit SHA that your workflow checked out. For example, if your workflow is triggered by the `pull_request`, but you customize the workflow to check out the pull request merge commit, then you'll want to set this input to the pull request HEAD commit SHA.

### `branch`

**Required** The branch of the current build.

### `repositoryPath`

_Optional_ The path to the local git clone of the repository (default: workspace directory).

### `coveragePaths`

_Optional_ The paths to the coverage file(s) for the test results (space-separated).

### `tags`

_Optional_ Tags to apply to this build (space-separated).

### `quota`

_Optional_ Quota ID to count this upload against. Please set on BuildPulse Dashboard first.

## Example Jenkinsfile
```
    pipeline {
    agent {
        docker {
            image 'ruby:3.0.5' // Specify the Ruby container image you want to use
            args '-u root' // Optionally, specify additional Docker container run arguments
        }
    }


    environment {
        GIT_REPO_URL = 'https://github.com/BuildPulseLLC/decentralized-forums' // Replace with your repository URL
        GIT_BRANCH = 'main' // Replace with your repository branch
        BUILDPULSE_ACCOUNT_ID = '' // Replace with your BuildPulse account ID
        BUILDPULSE_REPOSITORY_ID = '' // Replace with your BuildPulse repository ID
    }

    stages {
        stage('Clone Repository') {
            steps {
                script {
                    echo 'Cloning the repository...'
                    scmVars = git branch: "${env.GIT_BRANCH}", url: "${env.GIT_REPO_URL}", credentialsId: 'github'
                    env.GIT_COMMIT = scmVars.GIT_COMMIT
                }
            }
        }

        stage('Setup & Test') {
            // install dependencies & run tests
        }
    }

    post {
        always {
              buildpulseStep account: env.BUILDPULSE_ACCOUNT_ID, repository: env.BUILDPULSE_REPOSITORY_ID, path: "/spec/reports", keyCredentialId: 'buildpulse-credentials', commit: env.GIT_COMMIT, branch: env.GIT_BRANCH
        }
    }
}
```

[buildpulse.io]: https://buildpulse.io
