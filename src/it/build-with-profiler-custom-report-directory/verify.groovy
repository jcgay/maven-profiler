#!/usr/bin/env groovy
String buildLog = new File(basedir, 'build.log').text
assert buildLog.contains('[INFO] Profiling mvn execution...')
assert buildLog.contains("[INFO] HTML profiling report has been saved in: $basedir/target/custom-directory/profiler-report-")

def directory = new File(basedir, 'target/custom-directory')
assert directory.exists()

List<File> report = directory.listFiles().toList()
assert !report.isEmpty()
assert report[0].name.endsWith('.html')
