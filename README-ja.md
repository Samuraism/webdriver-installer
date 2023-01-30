# [&#35;webdriverInstaller](https://twitter.com/search?q=%23webdriverInstaller&src=typed_query&f=live)

webdriver-installerはインストールされているGoogle ChromeまたはFirefoxのバージョンを検出して、最適なバージョンのwebdriverを指定した場所にインストールしてくれる小さなユーティリティです。

## ハッシュタグ

[&#35;webdriverInstaller](https://twitter.com/intent/tweet?text=https://github.com/samuraism/chrome-driver-installer/+%23webdriverInstaller)

## システム要件

Java: Java 8+

OS: Windows, Linux, macOS

## chrome-driver-installer をプロジェクトに追加する

このライブラリはMaven Central Repositoryにあります。

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/one.cafebabe/chrome-driver-installer/badge.svg)](https://maven-badges.herokuapp.com/maven-central/one.cafebabe/chrome-driver-installer)
### Maven

```xml

<dependencies>
    <dependency>
        <groupId>one.cafebabe</groupId>
        <artifactId>webdriver-installer</artifactId>
        <version>1.10</version>
    </dependency>
</dependencies>
```

### Gradle

```text
dependencies {
    compile 'one.cafebabe:webdriver-installer:1.10'
}
```

### Java modularity

```text
require one.cafebabe.webdriverinstaller
```

## 使い方

ensureChrome/GeckoDriverInstalled メソッドを呼び出せばドライバが $HOME/chromedriver / $HOME/geckodriver インストールされている状態になります。 環境変数
CHROME_DRIVER_HOME / GECKO_DRIVER_HOME、またはシステムプロパティ or chromedriver.home / geckodriver.home でインストールする場所を指定することもできます。

```java
package yourpackage;

import one.cafebabe.webdriverinstaller.WebDriverInstaller;

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