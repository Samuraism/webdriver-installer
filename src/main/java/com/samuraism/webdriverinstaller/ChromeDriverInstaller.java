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
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Logger;

@SuppressWarnings("WeakerAccess")
public final class ChromeDriverInstaller extends WebDriverInstaller {
    private final static Logger logger = Logger.getLogger("com.samuraism.webdriverinstaller.ChromeDriverInstaller");

    public static void main(String[] args) {
        // install Chrome Driver in /tmp/chromedriver
        // This ensures chrome driver to be installed at /tmp/chromedriver
        // "webdriver.chrome.driver" system property will be also set.
        Optional<String> path = new ChromeDriverInstaller().ensureInstalled(System.getProperty("user.home")
                + File.separator + "chromedriver");
        if (path.isPresent()) {
            logger.info("ChromeDriver installed at: " + path.get());
        } else {
            logger.warning("Failed to install ChromeDriver");
        }
    }

    private boolean initialized = false;

    /**
     * ensure ChromeDriver is installed on the specified directory
     *
     * @param installRoot directory to be installed
     * @return path to the ChromeDriver binary
     */
    public synchronized Optional<String> ensureInstalled(String installRoot) {
        String fileName;
        String dirName = "chromedriver_";
        String binName = "chromedriver";

        switch (DETECTED_OS) {
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
                    logger.info("ChromeDriver is already installed at: " + bin.toAbsolutePath().toString());
                    initialized = true;
                } else {
                    if (listAvailableChromeDriverVersions().stream().noneMatch(e -> e.equals(chromeVersion))) {
                        logger.warning("chrome driver for version:" + chromeVersion + " is not available at this moment. https://chromedriver.storage.googleapis.com/index.html");
                        return Optional.empty();
                    }
                    download(downloadURL, archivePath, installRootPath, bin);
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
    public Optional<String> getInstalledChromeVersion() {
        try {
            String chromePath = "";
            switch (DETECTED_OS) {
                case MAC:
                    chromePath = "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome";
                    break;
                case LINUX32:
                case LINUX64:
                    chromePath = "/usr/bin/google-chrome";
                    break;
                case WINDOWS32:
                case WINDOWS64:
                    chromePath = getAppPath("chrome.exe");
                    break;
                case UNKNOWN:
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
            logger.warning("Failed to locate Google Chrome");
            e.printStackTrace();
            return Optional.empty();
        }
    }


    List<String> listAvailableChromeDriverVersions() {
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
