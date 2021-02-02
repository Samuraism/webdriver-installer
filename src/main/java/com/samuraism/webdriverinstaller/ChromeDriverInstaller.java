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
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

@SuppressWarnings("WeakerAccess")
final class ChromeDriverInstaller extends com.samuraism.webdriverinstaller.WebDriverInstaller {
    private final static Logger logger = Logger.getLogger("com.samuraism.webdriverinstaller.ChromeDriverInstaller");

    public static void main(String... args) {
        // installs ChromeDriver in the path specified by argument, or the path specified by GECKO_DRIVER_HOME environment variable, or $HOME/geckodriver
        // "webdriver.chrome.driver" system property will be also set.
        if (0 < args.length) {
            System.setProperty(CHROME_DRIVER_PROPERTY_NAME, args[0]);
        }
        Optional<String> path = WebDriverInstaller.ensureChromeDriverInstalled();
        if (path.isPresent()) {
            logger.info("ChromeDriver installed at: " + path.get());
        } else {
            logger.warning("Failed to install ChromeDriver");
        }
    }

    ChromeDriverInstaller() {
        super("webdriver.chrome.driver", "Google Chrome", "chromedriver", "google-chrome", "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome", "chrome.exe");
    }

    @Override
    String toFileName(String version) {
        return "chromedriver_" + choose("linux32", "linux64", "mac64", "win32", "win32") + ".zip";
    }

    @Override
    String getDownloadURL(String version, String fileName) {
        return "https://chromedriver.storage.googleapis.com/" + version + "/" + fileName;
    }

    @Override
    String getSuitableDriverVersion(String installedVersion) {
        List<String> availableVersions = listAvailableChromeDriverVersions();
        return getSuitableDriverVersion(availableVersions, installedVersion);
    }

    String getSuitableDriverVersion(List<String> availableVersions, String installedVersion) {
        listAvailableChromeDriverVersions();
        if (availableVersions.contains(installedVersion)) {
            return installedVersion;
        }
        logger.info(String.format("ChromeDriver version %s is not available.", installedVersion));
        String fallbackVersion = null;
        while (fallbackVersion == null && installedVersion.contains(".")) {
            // When Chrome version is 88.0.4324.104 and chrome driver version 88.0.4324.104 is not available,
            // look up the latest 88.0.4324.**, 88.0.** …
            installedVersion = installedVersion.substring(0, installedVersion.lastIndexOf("."));
            for (int i = availableVersions.size() - 1; 0 <= i; i--) {
                if (availableVersions.get(i).contains(installedVersion)) {
                    fallbackVersion = availableVersions.get(i);
                    break;
                }
            }
        }
        if (fallbackVersion == null) {
            // find the latest, but older major version
            int installedMajorVersion = Integer.parseInt(installedVersion);
            for (int i = installedMajorVersion - 1; 0 < i && fallbackVersion == null; i--) {
                String checkVersion = String.valueOf(i);
                for (int j = availableVersions.size() - 1; 0 <= j; j--) {
                    String majorVersion = availableVersions.get(j).substring(0, availableVersions.get(j).indexOf("."));
                    if (majorVersion.contains(checkVersion)) {
                        fallbackVersion = availableVersions.get(j);
                        break;
                    }
                }
            }
        }
        assert (fallbackVersion != null);
        logger.info(String.format("Fallback to Chrome Driver version %s.", fallbackVersion));
        return fallbackVersion;
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
