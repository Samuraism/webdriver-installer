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
package one.cafebabe.webdriverinstaller;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ChromeDriverInstallerTest {
    @Test
    void versions() {
        final List<String> versions = new ChromeDriverInstaller().listAvailableChromeDriverVersions();
        for (String version : versions) {
            System.out.println(version);
        }
        assertTrue(90 <= versions.size());
        assertTrue(versions.contains("2.0"));
        assertTrue(versions.contains("74.0.3729.6"));
        assertTrue(versions.contains("88.0.4324.96"));
    }

    @Test
    void getSuitableDriverVersion() {
        final List<String> availableVersions = Arrays.asList("2.0", "2.1", "2.10", "2.11", "2.12", "2.13", "2.14", "2.15", "2.16", "2.17", "2.18", "2.19", "2.2", "2.20", "2.21", "2.22", "2.23", "2.24", "2.25", "2.26", "2.27", "2.28", "2.29", "2.3", "2.30", "2.31", "2.32", "2.33", "2.34", "2.35", "2.36", "2.37", "2.38", "2.39", "2.4", "2.40", "2.41", "2.42", "2.43", "2.44", "2.45", "2.46", "2.5", "2.6", "2.7", "2.8", "2.9", "70.0.3538.16", "70.0.3538.67", "70.0.3538.97", "71.0.3578.137", "71.0.3578.30", "71.0.3578.33", "71.0.3578.80", "72.0.3626.69", "72.0.3626.7", "73.0.3683.20", "73.0.3683.68", "74.0.3729.6", "75.0.3770.140", "75.0.3770.8", "75.0.3770.90", "76.0.3809.12", "76.0.3809.126", "76.0.3809.25", "76.0.3809.68", "77.0.3865.10", "77.0.3865.40", "78.0.3904.105", "78.0.3904.11", "78.0.3904.70", "79.0.3945.16", "79.0.3945.36", "80.0.3987.106", "80.0.3987.16", "81.0.4044.138", "81.0.4044.20", "81.0.4044.69", "83.0.4103.14", "83.0.4103.39", "84.0.4147.30", "85.0.4183.38", "85.0.4183.83", "85.0.4183.87", "86.0.4240.22", "87.0.4280.20", "87.0.4280.87", "87.0.4280.88", "88.0.4324.27", "88.0.4324.96", "89.0.4389.23");

        final ChromeDriverInstaller installer = new ChromeDriverInstaller();
        assertAll(
                () -> assertEquals("81.0.4044.69", installer.getSuitableDriverVersion(availableVersions, "82.0.4103.14"))
                , () -> assertEquals("88.0.4324.96", installer.getSuitableDriverVersion(availableVersions, "88.0.4324.96"))
                , () -> assertEquals("88.0.4324.96", installer.getSuitableDriverVersion(availableVersions, "88.0.4324.104"))

                , () -> assertEquals("89.0.4389.23", installer.getSuitableDriverVersion(availableVersions, "89.0.5960.49"))

                , () -> assertEquals("89.0.4389.23", installer.getSuitableDriverVersion(availableVersions, "95.0.1023.00"))
        );
        for (String listAvailableChromeDriverVersion : installer.listAvailableChromeDriverVersions()) {
            System.out.println(listAvailableChromeDriverVersion);
        }
        System.out.println(installer.getSuitableDriverVersion(installer.listAvailableChromeDriverVersions(), "91.0.4472.114"));

    }
}