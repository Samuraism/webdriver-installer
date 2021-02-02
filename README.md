# [&#35;weberiverInstaller](https://twitter.com/search?q=%23weberiverInstaller&src=typed_query&f=live)

webdriver-installer is a utility which detects the version of the installed Google Chrome, or Firefox and installs
appropriate version of web driver in the specified location.

## hashtag

[&#35;weberiverInstaller](https://twitter.com/intent/tweet?text=https://github.com/samuraism/webdriver-installer/+%23weberiverInstaller)

## System Requirements

Java: Java 8+

OS: Windows, Linux, macOS

## Adding webdriver-installer to your project

This library is available at the Maven Central Repository

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.samuraism/webdriver-installer/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.samuraism/webdriver-installer)
### Maven

```xml

<dependencies>
    <dependency>
        <groupId>com.samuraism</groupId>
        <artifactId>webdriver-installer</artifactId>
        <version>1.4</version>
    </dependency>
</dependencies>
```
### Gradle
```text
dependencies {
    compile 'com.samuraism:webdriver-installer:1.4'
}
```
## How to use

Call Chrome/GeckoDriver method to ensure the driver is installed at $HOME/chromedriver / $HOME/geckodriver You can
specify the location via CHROME_DRIVER_HOME / GECKO_DRIVER_HOME environment variable, or chromedriver.home /
geckodriver.home system property.

```java
package yourpackage;

import com.samuraism.webdriverinstaller.WebDriverInstaller;

public class Example {
    public static void main(String[] args) {
        // install Chrome Driver in $HOME/chromedriver
        // "webdriver.chrome.driver" system property will be also set.
        Optional<String> chromeDriverPath = WebDriverInstaller.ensureChromeDriverInstalled();
        if (chromeDriverPath.isPresent()) {
            System.out.println("Chrome Driver is installed at:" + chromeDriverPath.get());
        } else {
            throw new ExceptionInInitializerError("Failed to install Chrome Driver");
        }

        // install geckodriver in $HOME/geckodriver
        // "webdriver.gecko.driver" system property will be also set.
        Optional<String> geckodriverPath = WebDriverInstaller.ensureGeckoDriverInstalled();
        if (geckodriverPath.isPresent()) {
            System.out.println("geckodriver is installed at:" + geckodriverPath.get());
        } else {
            throw new ExceptionInInitializerError("Failed to install geckodriver");
        }
    }
}
```

# License

Apache License Version 2.0

![Java CI with Gradle](https://github.com/Samuraism/webdriver-installer/workflows/Java%20CI%20with%20Gradle/badge.svg)