# [&#35;chromeDriverInstaller](https://twitter.com/search?q=%23chromeDriverInstaller&src=typed_query&f=live)
chrome-driver-installer is a tiny utility class which detects the version of the installed Google Chrome and installs chrome driver in the specified location.

## hashtag
[&#35;chromeDriverInstaller](https://twitter.com/intent/tweet?text=https://github.com/samuraism/chrome-driver-installer/+%23chromeDriverInstaller)
## System Requirements
Java: Java 8+

OS: Windows, Linux, macOS

## Adding chrome-driver-installer to your project
This library is available at the Maven Central Repository

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.samuraism/chrome-driver-installer/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.samuraism/chrome-driver-installer)
### Maven
```xml
<dependencies>
    <dependency>
        <groupId>com.samuraism</groupId>
        <artifactId>chrome-driver-installer</artifactId>
        <version>1.3</version>
    </dependency>
</dependencies>
```
### Gradle
```text
dependencies {
    compile 'com.samuraism:chrome-driver-installer:1.3'
}
```
## How to use
Call ensureInstalled method to ensure the driver is installed.
```java
package yourpackage;
import com.samuraism.chromedriverinstaller.ChromeDriverInstaller;
public class Example {
    public static void main(String[] args) {
        // install Chrome Driver in /tmp/chromedriver
        // This ensures chrome driver to be installed at /tmp/chromedriver
        // "webdriver.chrome.driver" system property will be also set.
        Optional<String> path = ChromeDriverInstaller.ensureInstalled("/tmp/chromedriver");
        if(path.isPresent()){
            System.out.println("Chrome Driver is installed at:" + path.get());
        }else {
            System.out.println("Failed to install Chrome Driver");
        }
    }
}
```

# License
Apache License Version 2.0

![Java CI with Gradle](https://github.com/Samuraism/chrome-driver-installer/workflows/Java%20CI%20with%20Gradle/badge.svg)