package io.jenkins.plugins;

import hudson.Extension;
import hudson.model.TaskListener;

import java.util.Collections;
import java.util.Set;

import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.jenkinsci.plugins.workflow.steps.SynchronousNonBlockingStepExecution;
import org.kohsuke.stapler.DataBoundConstructor;

import com.google.inject.Inject;

public class BuildPulseStep extends Step {

    private final String myArgument;

    @DataBoundConstructor
    public BuildPulseStep(String myArgument) {
        this.myArgument = myArgument;
    }

    public String getMyArgument() {
        return myArgument;
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
        protected Void run() throws Exception {
            TaskListener listener = getContext().get(TaskListener.class);
            listener.getLogger().println("Executing shell script with argument: " + step.getMyArgument());
            Runtime.getRuntime().exec("echo " + step.getMyArgument());
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
