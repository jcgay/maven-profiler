#Maven Profiler

A time execution recorder for Maven which log time taken by each mojo in your build lifecycle.

##Installation

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

Get [maven-profiler](http://dl.bintray.com/jcgay/maven/fr/jcgay/maven/maven-profiler/2.3/maven-profiler-2.3-shaded.jar) and copy it in `%M2_HOME%/lib/ext` folder.

*or*

Use the new [core extensions configuration mechanism](http://takari.io/2015/03/19/core-extensions.html) by creating a `${maven.multiModuleProjectDirectory}/.mvn/extensions.xml` file with:

	<?xml version="1.0" encoding="UTF-8"?>
	<extensions>
	    <extension>
	      <groupId>fr.jcgay.maven</groupId>
	      <artifactId>maven-profiler</artifactId>
	      <version>2.3</version>
	    </extension>
	</extensions>

### Maven >= 3.1.x

Get [maven-profiler](http://dl.bintray.com/jcgay/maven/fr/jcgay/maven/maven-profiler/2.3/maven-profiler-2.3-shaded.jar) and copy it in `%M2_HOME%/lib/ext` folder.

### Maven 3.0.x
(with limited functionality, kept for compatibility)
Get [maven-profiler](http://dl.bintray.com/jcgay/maven/com/github/jcgay/maven/maven-profiler/1.0/maven-profiler-1.0.jar) and copy it in `%M2_HOME%/lib/ext` folder.

##Usage

Use property `profile` when running Maven.

	mvn install -Dprofile

This will generate a report in `.profiler` folder.

One can choose between `HTML` (by default) or `JSON` report using property `profileFormat`.

Also you can add the property `disableTimeSorting` if you want the reported times to be in the order of execution
instead of sorted by execution time.

    mvn install -Dprofile -DdisableTimeSorting


### HTML

	mvn install -Dprofile

[![maven-profiler](http://jeanchristophegay.com/images/maven-profiler-resize.png)](http://jeanchristophegay.com/images/maven-profiler.png)

### JSON

	mvn install -Dprofile -DprofileFormat=JSON

```
{
	"name": "maven-profiler",
	"goals": "clean install",
	"date": "2015/01/17 15:28:49",
	"parameters": "{profile=true, profileFormat=JSON}",
	"projects": [{
		"project": "maven-profiler",
		"time": "6.793 s",
		"mojos": [{
			"mojo": "org.apache.maven.plugins:maven-surefire-plugin:2.18:test {execution: default-test}",
			"time": "2512 ms"
		}, {
			"mojo": "org.apache.maven.plugins:maven-shade-plugin:2.3:shade {execution: default}",
			"time": "1458 ms"
		}, {
			"mojo": "org.codehaus.gmavenplus:gmavenplus-plugin:1.2:testCompile {execution: default}",
			"time": "818.3 ms"
		}, {
			"mojo": "org.apache.maven.plugins:maven-compiler-plugin:3.2:compile {execution: default-compile}",
			"time": "538.7 ms"
		}, {
			"mojo": "org.apache.maven.plugins:maven-source-plugin:2.4:jar-no-fork {execution: attach-sources}",
			"time": "248.5 ms"
		}, {
			"mojo": "org.apache.maven.plugins:maven-jar-plugin:2.5:jar {execution: default-jar}",
			"time": "238.2 ms"
		}, {
			"mojo": "org.apache.maven.plugins:maven-resources-plugin:2.7:resources {execution: default-resources}",
			"time": "171.7 ms"
		}, {
			"mojo": "org.codehaus.plexus:plexus-component-metadata:1.6:generate-metadata {execution: default}",
			"time": "170.5 ms"
		}, {
			"mojo": "org.apache.maven.plugins:maven-enforcer-plugin:1.2:enforce {execution: enforce-maven}",
			"time": "126.0 ms"
		}, {
			"mojo": "org.apache.maven.plugins:maven-clean-plugin:2.6.1:clean {execution: default-clean}",
			"time": "76.11 ms"
		}, {
			"mojo": "org.apache.maven.plugins:maven-install-plugin:2.5.2:install {execution: default-install}",
			"time": "53.95 ms"
		}, {
			"mojo": "org.apache.maven.plugins:maven-compiler-plugin:3.2:testCompile {execution: default-testCompile}",
			"time": "2.850 ms"
		}, {
			"mojo": "org.apache.maven.plugins:maven-resources-plugin:2.7:testResources {execution: default-testResources}",
			"time": "2.518 ms"
		}]
	}]
}
```

## Build status

[![Build Status](https://travis-ci.org/jcgay/maven-profiler.png)](https://travis-ci.org/jcgay/maven-profiler)
[![Coverage Status](https://coveralls.io/repos/jcgay/maven-profiler/badge.svg?branch=master)](https://coveralls.io/r/jcgay/maven-profiler?branch=master)

## Release

    mvn -B release:prepare release:perform
