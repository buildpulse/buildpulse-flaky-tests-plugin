package io.jenkins.plugins;

import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.tasks.Builder;
import hudson.tasks.BuildStepDescriptor;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

public class BuildPulseBuilder extends Builder implements SimpleBuildStep {

    private final String accountId;
    private final String repositoryId;
    private List<String> junitXmlReportPaths; // Change to List<String>
    private String repositoryPath;
    private String key;
    private String secret;

    @DataBoundConstructor
    public BuildPulseBuilder(String accountId, String repositoryId) {
        this.accountId = accountId;
        this.repositoryId = repositoryId;
    }

    public String getAccountId() {
        return accountId;
    }

    public String getRepositoryId() {
        return repositoryId;
    }

    public List<String> getJunitXmlReportPaths() {
        return junitXmlReportPaths;
    }

    @DataBoundSetter
    public void setJunitXmlReportPaths(List<String> junitXmlReportPaths) {
        this.junitXmlReportPaths = junitXmlReportPaths;
    }

    public String getRepositoryPath() {
        return repositoryPath;
    }

    @DataBoundSetter
    public void setRepositoryPath(String repositoryPath) {
        this.repositoryPath = repositoryPath;
    }

    public String getKey() {
        return key;
    }

    @DataBoundSetter
    public void setKey(String key) {
        this.key = key;
    }

    public String getSecret() {
        return secret;
    }

    @DataBoundSetter
    public void setSecret(String secret) {
        this.secret = secret;
    }

    // ... define perform method here to execute the build step ...

    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return "BuildPulse";
        }
    }
}

