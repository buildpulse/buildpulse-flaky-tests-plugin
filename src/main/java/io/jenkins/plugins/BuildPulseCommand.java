package io.jenkins.plugins;

import hudson.FilePath;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;

import org.kohsuke.stapler.verb.POST;

@SuppressWarnings("lgtm[jenkins/plaintext-storage]")
public class BuildPulseCommand {
    private String accountId;
    private String repositoryId;
    private String junitXmlReportPaths;
    private String keyId;
    private String secret;
    private String workspacePath;

    // optional arguments
    private String coveragePaths;
    private String tags;
    private String quota;
    private String repositoryPath;

    public static String[] submit(String accountId, String repositoryId, String junitXmlReportPaths, String repositoryPath, String keyId, String secret, String workspacePath, String coveragePaths, String tags, String quota) throws IOException, InterruptedException {
        BuildPulseCommand command = new BuildPulseCommand(accountId, repositoryId, junitXmlReportPaths, repositoryPath, keyId, secret, workspacePath, coveragePaths, tags, quota);
        return command.downloadAndGetCommand();
    }

    public BuildPulseCommand(String accountId, String repositoryId, String junitXmlReportPaths, String repositoryPath,
            String keyId, String secret, String workspacePath, String coveragePaths, String tags, String quota) {
        this.accountId = accountId;
        this.repositoryId = repositoryId;
        this.junitXmlReportPaths = junitXmlReportPaths;
        this.keyId = keyId;
        this.secret = secret;
        this.workspacePath = workspacePath;

        // optional arguments
        this.repositoryPath = repositoryPath.length() > 0 ? repositoryPath : workspacePath;

        if (coveragePaths != null) {
            this.coveragePaths = coveragePaths;
        }

        if (tags != null) {
            this.tags = tags;
        }

        if (quota != null) {
            this.quota = quota;
        }
    }

    @POST
    @SuppressWarnings("lgtm[jenkins/no-permission-check]")
    public String[] downloadAndGetCommand() throws IOException, InterruptedException {
        this.validateArguments();

        ArrayList<String> validatedJunitXmlReportPaths = this.resolveFilePaths(this.junitXmlReportPaths);
        ArrayList<String> validatedCoverageReportPaths = this.resolveFilePaths(this.coveragePaths);

        FilePath binaryFilePath = this.downloadBuildPulseBinary();

        ArrayList<String> command = new ArrayList<String>(Arrays.asList(binaryFilePath.getRemote(), "submit"));
        command.addAll(validatedJunitXmlReportPaths);
        command.addAll(Arrays.asList("--account-id", this.accountId, "--repository-id", this.repositoryId, "--repository-dir", this.repositoryPath));

        if (validatedCoverageReportPaths.size() > 0) {
            command.add("--coverage-files");
            command.addAll(validatedCoverageReportPaths);
        }

        if (this.tags != null && this.tags.length() > 0) {
            command.add("--tags");
            command.add(this.tags);
        }

        if (this.quota != null && this.quota.length() > 0) {
            command.add("--quota-id");
            command.add(this.quota);
        }

        command.add("--disable-coverage-auto");

        return command.toArray(new String[0]);
    }

    private FilePath downloadBuildPulseBinary() throws IOException, InterruptedException {
        String binaryFileName = determineBinaryFileName();
        URL binaryUrl = new URL(
                "https://github.com/buildpulse/test-reporter/releases/latest/download/" + binaryFileName);
        String tempDirectoryPath = System.getProperty("java.io.tmpdir");
        FilePath binaryFilePath = new FilePath(Paths.get(tempDirectoryPath, binaryFileName).toFile());

        if (!binaryFilePath.exists()) {
            try (InputStream in = binaryUrl.openStream();
                    FileOutputStream out = new FileOutputStream(binaryFilePath.getRemote())) {
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

    private void validateArguments() throws RuntimeException, IOException {
        if (!this.accountId.matches("^[0-9]+$")) {
            throw new RuntimeException("Account ID doesn't match expected format: '^[0-9]+$'");
        }

        if (!this.repositoryId.matches("^[0-9]+$")) {
            throw new RuntimeException("Repository ID doesn't match expected format: '^[0-9]+$'");
        }

        Path path = Paths.get(this.repositoryPath);
        if (!Files.isDirectory(path)) {
            throw new RuntimeException("Repository ID doesn't match expected format: '^[0-9]+$'");
        }

        if (this.keyId.length() == 0) {
            throw new RuntimeException("BuildPulse key is required");
        }

        if (this.secret.length() == 0) {
            throw new RuntimeException("BuildPulse secret is required");
        }
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

    private ArrayList<String> resolveFilePaths(String pathsString) throws IOException {
        if (pathsString == null) {
            return new ArrayList<String>();
        }

        String[] paths = pathsString.split(" ");
        ArrayList<String> resolvedPaths = new ArrayList<String>();

        for (int i = 0; i < paths.length; i++) {
            String path = paths[i];
            resolvedPaths.add(new File(this.workspacePath, path.toString()).getPath());
        }

        return resolvedPaths;
    }
}
