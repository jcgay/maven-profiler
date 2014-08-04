#Maven Profiler

A time execution recorder for Maven which log time taken by each mojo in your build lifecycle.

##Installation

### Maven >= 3.1.x

Get [maven-profiler](http://dl.bintray.com/jcgay/maven/com/github/jcgay/maven/maven-profiler/2.0/maven-profiler-2.0.jar) and copy it in `%M2_HOME%/lib/ext` folder.

### Maven 3.0.x

Get [maven-profiler](http://dl.bintray.com/jcgay/maven/com/github/jcgay/maven/maven-profiler/1.0/maven-profiler-1.0.jar) and copy it in `%M2_HOME%/lib/ext` folder.

##Usage

Use property `profile` when running Maven.

	mvn install -Dprofile
	
This will generate a report in `.profiler` folder.

[![maven-profiler](http://jeanchristophegay.com/images/maven-profiler-resize.png)](http://jeanchristophegay.com/images/maven-profiler.png)
