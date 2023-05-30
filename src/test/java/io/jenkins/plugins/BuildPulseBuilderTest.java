import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.tasks.Builder;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class BuildPulseBuilderTest {
    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private BuildPulseUploader buildPulseUploader;

    @Test
    public void testPerform() throws Exception {
        BuildPulseBuilder builder = new BuildPulseBuilder();
        builder.setAccountId("1234");
        builder.setRepositoryId("5678");
        builder.setJunitXmlReportPaths(Arrays.asList("path1.xml", "path2.xml"));
        builder.setKey("key");
        builder.setSecret("secret");

        FreeStyleProject project = jenkins.createFreeStyleProject();
        project.getBuildersList().add(builder);

        FreeStyleBuild build = project.scheduleBuild2(0).get();

        verify(buildPulseUploader, times(1)).uploadTestResults(any(), eq("1234"), eq("5678"), eq(Arrays.asList("path1.xml", "path2.xml")), eq("key"), eq("secret"));
        assertEquals(Result.SUCCESS, build.getResult());
    }
}

