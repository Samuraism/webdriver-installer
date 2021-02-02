package com.samuraism.webdriverinstaller;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GeckodriverInstallerTest {

    @Test
    void getGeckoDriverVersionFromFirefoxVersion(){
        // Firefox 60+:  0.29.0
        final GeckodriverInstaller geckodriverInstaller = new GeckodriverInstaller();
        assertEquals("v0.29.0", geckodriverInstaller.getSuitableDriverVersion("60.1"));
        assertEquals("v0.29.0", geckodriverInstaller.getSuitableDriverVersion("68.0b14"));
        assertEquals("v0.29.0", geckodriverInstaller.getSuitableDriverVersion("70.2"));
        assertEquals("v0.29.0", geckodriverInstaller.getSuitableDriverVersion("78.6.1esr"));
        assertEquals("v0.29.0", geckodriverInstaller.getSuitableDriverVersion("80"));
        assertEquals("v0.29.0", geckodriverInstaller.getSuitableDriverVersion("85.0"));
        assertEquals("v0.29.0", geckodriverInstaller.getSuitableDriverVersion("80.5"));
        assertEquals("v0.29.0", geckodriverInstaller.getSuitableDriverVersion("90.1.1"));

        // Firefox 57+: 0.25.0
        assertEquals("v0.25.0", geckodriverInstaller.getSuitableDriverVersion("57"));
        assertEquals("v0.25.0", geckodriverInstaller.getSuitableDriverVersion("57.1"));
        assertEquals("v0.25.0", geckodriverInstaller.getSuitableDriverVersion("58.0"));
        assertEquals("v0.25.0", geckodriverInstaller.getSuitableDriverVersion("58.2"));
        assertEquals("v0.25.0", geckodriverInstaller.getSuitableDriverVersion("59.1.1"));
        assertEquals("v0.25.0", geckodriverInstaller.getSuitableDriverVersion("59.5"));


        // Other: 0.20.1
        assertEquals("v0.20.1", geckodriverInstaller.getSuitableDriverVersion("37.1"));
        assertEquals("v0.20.1", geckodriverInstaller.getSuitableDriverVersion("38.2"));
        assertEquals("v0.20.1", geckodriverInstaller.getSuitableDriverVersion("39.5"));
        assertEquals("v0.20.1", geckodriverInstaller.getSuitableDriverVersion("47"));
        assertEquals("v0.20.1", geckodriverInstaller.getSuitableDriverVersion("55.0"));
        assertEquals("v0.20.1", geckodriverInstaller.getSuitableDriverVersion("56.1.1"));

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

}