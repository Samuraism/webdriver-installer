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
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Logger;

@SuppressWarnings("WeakerAccess")
public final class GeckodriverInstaller {
    private final static Logger logger = Logger.getLogger("com.samuraism.chromedriverinstaller.FirefoxDriverInstaller");

    public static void main(String[] args) {
        // install geckodriver in /tmp/geckodriver
        // This ensures gecko driver to be installed at /tmp/geckodriver
        // "webdriver.gecko.driver" system property will be also set.
        Optional<String> path = GeckodriverInstaller.ensureInstalled("/tmp/geckodriver");
        if (!path.isPresent()) {
            logger.warning("Failed to install geckodriver");
        }
    }

    static String getGeckoDriverVersionFromFirefoxVersion(String firefoxVersion) {
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

    static String toURL(String version, Util.OS os) {
        return String.format("https://github.com/mozilla/geckodriver/releases/download/%s/%s",
                version, toFileName(version, os));
    }

    static String toFileName(String version, Util.OS os) {
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

    // https://firefox-source-docs.mozilla.org/testing/geckodriver/Support.html
    // Firefox 60+:  0.29.0
    // https://github.com/mozilla/geckodriver/releases/download/v0.29.0/geckodriver-v0.29.0-linux32.tar.gz
    // https://github.com/mozilla/geckodriver/releases/download/v0.29.0/geckodriver-v0.29.0-linux64.tar.gz
    // https://github.com/mozilla/geckodriver/releases/download/v0.29.0/geckodriver-v0.29.0-macos.tar.gz
    // https://github.com/mozilla/geckodriver/releases/download/v0.29.0/geckodriver-v0.29.0-win32.zip
    // https://github.com/mozilla/geckodriver/releases/download/v0.29.0/geckodriver-v0.29.0-win64.zip
    // Firefox 57+: 0.25.0
    // https://github.com/mozilla/geckodriver/releases/download/v0.25.0/geckodriver-v0.25.0-linux32.tar.gz
    // https://github.com/mozilla/geckodriver/releases/download/v0.25.0/geckodriver-v0.25.0-linux64.tar.gz
    // https://github.com/mozilla/geckodriver/releases/download/v0.25.0/geckodriver-v0.25.0-macos.tar.gz
    // https://github.com/mozilla/geckodriver/releases/download/v0.25.0/geckodriver-v0.25.0-win32.zip
    // https://github.com/mozilla/geckodriver/releases/download/v0.25.0/geckodriver-v0.25.0-win64.zip
    // Other: 0.20.1
    // https://github.com/mozilla/geckodriver/releases/download/v0.21.0/geckodriver-v0.21.0-linux32.tar.gz
    // https://github.com/mozilla/geckodriver/releases/download/v0.21.0/geckodriver-v0.21.0-linux64.tar.gz
    // https://github.com/mozilla/geckodriver/releases/download/v0.21.0/geckodriver-v0.21.0-macos.tar.gz
    // https://github.com/mozilla/geckodriver/releases/download/v0.21.0/geckodriver-v0.21.0-win32.zip
    // https://github.com/mozilla/geckodriver/releases/download/v0.21.0/geckodriver-v0.21.0-win64.zip

    private static boolean initialized = false;

    /**
     * ensure geckodriver is installed on the specified directory
     *
     * @param installRoot directory to be installed
     * @return path to the geckodriver binary
     */
    public synchronized static Optional<String> ensureInstalled(String installRoot) {
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
        final String fileName = toFileName(geckoDriverVersion, Util.DETECTED_OS);
        // ex) /root/firefoxDriver/0.29.0/geckodriver-v0.29.0-linux64.tar.gz
        Path arcihvePath = installRootPath.resolve(fileName);
        String binName = "geckodriver" + ((Util.DETECTED_OS == Util.OS.WINDOWS32 || Util.DETECTED_OS == Util.OS.WINDOWS64) ? ".exe" : "");
        // ex) /root/firefoxDriver/0.29.0/geckodriver
        final Path bin = installRootPath.resolve(binName).toAbsolutePath();
        String geckodriver = bin.toString();
        // download geckodriver
        String downloadURL = toURL(geckoDriverVersion, Util.DETECTED_OS);
        if (!initialized) {
            try {
                if (Files.exists(bin)) {
                    logger.info("geckodriver is installed at: " + bin.toAbsolutePath().toString());
                    initialized = true;
                } else {
                    Files.createDirectories(installRootPath);
                    //noinspection ResultOfMethodCallIgnored
                    arcihvePath.toFile().delete();
                    URL url = new URL(downloadURL);
                    HttpURLConnection con = null;
                    try {
                        con = (HttpURLConnection) url.openConnection();
                        con.setReadTimeout(5000);
                        con.setConnectTimeout(5000);
                        int code = con.getResponseCode();
                        if (code == 200) {
                            Files.copy(con.getInputStream(), arcihvePath);
                        } else {
                            throw new IOException("URL[" + url + "] returns code [" + code + "].");
                        }
                    } finally {
                        if (con != null) {
                            con.disconnect();
                        }
                    }
                    Util.decompress(arcihvePath, installRootPath);
                    //noinspection ResultOfMethodCallIgnored
                    bin.toFile().setExecutable(true);
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
    public static Optional<String> getInstalledFirefoxVersion() {
        try {
            String firefoxPath = "";
            switch (Util.DETECTED_OS) {
                case MAC:
                    firefoxPath = "/Applications/Firefox.app/Contents/MacOS/firefox-bin";
                    break;
                case LINUX32:
                case LINUX64:
                    firefoxPath = "/usr/bin/firefox";
                    break;
                case WINDOWS32:
                case WINDOWS64:
                    return getInstalledFirefoxVersionForWindows();
                case UNKNOWN:
                    throw new UnsupportedOperationException("Not yet supported");
            }
            if (!new File(firefoxPath).exists()) {
                logger.warning("Firefox not found at " + firefoxPath);
                return Optional.empty();
            }
            final String result = Util.execute(new File("/"), new String[]{firefoxPath, "-version"});
            final String versionString = result.substring("Mozilla Firefox ".length()).trim();
            return Optional.of(versionString);
        } catch (IOException | InterruptedException e) {
            logger.warning("Failed to locate Firefox");
            e.printStackTrace();
            return Optional.empty();
        }
    }

    //"\Program Files\Mozilla Firefox\firefox.exe" -v|more
    private static Optional<String> getInstalledFirefoxVersionForWindows() throws IOException, InterruptedException {
        final File currentDir = new File(".");
        final String firefoxPath = Util.execute(currentDir, new String[]{"powershell", "-command", "(Get-ItemProperty -ErrorAction Stop -Path \\\"HKLM:SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\App Paths\\firefox.exe\\\").'(default)'"});

        // See: How to get firefox version using command prompt in windows
        // https://stackoverflow.com/a/57618035/1932017
        final String versionString = Util.execute(currentDir, new String[]{"powershell", "-command", "(Get-Item -ErrorAction Stop \\\"" + firefoxPath.trim() + "\\\").VersionInfo.ProductVersion"});

        return Optional.of(versionString.trim());
    }
}
