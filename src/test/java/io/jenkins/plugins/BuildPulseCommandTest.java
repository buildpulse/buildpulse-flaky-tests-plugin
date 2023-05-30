import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class BuildPulseCommandTest {

    @Mock
    private BuildPulseUploader buildPulseUploader;

    private BuildPulseCommand buildPulseCommand;

    @Before
    public void setUp() {
        buildPulseCommand = new BuildPulseCommand(buildPulseUploader);
        buildPulseCommand.setAccountId("1234");
        buildPulseCommand.setRepositoryId("5678");
        buildPulseCommand.setJunitXmlReportPaths(Arrays.asList("path1.xml", "path2.xml"));
        buildPulseCommand.setKey("key");
        buildPulseCommand.setSecret("secret");
    }

    @Test
    public void testCommandExecutesUpload() throws Exception {
        buildPulseCommand.execute();

        verify(buildPulseUploader, times(1)).uploadTestResults(any(), eq("1234"), eq("5678"), eq(Arrays.asList("path1.xml", "path2.xml")), eq("key"), eq("secret"));
    }
}

