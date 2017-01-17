#!/usr/bin/env groovy

File buildLog = new File(basedir, 'build.log')

assert buildLog.exists()
assert buildLog.text.contains('[INFO] Profiling mvn execution...')
assert buildLog.text.contains("[INFO] HTML profiling report has been saved in: $basedir/.profiler/profiler-report-")
