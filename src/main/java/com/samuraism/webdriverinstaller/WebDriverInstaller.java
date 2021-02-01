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
import java.util.Arrays;
import java.util.Enumeration;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/*protected*/ class WebDriverInstaller {
    static final OS DETECTED_OS;

    enum OS {
        MAC,
        LINUX32,
        LINUX64,
        WINDOWS32,
        WINDOWS64,
        UNKNOWN
    }

    static boolean isWin() {
        return DETECTED_OS == OS.WINDOWS32 || DETECTED_OS == OS.WINDOWS64;
    }

    static {
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

    static void download(String downloadURL, Path arcihvePath, Path installRootPath, Path bin) throws IOException {
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
        decompress(arcihvePath, installRootPath);
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
    static String getAppPath(String name) throws IOException, InterruptedException {
        return execute(new File("."), new String[]{"powershell", "-command",
                String.format("(Get-ItemProperty -ErrorAction Stop -Path \\\"HKLM:SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\App Paths\\%s\\\").'(default)'", name)});
    }

    static String getAppVersion(String appPath) throws IOException, InterruptedException {
        return isWin() ?
                execute(new File("."), new String[]{"powershell", "-command",
                        "(Get-Item -ErrorAction Stop \\\"" + appPath + "\\\").VersionInfo.ProductVersion"})
                : execute(new File("/"),
                new String[]{"/bin/bash", "-c", String.format("`which %s` -version", appPath)});
    }
}
