package io.jenkins.plugins;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.listeners.RunListener;
import hudson.util.ArgumentListBuilder;
import jenkins.YesNoMaybe;
import jenkins.tasks.SimpleBuildStep;
import org.jenkinsci.remoting.RoleChecker;

import java.io.IOException;
import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;

import java.io.InputStream;
import java.io.FileOutputStream;
import java.net.URL;
import java.nio.file.Paths;
import java.nio.file.Files;

import java.nio.file.*;
import java.io.IOException;
import java.util.*;
import java.util.stream.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Extension(dynamicLoadable = YesNoMaybe.YES)
public class BuildPulseTestListener extends RunListener<Run<?, ?>> {

    private static final Logger LOGGER = Logger.getLogger(BuildPulseTestListener.class.getName());

    @Override
    public void onCompleted(Run<?, ?> run, TaskListener listener) {
        try {
            // You'd need to set these based on your setup
            String accountId = "your_account_id";
            String repositoryId = "your_repository_id";
            String path = "path_to_your_test_reports";
            String repositoryPath = "path_to_your_repository";
            String key = "your_key";
            String secret = "your_secret";
            String commit = run.getEnvironment(listener).get("GIT_COMMIT");

            FilePath workspace = run.getExecutor().getCurrentWorkspace();

            // Execute the script
            BuildPulseCommand command = new BuildPulseCommand(accountId, repositoryId, path, repositoryPath, key, secret, commit);
            workspace.act(command);
        } catch (IOException | InterruptedException e) {
            LOGGER.log(Level.SEVERE, "Failed to send test results to BuildPulse", e);
        }
    }

    private static class BuildPulseCommand implements SimpleBuildStep.LastBuildAction, Serializable, FilePath.FileCallable<Void> {
        private final String accountId;
        private final String repositoryId;
        private List<String> junitXmlReportPaths;
        private final String repositoryPath;
        private final String key;
        private final String secret;
        private final String commit;

        public BuildPulseCommand(String accountId, String repositoryId, List<String> junitXmlReportPaths, String repositoryPath, String key, String secret, String commit) {
            this.accountId = accountId;
            this.repositoryId = repositoryId;
            this.junitXmlReportPaths = junitXmlReportPaths;
            this.repositoryPath = repositoryPath;
            this.key = key;
            this.secret = secret;
            this.commit = commit;
        }

        @Override
        public Void invoke(FilePath workspace, hudson.remoting.VirtualChannel virtualChannel) throws IOException, InterruptedException {
            this.validateArguments();
9
            FilePath binaryFilePath = this.downloadBuildPulseBinary();

            String path = this.getPathsString(reportFiles);
            String cmd = String.format("BUILDPULSE_ACCESS_KEY_ID=%s BUILDPULSE_SECRET_ACCESS_KEY=%s GITHUB_SHA=%s %s submit %s --account-id %s --repository-id %s --repository-dir %s", this.key, this.secret, this.commit, binaryFilePath, path, this.accountId, this.repositoryId, this.repositoryPath);

            ArgumentListBuilder args = new ArgumentListBuilder();
            args.add("bash", "-c");
            args.addQuoted(cmd);

            int result = workspace.act(new hudson.tasks.CommandInterpreter(args.toString()));

            if (result != 0) {
                throw new RuntimeException("Command failed with exit code: " + result);
            }

            return null;
        }

        private FilePath downloadBuildPulseBinary() throws IOException {
            String binaryFileName = determineBinaryFileName();
            URL binaryUrl = new URL("https://github.com/buildpulse/test-reporter/releases/latest/download/" + binaryFileName);
            String tempDirectoryPath = System.getProperty("java.io.tmpdir");
            FilePath binaryFilePath = new FilePath(Paths.get(tempDirectoryPath, binaryFileName).toFile());

            if (!binaryFilePath.exists()) {
                try (InputStream in = binaryUrl.openStream(); FileOutputStream out = new FileOutputStream(binaryFilePath.getRemote())) {
                    byte[] buffer = new byte[4096];
                    int n;

                    while ((n = in.read(buffer)) != -1) {
                        out.write(buffer, 0, n);
                    }
                }
            }

            // Making the binary executable
            binaryFilePath.chmod(0755);

            return binaryFilePath;
        }

        private List<String> validateArguments() throws RuntimeException {
            if (!this.accountId.matches('^[0-9]+$')) {
                throw new RuntimeException("Account ID doesn't match expected format: '^[0-9]+$'");
            }

            if (!this.repositoryId.matches('^[0-9]+$')) {
                throw new RuntimeException("Repository ID doesn't match expected format: '^[0-9]+$'");
            }

            Path path = Paths.get(this.repositoryPath);
            if (!Files.isDirectory(path)) {
                throw new RuntimeException("Repository ID doesn't match expected format: '^[0-9]+$'");
            }

            if (this.key.length() == 0) {
                throw new RuntimeException("BuildPulse key is required");
            }

            if (this.secret.length() == 0) {
                throw new RuntimeException("BuildPulse secret is required");
            }

            return this.resolveFilePaths(this.junitXmlReportPaths);
        }

        private String getPathsString(List<string> reportPaths) {
            return String.join(" ", reportPaths);
        }

        private String determineBinaryFileName() {
            String osName = System.getProperty("os.name").toLowerCase();
            String binaryFileName;

            if (osName.contains("win")) {
                binaryFileName = "test-reporter-windows-amd64.exe";
            } else if (osName.contains("mac")) {
                binaryFileName = "test-reporter-darwin-amd64";
            } else {
                binaryFileName = "test-reporter-linux-amd64";
            }

            return binaryFileName;
        }

        private List<String> resolveFilePaths(List<String> junitXmlReportPaths) throws IOException {
            String[] individualPaths = paths.split(" ");
            List<String> resolvedPaths = new ArrayList<>();

            List<File> reportFiles = new ArrayList<>();
            for (String glob : junitXmlReportPaths) {
                PathMatcher pathMatcher = FileSystems.getDefault().getPathMatcher("glob:" + glob);
                Files.walk(FileSystems.getDefault().getPath("."))
                    .filter(pathMatcher::matches)
                    .map(Path::toFile)
                    .forEach(reportFiles::add);
            }

            return reportFiles;
        }
    }
}

