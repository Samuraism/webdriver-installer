/*
   Copyright 2016 - 2021 Shinya Mochida, Yusuke Yamamoto

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package com.samuraism.chromedriverinstaller;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@SuppressWarnings("WeakerAccess")
public final class ChromeDriverInstaller {
    private final static Logger logger = Logger.getLogger("com.samuraism.chromedriverinstaller.ChromeDriverInstaller");
    public static void main(String[] args) {
        // install Chrome Driver in /tmp/chromedriver
        // This ensures chrome driver to be installed at /tmp/chromedriver
        // "webdriver.chrome.driver" system property will be also set.
        Optional<String> path = ChromeDriverInstaller.ensureInstalled("/tmp/chromedriver");
        if(!path.isPresent()){
            logger.warning("Failed to install Chrome Driver");
        }
    }

    static String fileName;
    static String dirName = "chromedriver_";
    static String binName = "chromedriver";

    private static final OS os;

    enum OS {
        MAC,
        LINUX64,
        LINUX32,
        WINDOWS,
        UNKNOWN
    }

    static {
        String osName = System.getProperty("os.name").toLowerCase();
        if (osName.contains("nux")) {
            os = "32".equals(System.getProperty("sun.arch.data.model")) ? OS.LINUX32 : OS.LINUX64;
        } else {
            if (osName.startsWith("windows")) {
                os = OS.WINDOWS;
            } else if (osName.contains("mac") || osName.contains("darwin")) {
                os = OS.MAC;
            } else {
                os = OS.UNKNOWN;
            }
        }
        switch (os) {
            case LINUX32:
                dirName += "linux32";
                break;
            case LINUX64:
                dirName += "linux64";
                break;
            case MAC:
                dirName += "mac64";
                break;
            case WINDOWS:
                dirName += "win32";
                binName += ".exe";
                break;
            case UNKNOWN:
                throw new IllegalStateException("Unexpected os:" + os);

        }
        fileName = dirName + ".zip";
    }

    private static boolean initialized = false;
    /**
     * ensure ChromeDriver is installed on the specified directory
     *
     * @param installRoot directory to be installed
     * @return path to the ChromeDriver binary
     */
    public synchronized static Optional<String> ensureInstalled(String installRoot) {
        final Optional<String> installedVersion = getInstalledChromeVersion();
        if (!installedVersion.isPresent()) {
            return Optional.empty();
        }
        final String chromeVersion = installedVersion.get();
        Path installRootPath = Paths.get(installRoot, chromeVersion);

        Path filePath = installRootPath.resolve(fileName);
        final Path bin = installRootPath.resolve(binName);
        String chromedriver = bin.toAbsolutePath().toFile().getAbsolutePath();
        // download ChromeDriver
        String downloadURL = "";
        if (!initialized) {
            try {
                if (Files.exists(bin)) {
                    logger.info("ChromeDriver is installed at: " + bin.toAbsolutePath().toString());
                    initialized = true;
                } else {
                    if (listAvailableChromeDriverVersions().stream().noneMatch(e -> e.equals(chromeVersion))) {
                        logger.warning("chrome driver for version:"+ chromeVersion + " is not available at this moment. https://chromedriver.storage.googleapis.com/index.html");
                        return Optional.empty();
                    }
                    Files.createDirectories(installRootPath);
                    downloadURL = "https://chromedriver.storage.googleapis.com/" + chromeVersion + "/" + fileName;
                    //noinspection ResultOfMethodCallIgnored
                    filePath.toFile().delete();
                    URL url = new URL(downloadURL);
                    HttpURLConnection con = null;
                    try {
                        con = (HttpURLConnection) url.openConnection();
                        con.setReadTimeout(5000);
                        con.setConnectTimeout(5000);
                        int code = con.getResponseCode();
                        if (code == 200) {
                            Files.copy(con.getInputStream(), filePath);
                        } else {
                            throw new IOException("URL[" + url + "] returns code [" + code + "].");
                        }
                    } finally {
                        if (con != null) {
                            con.disconnect();
                        }
                    }
                    if (filePath.getFileName().toString().endsWith("tar.bz2")) {
                        unTar(filePath.toFile().getAbsolutePath(), installRootPath);
                    } else {
                        unZip(installRootPath, filePath);
                    }
                    //noinspection ResultOfMethodCallIgnored
                    bin.toFile().setExecutable(true);
                }
                System.setProperty("webdriver.chrome.driver", chromedriver);
                initialized = true;
            } catch (IOException ioe) {
                logger.warning("Failed to download: " + downloadURL);
                ioe.printStackTrace();
            }
        }
        return Optional.of(chromedriver);
    }

    /**
     * Returns version string of installed chrome.
     *
     * @return version string of installed chrome
     */
    public static Optional<String> getInstalledChromeVersion() {
        try {
            String chromePath = "";
            switch (os) {
                case MAC:
                    chromePath = "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome";
                    break;
                case LINUX64:
                case LINUX32:
                    chromePath = "/usr/bin/google-chrome";
                    break;
                case WINDOWS:
                    return getInstalledChromeVersionForWindows();
                case UNKNOWN:
                    throw new UnsupportedOperationException("Not yet supported");
            }
            if (!new File(chromePath).exists()) {
                logger.warning("Chrome not found at " + chromePath);
                return Optional.empty();
            }
            final String result = execute(new File("/"), new String[]{chromePath, "-version"});
            final String versionString = result.substring("Google Chrome ".length()).trim();
            return Optional.of(versionString);
        } catch (IOException | InterruptedException e) {
            logger.warning("Failed to locate Google Chrome");
            e.printStackTrace();
            return Optional.empty();
        }
    }

    private static String execute(File directory, String[] commands) throws IOException, InterruptedException {
        File tempFile = File.createTempFile("chromeDriverInstaller", "out");
        tempFile.deleteOnExit();
        try {
            ProcessBuilder pb = new ProcessBuilder(commands)
                    .directory(directory)
                    .redirectErrorStream(true)
                    .redirectOutput(ProcessBuilder.Redirect.to(tempFile));
            Process process = pb.start();
            process.waitFor();

            String output = new String(Files.readAllBytes(tempFile.toPath()));
            if (process.exitValue() != 0) {
                throw new IOException("Execution failed. commands: " + Arrays.toString(commands) + ", output:" + output);
            }

            return output;
        }finally {
            //noinspection ResultOfMethodCallIgnored
            tempFile.delete();
        }
    }

    private static Optional<String> getInstalledChromeVersionForWindows() throws IOException, InterruptedException {
        final File currentDir = new File(".");
        final String chromePath = execute(currentDir, new String[]{"powershell", "-command", "(Get-ItemProperty -ErrorAction Stop -Path \\\"HKLM:SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\App Paths\\chrome.exe\\\").'(default)'"});

        // See: How to get chrome version using command prompt in windows
        // https://stackoverflow.com/a/57618035/1932017
        final String versionString = execute(currentDir, new String[]{"powershell", "-command", "(Get-Item -ErrorAction Stop \\\"" + chromePath.trim() + "\\\").VersionInfo.ProductVersion"});

        return Optional.of(versionString.trim());
    }

    static List<String> listAvailableChromeDriverVersions() {
        final URLConnection con;
        try {
            con = new URL("https://chromedriver.storage.googleapis.com/?delimiter=/&prefix=").openConnection();
            con.setConnectTimeout(5000);
            con.setReadTimeout(5000);
            final org.w3c.dom.Document doc = DocumentBuilderFactory
                    .newInstance()
                    .newDocumentBuilder().parse(con.getInputStream());
            final NodeList prefix = doc.getElementsByTagName("Prefix");
            List<String> versions = new ArrayList<>();
            for (int i = 0; i < prefix.getLength(); i++) {
                final String textContent = prefix.item(i).getTextContent();
                if (textContent.matches("[0-9./]+")) {
                    versions.add(textContent.replace("/", ""));
                }
            }
            return versions;
        } catch (IOException | ParserConfigurationException | SAXException e) {
            throw new IllegalStateException(e);
        }

    }



    private static void unZip(Path root, Path archiveFile) throws IOException {
        ZipFile zip = new ZipFile(archiveFile.toFile());
        Enumeration<? extends ZipEntry> entries = zip.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            if (entry.isDirectory()) {
                Files.createDirectories(root.resolve(entry.getName()));
            } else {
                try (InputStream is = new BufferedInputStream(zip.getInputStream(entry))) {
                    Files.copy(is, root.resolve(entry.getName()));
                }
            }

        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private static void unTar(String s, Path root) throws IOException {
        File tarFile = File.createTempFile("driver", "tar");
        try (BZip2CompressorInputStream in = new BZip2CompressorInputStream(new FileInputStream(s));
             FileOutputStream out = new FileOutputStream(tarFile)) {
            IOUtils.copy(in, out);
        }

        File outputDir = root.toFile();
        outputDir.mkdirs();
        try (ArchiveInputStream is = new ArchiveStreamFactory()
                .createArchiveInputStream("tar", new FileInputStream(tarFile))) {
            ArchiveEntry entry;
            while ((entry = is.getNextEntry()) != null) {
                File out = new File(outputDir, entry.getName());
                if (entry.isDirectory()) {
                    out.mkdirs();
                } else {
                    try (OutputStream fos = new FileOutputStream(out)) {
                        IOUtils.copy(is, fos);
                    }
                }
            }
        } catch (ArchiveException e) {
            throw new IOException(e);
        }
        tarFile.delete();
    }
}
