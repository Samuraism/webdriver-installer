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
package com.samuraism.webdriverinstaller;

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

@SuppressWarnings("WeakerAccess")
public final class ChromeDriverInstaller {
    private final static Logger logger = Logger.getLogger("com.samuraism.webdriverinstaller.ChromeDriverInstaller");
    public static void main(String[] args) {
        // install Chrome Driver in /tmp/chromedriver
        // This ensures chrome driver to be installed at /tmp/chromedriver
        // "webdriver.chrome.driver" system property will be also set.
        Optional<String> path = ChromeDriverInstaller.ensureInstalled(System.getProperty("user.home")
                + File.separator + "chromedriver");
        if (path.isPresent()) {
            logger.info("ChromeDriver installed at: " + path.get());
        } else {
            logger.warning("Failed to install ChromeDriver");
        }
    }

    private static boolean initialized = false;
    /**
     * ensure ChromeDriver is installed on the specified directory
     *
     * @param installRoot directory to be installed
     * @return path to the ChromeDriver binary
     */
    public synchronized static Optional<String> ensureInstalled(String installRoot) {
        String fileName;
        String dirName = "chromedriver_";
        String binName = "chromedriver";

        switch (Util.DETECTED_OS) {
            case LINUX32:
                dirName += "linux32";
                break;
            case LINUX64:
                dirName += "linux64";
                break;
            case MAC:
                dirName += "mac64";
                break;
            case WINDOWS32:
            case WINDOWS64:
                dirName += "win32";
                binName += ".exe";
                break;
        }
        fileName = dirName + ".zip";

        final Optional<String> installedVersion = getInstalledChromeVersion();
        if (!installedVersion.isPresent()) {
            return Optional.empty();
        }
        // 88.0.4324.96
        final String chromeVersion = installedVersion.get();
        // /root/88.0.4324.96
        Path installRootPath = Paths.get(installRoot, chromeVersion);
        // /root/88.0.4324.96/chromedriver_mac64.zip
        Path archivePath = installRootPath.resolve(fileName);
        // /root/88.0.4324.96/chromedriver
        final Path bin = installRootPath.resolve(binName);
        // /root/88.0.4324.96/chromedriver
        String chromedriver = bin.toAbsolutePath().toFile().getAbsolutePath();
        // download ChromeDriver
        String downloadURL = "https://chromedriver.storage.googleapis.com/" + chromeVersion + "/" + fileName;
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
                    Util.download(downloadURL, archivePath, installRootPath, bin);
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
            switch (Util.DETECTED_OS) {
                case MAC:
                    chromePath = "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome";
                    break;
                case LINUX32:
                case LINUX64:
                    chromePath = "/usr/bin/google-chrome";
                    break;
                case WINDOWS32:
                case WINDOWS64:
                    return getInstalledChromeVersionForWindows();
                case UNKNOWN:
                    throw new UnsupportedOperationException("Not yet supported");
            }
            if (!new File(chromePath).exists()) {
                logger.warning("Chrome not found at " + chromePath);
                return Optional.empty();
            }
            final String result = Util.execute(new File("/"), new String[]{chromePath, "-version"});
            final String versionString = result.substring("Google Chrome ".length()).trim();
            return Optional.of(versionString);
        } catch (IOException | InterruptedException e) {
            logger.warning("Failed to locate Google Chrome");
            e.printStackTrace();
            return Optional.empty();
        }
    }

    private static Optional<String> getInstalledChromeVersionForWindows() throws IOException, InterruptedException {
        // See: How to get chrome version using command prompt in windows
        // https://stackoverflow.com/a/57618035/1932017
        return Optional.of(Util.execute(new File("."),
                new String[]{"powershell", "-command", "(Get-Item -ErrorAction Stop \\\"" + Util.getAppPath("chrome.exe")
                        + "\\\").VersionInfo.ProductVersion"}).trim());
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
}
