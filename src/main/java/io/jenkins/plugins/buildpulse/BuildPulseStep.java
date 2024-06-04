package io.jenkins.plugins.buildpulse;

import hudson.Extension;
import hudson.FilePath;
import hudson.FilePath.FileCallable;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import hudson.security.ACL;
import jenkins.model.Jenkins;
import jenkins.security.Roles;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.jenkinsci.plugins.workflow.steps.SynchronousNonBlockingStepExecution;
import org.jenkinsci.remoting.RoleChecker;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.google.inject.Inject;

public class BuildPulseStep extends Step {
    String accountId;
    String repositoryId;
    String junitXMLPaths;
    String keyCredentialId;
    String commitSHA;
    String branch;

    // optional
    String repositoryPath;
    String coveragePaths;
    String tags;
    String quota;

    @DataBoundConstructor
    public BuildPulseStep(String account, String repository, String path, String keyCredentialId, String commit, String branch) {
        this.accountId = account;
        this.repositoryId = repository;
        this.junitXMLPaths = path;
        this.commitSHA = commit;
        this.branch = branch;
        this.keyCredentialId = keyCredentialId;
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

            FilePath remotePath = this.getContext().get(FilePath.class);

            // Retrieve the credential object using the credential ID
            @SuppressWarnings("deprecation") // CredentialsProvider.lookupCredentials argument needs to be updated
            StandardUsernamePasswordCredentials credentials = CredentialsMatchers.firstOrNull(
                    CredentialsProvider.lookupCredentials(
                            StandardUsernamePasswordCredentials.class,
                            Jenkins.get(),
                            ACL.SYSTEM,
                            Collections.<DomainRequirement>emptyList()),
                    CredentialsMatchers.withId(this.step.keyCredentialId));

            if (credentials == null) {
                throw new IllegalArgumentException("No credentials found for ID: " + this.step.keyCredentialId);
            }

            String key = credentials.getUsername();
            String secret = credentials.getPassword().getPlainText();

            String[] submitCommand = BuildPulseCommand.submit(this.step.accountId, this.step.repositoryId, this.step.junitXMLPaths, this.step.repositoryPath, key, secret,
                    remotePath, this.step.coveragePaths, this.step.tags, this.step.quota);

            // Get the build URL
            String buildUrl = this.getContext().get(Run.class).getEnvironment(listener).get("BUILD_URL");

            BinaryExecutor binaryExecutor = new BinaryExecutor(submitCommand, new HashMap<String, String>() {{
                put("BUILDPULSE_ACCESS_KEY_ID", key);
                put("BUILDPULSE_SECRET_ACCESS_KEY", secret);
                put("BUILD_URL", buildUrl);
                put("GIT_COMMIT", step.commitSHA);
                put("GIT_BRANCH", step.branch);
                put("GIT_URL", "github.com/owner/repo"); // dummy value as required argument for binary
                put("JENKINS_HOME", remotePath.getRemote());
            }});

            // Execute the binary and get output
            listener.getLogger().println("Executing: " + String.join(" ", binaryExecutor.command));
            BinaryExecutorResult result = remotePath.act(binaryExecutor);
            listener.getLogger().println("Output: " + result.output);
            listener.getLogger().println("BuildPulse Submit Exit Code: " + result.exitCode);

            return null;
        }
    }

    private static class BinaryExecutorResult implements Serializable {
        String output;
        int exitCode;

        public BinaryExecutorResult(String output, int exitCode) {
            this.output = output;
            this.exitCode = exitCode;
        }
    }

    private static class BinaryExecutor implements FileCallable<BinaryExecutorResult> {
        private final String[] command;
        HashMap <String, String> env;

        public BinaryExecutor(String[] command, HashMap <String, String> env) {
            this.command = command;
            this.env = env;
        }

        @Override
        public void checkRoles(RoleChecker checker) throws SecurityException {
            checker.check(this, Roles.SLAVE);
        }

        @Override
        public BinaryExecutorResult invoke(File f, VirtualChannel channel) throws IOException, InterruptedException {
            ProcessBuilder builder = new ProcessBuilder(command);
            this.env.keySet().forEach(key -> builder.environment().put(key, this.env.get(key)));
            builder.directory(f);
            builder.redirectErrorStream(true);

            // execute and capture stdout and stderr
            Process buildpulseProcess = builder.start();
            String output = IOUtils.toString(buildpulseProcess.getInputStream(), StandardCharsets.UTF_8);
            int exitCode = buildpulseProcess.waitFor();

            return new BinaryExecutorResult(output, exitCode);
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
