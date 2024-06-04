package io.jenkins.plugins;

import hudson.Extension;
import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.jenkinsci.plugins.workflow.steps.SynchronousNonBlockingStepExecution;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import com.google.inject.Inject;

public class BuildPulseStep extends Step {
    String accountId;
    String repositoryId;
    String junitXMLPaths;
    String keyId; // lgtm[jenkins/plaintext-storage]
    String secret;
    String commitSHA;
    String branch;

    // optional
    String repositoryPath;
    String coveragePaths;
    String tags;
    String quota;

    @DataBoundConstructor
    public BuildPulseStep(String account, String repository, String path, String key, String secret, String commit, String branch) {
        this.accountId = account;
        this.repositoryId = repository;
        this.junitXMLPaths = path;
        this.keyId = key;
        this.secret = secret;
        this.commitSHA = commit;
        this.branch = branch;
    }

    @DataBoundSetter
    public void setRepositoryPath(String repositoryPath) {
        this.repositoryPath = repositoryPath;
    }

    @DataBoundSetter
    public void setCoveragePaths(String coveragePaths) {
        this.coveragePaths = coveragePaths;
    }

    @DataBoundSetter
    public void setTags(String tags) {
        this.tags = tags;
    }

    @DataBoundSetter
    public void setQuota(String quota) {
        this.quota = quota;
    }

    @Override
    public StepExecution start(StepContext context) throws Exception {
        return new Execution(context, this);
    }

    public static class Execution extends SynchronousNonBlockingStepExecution<Void> {
        private static final long serialVersionUID = 1L;

        private final transient BuildPulseStep step;

        @Inject
        public Execution(StepContext context, BuildPulseStep step) {
            super(context);
            this.step = step;
        }

        @Override
        protected Void run() throws IOException, InterruptedException {
            TaskListener listener = getContext().get(TaskListener.class);

            String workspacePath = this.getContext().get(FilePath.class).getRemote();
            if (this.step.repositoryPath == null) {
                this.step.repositoryPath = workspacePath;
            }

            String[] submitCommand = BuildPulseCommand.submit(this.step.accountId, this.step.repositoryId, this.step.junitXMLPaths, this.step.repositoryPath, this.step.keyId, this.step.secret, workspacePath, this.step.coveragePaths, this.step.tags, this.step.quota);

            // Get the build URL
            String buildUrl = this.getContext().get(Run.class).getEnvironment(listener).get("BUILD_URL");

            ProcessBuilder processBuilder = new ProcessBuilder(submitCommand);
            processBuilder.environment().put("BUILDPULSE_ACCESS_KEY_ID", this.step.keyId);
            processBuilder.environment().put("BUILDPULSE_SECRET_ACCESS_KEY", this.step.secret);
            processBuilder.environment().put("BUILD_URL", buildUrl);
            processBuilder.environment().put("GIT_COMMIT", this.step.commitSHA);
            processBuilder.environment().put("GIT_BRANCH", this.step.branch);
            processBuilder.environment().put("GIT_URL", "github.com/owner/repo");

            // Start the process
            listener.getLogger().println("Executing: " + String.join(" ", submitCommand));
            Process buildpulseProcess = processBuilder.start();

            String output = IOUtils.toString(buildpulseProcess.getInputStream(), StandardCharsets.UTF_8);
            String errorOutput = IOUtils.toString(buildpulseProcess.getErrorStream(), StandardCharsets.UTF_8);

            // Wait for the process to complete
            int exitCode = buildpulseProcess.waitFor();

            // Print the output
            String printString = exitCode == 0 ? output : errorOutput;
            listener.getLogger().println(printString);
            listener.getLogger().println("Exit Code: " + exitCode);

            return null;
        }
    }

    @Extension
    public static class DescriptorImpl extends StepDescriptor {

        public DescriptorImpl() {
            System.out.println("BuildPulseStep Descriptor Loaded");
        }

        @Override
        public String getFunctionName() {
            return "buildpulseStep";
        }

        @Override
        public String getDisplayName() {
            return "BuildPulse Step";
        }

        @Override
        public Set<? extends Class<?>> getRequiredContext() {
            return Collections.singleton(TaskListener.class);
        }

        @Override
        public boolean isAdvanced() {
            return false;
        }
    }
}
