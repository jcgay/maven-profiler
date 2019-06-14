# Maven Profiler

A time execution recorder for Maven which log time taken by each mojo in your build lifecycle.

## Installation

`$M2_HOME` refers to maven installation folder.

```
.
├── bin
├── boot
├── conf
└── lib
```

### OS X ?

You can install a pre-packaged maven named [maven-deluxe](https://github.com/jcgay/homebrew-jcgay#maven-deluxe) using `brew`.
It comes with [maven-color](https://github.com/jcgay/maven-color), [maven-notifier](https://github.com/jcgay/maven-notifier) and [maven-profiler](https://github.com/jcgay/maven-profiler).
It is based on latest maven release.

    brew tap jcgay/jcgay
    brew install maven-deluxe

### Maven >= 3.3.x

Get [maven-profiler](http://dl.bintray.com/jcgay/maven/fr/jcgay/maven/maven-profiler/2.6/maven-profiler-2.6-shaded.jar) and copy it in `%M2_HOME%/lib/ext` folder.

*or*

Use the new [core extensions configuration mechanism](http://takari.io/2015/03/19/core-extensions.html) by creating a `${maven.multiModuleProjectDirectory}/.mvn/extensions.xml` file with:

	<?xml version="1.0" encoding="UTF-8"?>
	<extensions>
	    <extension>
	      <groupId>fr.jcgay.maven</groupId>
	      <artifactId>maven-profiler</artifactId>
	      <version>2.6</version>
	    </extension>
	</extensions>

### Maven >= 3.1.x

Get [maven-profiler](http://dl.bintray.com/jcgay/maven/fr/jcgay/maven/maven-profiler/2.6/maven-profiler-2.6-shaded.jar) and copy it in `%M2_HOME%/lib/ext` folder.

### Maven 3.0.x
(with limited functionality, kept for compatibility)
Get [maven-profiler](http://dl.bintray.com/jcgay/maven/com/github/jcgay/maven/maven-profiler/1.0/maven-profiler-1.0.jar) and copy it in `%M2_HOME%/lib/ext` folder.

## Usage

Use property `profile` when running Maven.

	mvn install -Dprofile

This will generate a report in `.profiler` folder.

You might also add a profile name, which is included in the [report](#report-format)
and helps identify the experiment:
    
    mvn clean install -Dprofile="No custom JVM options"
    export MAVEN_OPTS='-XX:TieredStopAtLevel=1 -XX:+UseParallelGC'
    mvn clean install -Dprofile="With custom JVM options=${MAVEN_OPTS}"

The extension also works when `mvn` is executed on multiple threads (option `-T`).

### Report format

One can choose between `HTML` (by default) or `JSON` report using property `profileFormat`. 

    mvn install -Dprofile -DprofileFormat=HTML

Or you can compose multiple reporters separated by comma:

    mvn install -Dprofile -DprofileFormat=JSON,HTML

### Change sorting

Also you can add the property `disableTimeSorting` if you want the reported times to be in the order of execution instead of sorted by execution time.

    mvn install -Dprofile -DdisableTimeSorting

### Report directory

Report default directory (`.profiler`) can be customized.  
You can set it as a Maven property, for example in `pom.xml`:

```
<properties>
    <maven-profiler-report-directory>${project.build.directory}/custom-directory</maven-profiler-report-directory>
</properties>
```

or you can define it using a system property:

    mvn install -Dprofile -Dmaven-profiler-report-directory=/tmp/profiler-custom-report

## Output examples

### HTML

	mvn install -Dprofile

[![maven-profiler](http://jeanchristophegay.com/images/maven-profiler-resize.png)](http://jeanchristophegay.com/images/maven-profiler.png)

### JSON

	mvn install -Dprofile -DprofileFormat=JSON

```
{
  "name": "maven-profiler",
  "profile_name": "",
  "time": "44681 ms",
  "goals": "clean install",
  "date": "2017/01/21 19:10:04",
  "parameters": "{profile=true, profileFormat=JSON}",
  "projects": [
    {
      "project": "maven-profiler",
      "time": "43378 ms",
      "mojos": [
        {
          "mojo": "org.apache.maven.plugins:maven-invoker-plugin:2.0.0:run {execution: integration-test}",
          "time": "30706 ms"
        },
        {
          "mojo": "org.apache.maven.plugins:maven-surefire-plugin:2.19.1:test {execution: default-test}",
          "time": "7300 ms"
        },
        {
          "mojo": "org.apache.maven.plugins:maven-shade-plugin:2.4.3:shade {execution: default}",
          "time": "1378 ms"
        },
        {
          "mojo": "org.apache.maven.plugins:maven-compiler-plugin:3.6.0:compile {execution: default-compile}",
          "time": "1112 ms"
        },
        {
          "mojo": "org.codehaus.gmavenplus:gmavenplus-plugin:1.5:testCompile {execution: default}",
          "time": "1102 ms"
        },
        {
          "mojo": "org.apache.maven.plugins:maven-invoker-plugin:2.0.0:install {execution: integration-test}",
          "time": "293 ms"
        },
        {
          "mojo": "org.apache.maven.plugins:maven-enforcer-plugin:1.4.1:enforce {execution: enforce-maven}",
          "time": "225 ms"
        },
        {
          "mojo": "org.apache.maven.plugins:maven-clean-plugin:3.0.0:clean {execution: default-clean}",
          "time": "221 ms"
        },
        {
          "mojo": "org.codehaus.plexus:plexus-component-metadata:1.7.1:generate-metadata {execution: default}",
          "time": "195 ms"
        },
        {
          "mojo": "org.apache.maven.plugins:maven-jar-plugin:3.0.2:jar {execution: default-jar}",
          "time": "167 ms"
        },
        {
          "mojo": "org.apache.maven.plugins:maven-source-plugin:3.0.1:jar-no-fork {execution: attach-sources}",
          "time": "138 ms"
        },
        {
          "mojo": "org.apache.maven.plugins:maven-resources-plugin:3.0.2:resources {execution: default-resources}",
          "time": "106 ms"
        },
        {
          "mojo": "org.apache.maven.plugins:maven-toolchains-plugin:1.1:toolchain {execution: default}",
          "time": "72 ms"
        },
        {
          "mojo": "org.apache.maven.plugins:maven-install-plugin:2.5.2:install {execution: default-install}",
          "time": "46 ms"
        },
        {
          "mojo": "org.apache.maven.plugins:maven-resources-plugin:3.0.2:testResources {execution: default-testResources}",
          "time": "2 ms"
        },
        {
          "mojo": "org.apache.maven.plugins:maven-compiler-plugin:3.6.0:testCompile {execution: default-testCompile}",
          "time": "2 ms"
        }
      ]
    }
  ]
}
```

## Build status

[![Build Status](https://travis-ci.org/jcgay/maven-profiler.png)](https://travis-ci.org/jcgay/maven-profiler)
[![Coverage Status](https://coveralls.io/repos/jcgay/maven-profiler/badge.svg?branch=master)](https://coveralls.io/r/jcgay/maven-profiler?branch=master)
[![Quality Gate](https://sonarcloud.io/api/project_badges/measure?project=fr.jcgay.maven%3Amaven-profiler&metric=alert_status)](https://sonarqube.com/dashboard/index/fr.jcgay.maven:maven-profiler)
[![Technical debt ratio](https://sonarcloud.io/api/project_badges/measure?project=fr.jcgay.maven%3Amaven-profiler&metric=sqale_index)](https://sonarqube.com/dashboard/index/fr.jcgay.maven:maven-profiler)

## Release

    mvn -B release:prepare release:perform
