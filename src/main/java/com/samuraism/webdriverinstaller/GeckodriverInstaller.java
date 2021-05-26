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

import java.util.Optional;

@SuppressWarnings("WeakerAccess")
final class GeckodriverInstaller extends com.samuraism.webdriverinstaller.WebDriverInstaller {
    private final static Logger logger = Logger.getLogger();

    public static void main(String... args) {
        // installs geckodriver in the path specified by argument, or the path specified by GECKO_DRIVER_HOME environment variable, or $HOME/geckodriver
        // "webdriver.gecko.driver" system property will be also set.
        if (0 < args.length) {
            System.setProperty(GECKO_DRIVER_PROPERTY_NAME, args[0]);
        }
        Optional<String> path = WebDriverInstaller.ensureGeckoDriverInstalled();
        if (path.isPresent()) {
            logger.info(() -> "geckodriver installed at: " + path.get());
        } else {
            logger.warn(() -> "Failed to install geckodriver");
        }
    }

    GeckodriverInstaller() {
        super("webdriver.gecko.driver", "Firefox", "geckodriver", "firefox", "/Applications/Firefox.app/Contents/MacOS/firefox-bin", "firefox.exe");
    }

    @Override
    String toFileName(String version) {
        String osString = choose("linux32", "linux64", "macos", "win32", "win64");
        String suffix = choose(".tar.gz", ".tar.gz", ".tar.gz", ".zip", ".zip");
        return String.format("geckodriver-%s-%s%s", version, osString, suffix);
    }

    @Override
    String getDownloadURL(String version, String fileName) {
        return String.format("https://github.com/mozilla/geckodriver/releases/download/%s/%s",
                version, fileName);
    }

    @Override
    String getSuitableDriverVersion(String firefoxVersion) {
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


}
