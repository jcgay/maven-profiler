#!/usr/bin/env groovy
String buildLog = new File(basedir, 'build.log').text
assert buildLog.contains('[INFO] Profiling mvn execution...')
assert buildLog.contains("[INFO] HTML profiling report has been saved in: $basedir/.profiler/profiler-report-")

List<File> report = new File(basedir, '.profiler').listFiles().toList()
assert !report.isEmpty()
assert report[0].name.endsWith('.html')
