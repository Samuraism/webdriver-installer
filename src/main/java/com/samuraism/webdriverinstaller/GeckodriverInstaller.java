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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.logging.Logger;

@SuppressWarnings("WeakerAccess")
public final class GeckodriverInstaller extends com.samuraism.webdriverinstaller.WebDriverInstaller {
    private final static Logger logger = Logger.getLogger("com.samuraism.webdriverinstaller.FirefoxDriverInstaller");

    public static void main(String[] args) {
        // install geckodriver in /tmp/geckodriver
        // This ensures gecko driver to be installed at /tmp/geckodriver
        // "webdriver.gecko.driver" system property will be also set.
        Optional<String> path = new GeckodriverInstaller().ensureInstalled(System.getProperty("user.home")
                + File.separator + "geckodriver");
        if (path.isPresent()) {
            logger.info("geckodriver installed at: " + path.get());
        } else {
            logger.warning("Failed to install geckodriver");
        }
    }

    String getGeckoDriverVersionFromFirefoxVersion(String firefoxVersion) {
        final String version = firefoxVersion.trim().replaceAll("\\..*", "");
        final int intVersion = Integer.parseInt(version);
        if (intVersion < 57) {
            return "v0.20.1";
        }
        if (intVersion < 60) {
            return "v0.25.0";
        }
        return "v0.29.0";
    }

    String toURL(String version, OS os) {
        return String.format("https://github.com/mozilla/geckodriver/releases/download/%s/%s",
                version, toFileName(version, os));
    }

    String toFileName(String version, OS os) {
        String osString;
        String suffix;
        switch (os) {
            case MAC:
                osString = "macos";
                suffix = ".tar.gz";
                break;
            case LINUX64:
                osString = "linux64";
                suffix = ".tar.gz";
                break;
            case LINUX32:
                osString = "linux32";
                suffix = ".tar.gz";
                break;
            case WINDOWS32:
                osString = "win32";
                suffix = ".zip";
                break;
            case WINDOWS64:
                osString = "win64";
                suffix = ".zip";
                break;
            default:
                throw new UnsupportedOperationException("Not yet supported");
        }
        return String.format("geckodriver-%s-%s%s", version, osString, suffix);
    }

    private boolean initialized = false;

    /**
     * ensure geckodriver is installed on the specified directory
     *
     * @param installRoot directory to be installed
     * @return path to the geckodriver binary
     */
    public synchronized Optional<String> ensureInstalled(String installRoot) {
        final Optional<String> installedVersion = getInstalledFirefoxVersion();
        if (!installedVersion.isPresent()) {
            return Optional.empty();
        }
        // ex) 85.0
        final String firefoxVersion = installedVersion.get();
        // ex) getGeckoDriverVersionFromFirefoxVersion
        final String geckoDriverVersion = getGeckoDriverVersionFromFirefoxVersion(firefoxVersion);
        // ex) /root/firefoxDriver/0.29.0
        Path installRootPath = Paths.get(installRoot, geckoDriverVersion);

        // ex) geckodriver-v0.29.0-linux64.tar.gz
        final String fileName = toFileName(geckoDriverVersion, DETECTED_OS);
        // ex) /root/firefoxDriver/0.29.0/geckodriver-v0.29.0-linux64.tar.gz
        Path archivePath = installRootPath.resolve(fileName);
        String binName = "geckodriver" + (isWin() ? ".exe" : "");
        // ex) /root/firefoxDriver/0.29.0/geckodriver
        final Path bin = installRootPath.resolve(binName).toAbsolutePath();
        String geckodriver = bin.toString();
        // download geckodriver
        String downloadURL = toURL(geckoDriverVersion, DETECTED_OS);
        if (!initialized) {
            try {
                if (Files.exists(bin)) {
                    logger.info("geckodriver already is installed at: " + bin.toAbsolutePath().toString());
                    initialized = true;
                } else {
                    download(downloadURL, archivePath, installRootPath, bin);
                }
                System.setProperty("webdriver.gecko.driver", geckodriver);
                initialized = true;
            } catch (IOException ioe) {
                logger.warning("Failed to download: " + downloadURL);
                ioe.printStackTrace();
            }
        }
        return Optional.of(geckodriver);
    }

    /**
     * Returns version string of installed firefox.
     *
     * @return version string of installed firefox
     */
    public Optional<String> getInstalledFirefoxVersion() {
        try {
            String firefoxPath = "";
            switch (DETECTED_OS) {
                case MAC:
                    firefoxPath = "/Applications/Firefox.app/Contents/MacOS/firefox-bin";
                    break;
                case LINUX32:
                case LINUX64:
                    firefoxPath = getAppPath("firefox");
                    break;
                case WINDOWS32:
                case WINDOWS64:
                    firefoxPath = getAppPath("firefox.exe");
                    break;
                case UNKNOWN:
                    throw new UnsupportedOperationException("Not yet supported");
            }
            if (!new File(firefoxPath).exists()) {
                logger.warning("Firefox not found at " + firefoxPath);
                return Optional.empty();
            }
            final String result = getAppVersion(firefoxPath);
            final String versionString = result.substring(result.lastIndexOf(" ") + 1);
            return Optional.of(versionString);
        } catch (IOException | InterruptedException e) {
            logger.warning("Failed to locate Firefox");
            e.printStackTrace();
            return Optional.empty();
        }
    }
}
