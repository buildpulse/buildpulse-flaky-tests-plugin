package io.jenkins.plugins.buildpulse;

import hudson.FilePath;
import hudson.FilePath.FileCallable;
import hudson.remoting.VirtualChannel;
import jenkins.security.Roles;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;

import org.jenkinsci.remoting.RoleChecker;
import org.kohsuke.stapler.verb.POST;

public class BuildPulseCommand {
    private String accountId;
    private String repositoryId;
    private String junitXmlReportPaths;
    private String keyId; // lgtm[jenkins/plaintext-storage]
    private String secret;
    private FilePath workspacePath;

    // optional arguments
    private String coveragePaths;
    private String tags;
    private String quota;
    private FilePath repositoryPath;

    public static String[] submit(String accountId, String repositoryId, String junitXmlReportPaths, String repositoryPath, String keyId, String secret, FilePath workspacePath, String coveragePaths, String tags, String quota) throws IOException, InterruptedException {
        BuildPulseCommand command = new BuildPulseCommand(accountId, repositoryId, junitXmlReportPaths, repositoryPath, keyId, secret, workspacePath, coveragePaths, tags, quota);
        return command.downloadAndGetCommand();
    }

    public BuildPulseCommand(String accountId, String repositoryId, String junitXmlReportPaths, String repositoryPath,
            String keyId, String secret, FilePath workspacePath, String coveragePaths, String tags, String quota) {
        this.accountId = accountId;
        this.repositoryId = repositoryId;
        this.junitXmlReportPaths = junitXmlReportPaths;
        this.keyId = keyId;
        this.secret = secret;
        this.workspacePath = workspacePath;

        // optional arguments
        this.repositoryPath = repositoryPath != null && repositoryPath.length() > 0 ? new FilePath(workspacePath, repositoryPath) : workspacePath;

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
        command.addAll(Arrays.asList("--account-id", this.accountId, "--repository-id", this.repositoryId, "--repository-dir", this.repositoryPath.getRemote()));

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
        URL binaryUrl = new URL("https://github.com/buildpulse/test-reporter/releases/latest/download/" + binaryFileName);

        // Use act to safely perform the download on the agent where the workspace resides
        return workspacePath.act(new FileDownloader(binaryUrl, binaryFileName));
    }

    private static class FileDownloader implements FileCallable<FilePath>, Serializable {
        private final URL url;
        private final String fileName;

        public FileDownloader(URL url, String fileName) {
            this.url = url;
            this.fileName = fileName;
        }

        @Override
        public void checkRoles(RoleChecker checker) throws SecurityException {
            checker.check(this, Roles.SLAVE);
        }

        @Override
        public FilePath invoke(File f, VirtualChannel channel) throws IOException, InterruptedException {
            File localFile = new File(f, fileName);
            FilePath filePath = new FilePath(localFile);

            if (!filePath.exists()) {
                try (InputStream in = url.openStream(); OutputStream out = filePath.write()) {
                    byte[] buffer = new byte[4096];
                    int n;
                    while ((n = in.read(buffer)) != -1) {
                        out.write(buffer, 0, n);
                    }
                }
            }

            // Set the file to be executable
            filePath.chmod(0755);

            return filePath;
        }
    }

    private void validateArguments() throws RuntimeException, IOException {
        if (!this.accountId.matches("^[0-9]+$")) {
            throw new RuntimeException("Account ID " + this.accountId + " doesn't match expected format: '^[0-9]+$'");
        }

        if (!this.repositoryId.matches("^[0-9]+$")) {
            throw new RuntimeException("Repository ID " + this.repositoryId + " doesn't match expected format: '^[0-9]+$'");
        }

        boolean exists = false;
        boolean isDir = false;
        try {
            exists = this.repositoryPath.exists();
            isDir = this.repositoryPath.isDirectory();
        } catch (InterruptedException e) {}

        if (!(exists && isDir)) {
            throw new RuntimeException("Repository path is not a directory: " + this.repositoryPath);
        }

        if (this.keyId.length() == 0) {
            throw new RuntimeException("BuildPulse key is required");
        }

        if (this.secret.length() == 0) {
            throw new RuntimeException("BuildPulse secret is required");
        }
    }

    private String determineBinaryFileName() throws IOException, InterruptedException {
        String osName = (String)this.workspacePath.toComputer().getSystemProperties().get("os.name");
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
            resolvedPaths.add(new File(this.workspacePath.getRemote(), path.toString()).getPath());
        }

        return resolvedPaths;
    }
}
