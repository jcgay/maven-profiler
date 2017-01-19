#!/usr/bin/env groovy
import groovy.json.JsonSlurper

List<File> report = new File(basedir, '.profiler').listFiles().toList()
assert !report.isEmpty()
assert report[0].name.endsWith('.json')

def json = new JsonSlurper().parseText(report[0].text)
assert json.name == 'aggregator'
