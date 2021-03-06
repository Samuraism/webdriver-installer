/*
   Copyright 2021 Yusuke Yamamoto

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

package com.samuraism.webdriverinstaller;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.utils.IOUtils;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/*protected*/ public abstract class WebDriverInstaller {
    private final static Logger logger = Logger.getLogger("com.samuraism.webdriverinstaller.WebDriverInstaller");
    private final OS DETECTED_OS;
    private final String systemPropertyName;
    private final String appName;
    private final String driverName;
    private final String linuxApp;
    private final String macApp;
    private final String winApp;

    WebDriverInstaller(String systemPropertyName, String appName, String driverName, String linuxApp, String macApp, String winApp) {
        this.systemPropertyName = systemPropertyName;
        this.appName = appName;
        this.driverName = driverName;
        this.linuxApp = linuxApp;
        this.macApp = macApp;
        this.winApp = winApp;

        final String arch = "" + System.getProperty("sun.arch.data.model") + System.getProperty("os.arch");
        String osName = System.getProperty("os.name").toLowerCase();
        if (osName.contains("nux")) {
            DETECTED_OS = arch.contains("64") ? OS.LINUX64 : OS.LINUX32;
        } else {
            if (osName.startsWith("windows")) {
                DETECTED_OS = arch.contains("64") ? OS.WINDOWS64 : OS.WINDOWS32;
            } else if (osName.contains("mac") || osName.contains("darwin")) {
                DETECTED_OS = OS.MAC;
            } else {
                DETECTED_OS = OS.UNKNOWN;
            }
        }
    }

    protected final static String CHROME_DRIVER_ENV_NAME = "CHROME_DRIVER_HOME";
    protected final static String CHROME_DRIVER_PROPERTY_NAME = "chromedriver.home";

    /**
     * Checks if suitable version of ChromeDriver applicable to the version of installed Google Chrome at the path specified by CHROME_DRIVER_HOME environment variable or $HOME/chromedriver
     * If ChromeDriver is not found, attempts to download it from https://chromedriver.storage.googleapis.com/
     * System Property "webdriver.chrome.driver" will be also set.
     *
     * @return absolute path to installed chromedriver
     */
    public static Optional<String> ensureChromeDriverInstalled() {
        final String chromeDriverHome = System.getenv(CHROME_DRIVER_ENV_NAME);
        String path = chromeDriverHome != null ? chromeDriverHome :
                System.getProperty(CHROME_DRIVER_PROPERTY_NAME,
                        System.getProperty("user.home") + File.separator + "chromedriver");

        return new ChromeDriverInstaller().ensureInstalled(path);
    }

    protected final static String GECKO_DRIVER_ENV_NAME = "GECKO_DRIVER_HOME";
    protected final static String GECKO_DRIVER_PROPERTY_NAME = "geckodriver.home";

    /**
     * Checks if suitable version of geckodriver applicable to the version of installed Firefox at the path specified by GECKO_DRIVER_HOME environment variable,
     * or geckodriver.home system property, or $HOME/geckodriver
     * If geckodriver is not found, attempts to download it from https://github.com/mozilla/geckodriver/releases/
     * System Property "webdriver.gecko.driver" will be also set.
     *
     * @return absolute path to installed geckodriver
     */
    public static Optional<String> ensureGeckoDriverInstalled() {
        final String geckoDriverHome = System.getenv(GECKO_DRIVER_ENV_NAME);
        String path = geckoDriverHome != null ? geckoDriverHome :
                System.getProperty(GECKO_DRIVER_PROPERTY_NAME,
                        System.getProperty("user.home") + File.separator + "geckodriver");
        return new GeckodriverInstaller().ensureInstalled(path);
    }


    private boolean initialized = false;

    /**
     * ensure ChromeDriver is installed on the specified directory
     *
     * @param installRoot directory to be installed
     * @return path to the ChromeDriver binary
     */
    synchronized Optional<String> ensureInstalled(String installRoot) {
        final Optional<String> installedVersion = getInstalledAppVersion();
        if (!installedVersion.isPresent()) {
            return Optional.empty();
        }

        // 88.0.4324.96
        final String browserVersion = installedVersion.get();
        final String suitableDriverVersion = getSuitableDriverVersion(browserVersion);

        String binName = driverName + (isWin() ? ".exe" : "");
        // ex) geckodriver-v0.29.0-linux64.tar.gz

        String fileName = toFileName(suitableDriverVersion);

        // /root/88.0.4324.96
        // ex) /root/firefoxDriver/0.29.0
        Path installRootPath = Paths.get(installRoot, suitableDriverVersion);
        // /root/88.0.4324.96/chromedriver_mac64.zip
        // ex) /root/firefoxDriver/0.29.0/geckodriver-v0.29.0-linux64.tar.gz
        Path archivePath = installRootPath.resolve(fileName);
        // /root/88.0.4324.96/chromedriver
        // ex) /root/firefoxDriver/0.29.0/geckodriver
        final Path bin = installRootPath.resolve(binName).toAbsolutePath();
        // /root/88.0.4324.96/chromedriver
        String nativeDriver = bin.toString();
        // download nativeDriver
        String downloadURL = getDownloadURL(suitableDriverVersion, fileName);
        if (!initialized) {
            try {
                if (Files.exists(bin)) {
                    logger.info(nativeDriver + " already installed at: " + bin.toAbsolutePath().toString());
                    initialized = true;
                } else {
                    download(downloadURL, archivePath, installRootPath, bin);
                }
                System.setProperty(systemPropertyName, nativeDriver);
                initialized = true;
            } catch (IOException ioe) {
                logger.warning("Failed to download: " + downloadURL);
                ioe.printStackTrace();
            }
        }
        return Optional.of(nativeDriver);
    }

    abstract String getSuitableDriverVersion(String browserVersion);

    abstract String toFileName(String version);

    abstract String getDownloadURL(String version, String fileName);

    String choose(String linux32, String linux64, String mac, String win32, String win64) {
        switch (DETECTED_OS) {
            case LINUX32:
                return linux32;
            case LINUX64:
                return linux64;
            case MAC:
                return mac;
            case WINDOWS32:
                return win32;
            case WINDOWS64:
                return win64;
        }
        return null;
    }


    enum OS {
        MAC,
        LINUX32,
        LINUX64,
        WINDOWS32,
        WINDOWS64,
        UNKNOWN
    }

    /**
     * Returns version string of installed app.
     *
     * @return version string of installed app
     */
    public Optional<String> getInstalledAppVersion() {
        try {
            String chromePath;
            switch (DETECTED_OS) {
                case MAC:
                    chromePath = macApp;
                    break;
                case LINUX32:
                case LINUX64:
                    chromePath = getAppPath(linuxApp);
                    break;
                case WINDOWS32:
                case WINDOWS64:
                    chromePath = getAppPath(winApp);
                    break;
                default:
                    throw new UnsupportedOperationException("Not yet supported");
            }
            if (!new File(chromePath).exists()) {
                logger.warning("Chrome not found at " + chromePath);
                return Optional.empty();
            }
            final String result = getAppVersion(chromePath);
            final String versionString = result.substring(result.lastIndexOf(" ") + 1);
            return Optional.of(versionString);
        } catch (IOException | InterruptedException e) {
            logger.warning("Failed to locate " + appName);
            e.printStackTrace();
            return Optional.empty();
        }
    }


    protected boolean isWin() {
        return DETECTED_OS == OS.WINDOWS32 || DETECTED_OS == OS.WINDOWS64;
    }


    static String execute(File directory, String[] commands) throws IOException, InterruptedException {
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

            return output.trim();
        } finally {
            //noinspection ResultOfMethodCallIgnored
            tempFile.delete();
        }
    }

    static void download(String downloadURL, Path archivePath, Path installRootPath, Path bin) throws IOException {
        Files.createDirectories(installRootPath);
        //noinspection ResultOfMethodCallIgnored
        archivePath.toFile().delete();
        URL url = new URL(downloadURL);
        HttpURLConnection con = null;
        try {
            con = (HttpURLConnection) url.openConnection();
            con.setReadTimeout(5000);
            con.setConnectTimeout(5000);
            int code = con.getResponseCode();
            if (code == 200) {
                Files.copy(con.getInputStream(), archivePath);
            } else {
                throw new IOException("URL[" + url + "] returns code [" + code + "].");
            }
        } finally {
            if (con != null) {
                con.disconnect();
            }
        }
        decompress(archivePath, installRootPath);
        //noinspection ResultOfMethodCallIgnored
        bin.toFile().setExecutable(true);
    }

    private static void unZip(Path toUnzip, Path root) throws IOException {
        ZipFile zip = new ZipFile(toUnzip.toFile());
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

    static void decompress(Path toDecompress, Path root) throws IOException {
        if (toDecompress.toString().matches(".*(tar.bz2|tar.gz)$")) {
            unTar(toDecompress, root);
        } else {
            unZip(toDecompress, root);
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private static void unTar(Path toDecompress, Path root) throws IOException {
        File tarFile = File.createTempFile("driver", "tar");
        try (InputStream in = toDecompress.toString().endsWith(".gz") ?
                new GZIPInputStream(new FileInputStream(toDecompress.toFile()))
                : new BZip2CompressorInputStream(new FileInputStream(toDecompress.toFile()));
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

    /*package*/
    protected String getAppPath(String name) throws IOException, InterruptedException {
        return isWin() ?
                execute(new File("."), new String[]{"powershell", "-command",
                        String.format("(Get-ItemProperty -ErrorAction Stop -Path \\\"HKLM:SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\App Paths\\%s\\\").'(default)'", name)})
                : execute(new File("/"),
                new String[]{"/bin/bash", "-c", String.format("which '%s'", name)});
    }

    protected String getAppVersion(String appPath) throws IOException, InterruptedException {
        return isWin() ?
                execute(new File("."), new String[]{"powershell", "-command",
                        "(Get-Item -ErrorAction Stop \\\"" + appPath + "\\\").VersionInfo.ProductVersion"})
                : execute(new File("/"), new String[]{appPath, "-version", appPath});
    }
}
