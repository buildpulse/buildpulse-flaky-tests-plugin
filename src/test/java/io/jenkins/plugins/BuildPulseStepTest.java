package io.jenkins.plugins;

import hudson.model.Descriptor;
import jenkins.model.Jenkins;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import static org.junit.Assert.*;

public class BuildPulseStepTest {

  @Rule
  public JenkinsRule jenkinsRule = new JenkinsRule();

  @Test
  public void testDescriptorExists() {
    Descriptor<?> descriptor = Jenkins.get().getDescriptorByType(BuildPulseStep.DescriptorImpl.class);
    assertNotNull("Descriptor should not be null", descriptor);
    assertTrue(descriptor instanceof BuildPulseStep.DescriptorImpl);
    assertEquals("buildpulseStep", ((BuildPulseStep.DescriptorImpl) descriptor).getFunctionName());
  }
}
