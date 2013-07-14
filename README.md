#Maven Profiler

A time execution recorder for Maven which log time taken by each mojo in your build lifecycle.

##Installation

Get [maven-profiler](https://repository-jcgay.forge.cloudbees.com/snapshot/com/github/jcgay/maven/maven-profiler/0.1-SNAPSHOT/) and copy it in `%M2_HOME%/lib/ext` folder.

##Usage

Use property `profile` when running Maven.

	mvn install -Dprofile
	
This will print detailed execution time at the end of the build.

```
[INFO] EXECUTION TIME
[INFO] ------------------------------------------------------------------------
[INFO] maven-color-agent: 8.717 s
[INFO] org.apache.maven.plugins:maven-surefire-plugin:2.14:test {execution: default-test}                   2.702 s
[INFO] org.apache.maven.plugins:maven-compiler-plugin:2.5.1:compile {execution: default-compile}            2.565 s
[INFO] org.apache.maven.plugins:maven-jar-plugin:2.4:jar {execution: default-jar}                           1.009 s
[INFO] org.apache.maven.plugins:maven-resources-plugin:2.5:resources {execution: default-resources}         887.4 ms
[INFO] org.apache.maven.plugins:maven-shade-plugin:1.7.1:shade {execution: default}                         609.9 ms
[INFO] org.apache.maven.plugins:maven-compiler-plugin:2.5.1:testCompile {execution: default-testCompile}    310.1 ms
[INFO] org.apache.maven.plugins:maven-clean-plugin:2.4.1:clean {execution: default-clean}                   161.3 ms
[INFO] org.apache.maven.plugins:maven-install-plugin:2.3.1:install {execution: default-install}             69.06 ms
[INFO] org.apache.maven.plugins:maven-resources-plugin:2.5:testResources {execution: default-testResources} 6.592 ms
[INFO] maven-color-logger: 5.369 s
[INFO] org.apache.maven.plugins:maven-surefire-plugin:2.14:test {execution: default-test}                   1.337 s
[INFO] org.apache.maven.plugins:maven-compiler-plugin:2.5.1:compile {execution: default-compile}            975.4 ms
[INFO] org.codehaus.plexus:plexus-component-metadata:1.5.5:generate-metadata {execution: default}           821.1 ms
[INFO] org.apache.maven.plugins:maven-compiler-plugin:2.5.1:testCompile {execution: default-testCompile}    767.5 ms
[INFO] org.apache.maven.plugins:maven-jar-plugin:2.4:jar {execution: default-jar}                           302.6 ms
[INFO] org.apache.maven.plugins:maven-clean-plugin:2.4.1:clean {execution: default-clean}                   148.2 ms
[INFO] org.apache.maven.plugins:maven-shade-plugin:1.7.1:shade {execution: default}                         136.2 ms
[INFO] org.apache.maven.plugins:maven-install-plugin:2.3.1:install {execution: default-install}             54.80 ms
[INFO] org.apache.maven.plugins:maven-resources-plugin:2.5:testResources {execution: default-testResources} 5.077 ms
[INFO] org.apache.maven.plugins:maven-resources-plugin:2.5:resources {execution: default-resources}         3.676 ms
[INFO] maven-color: 804.7 ms
[INFO] org.apache.maven.plugins:maven-install-plugin:2.3.1:install {execution: default-install}             379.0 ms
[INFO] org.apache.maven.plugins:maven-clean-plugin:2.4.1:clean {execution: default-clean}                   272.5 ms
```