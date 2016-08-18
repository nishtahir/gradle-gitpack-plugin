# Gradle Gitpack plugin

Inspired by Jitpack.io, this plugin allows you to add public github or gitlab repositories
as dependencies directly to your java projects.

# Usage

Add the plugin to your buildscript

```
buildscript {
    repositories {
      mavenCentral()
      mavenLocal()
    }

    dependencies {
       classpath 'com.nishtahir:gradle-gitlib-plugin:1.0-SNAPSHOT'
    }
}
```

And apply the plugin to your build

```
apply plugin: 'com.nishtahir.gradle-gitlib-plugin'
```

Declare compile time dependencies using `git`
```
dependencies {
   git 'com.github.nishtahir:ALang:SNAPSHOT'
}
```

they should be in the format

```
git com.[github/gitlab].[username]:[project name]:[version]
```

