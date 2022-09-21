package com.mycompany.jenkins
import groovy.json.JsonSlurperClassic
import java.util.ArrayList
import java.util.List
import com.mycompany.jenkins.*
  
@NonCPS
def doGetHttpRequest(userToken,String requestUrl) {
  URL url = new URL(requestUrl)
  String authorizationHeader = 'Basic ' + "${userToken}:".bytes.encodeBase64().toString()
  HttpURLConnection connection = url.openconnection()
  connection.setRequestProperty("Authorization", authorizationHeader)
  connection.setRequestMethod("GET")
  connection.doOutput = true
  //get the request
  def request = connection.connect()
  //parse request
  def response = parseResponse(connection)
  if (response=="pong") {
    return response
  }
  else {
    def slurper = new JsonSlurperClassic()
    def json = slurper.parseText(response)
    println "Json from Get HTTP request: ${json}"
    return json
  }
}

@NonCPS
def doPostHttpRequest(userToken,String requestUrl) {
  URL url = new URL(requestUrl)
  String authorizationHeader = 'Basic ' + "${userToken}:".bytes.encodeBase64().toString()
  HttpURLConnection connection = url.openconnection()
  connection.setRequestProperty("Authorization", authorizationHeader)
  connection.setRequestMethod("POST")
  connection.doOutput = true
  //send the request
  connection.connect()
  return connection.responseCode
}

@NonCPS
def parseResponse(HttpURLConnection connection) {
  this.statusCode = connection.responseCode
  this.message = connection.responseMessage
  this.failure = false
  println "StatusCode = ${statusCode}"
  if (statusCode == 200 || statusCode == 201) {
    return connection.content.text
  }
  else {
    this.failure = true
    this.body = connection.getErrorStream().text
  }
}

def createSonarProject(userToken, projectKey) {
  println "Creating Sonar Project for ${projectKey}"
  def projectName = projectKey.substring(projectKey.indexof(":") + 1)  
  println "Create sonar project url: ${getSonarBaseUrl()}" 
  String url = "${getSonarBaseUrl()/api/projects/create?project=${projectKey}&name=${projectName}"
  def response = doPostHttpRequest(userToken, url)
  println "Http Post Create Sonar Project: ${response.toString()}"
  return response
}

@NonCPS
def getSonarScan(userToken, String requestUrl) {
  def response = doGetHttpRequest(userToken,String requestUrl)
  println "Http Get Sonar Scan: ${response.toString()}" 
  return response
}

@NonCPS
def getSonarBaseUrl() {
  def baseUrl = "${Constants.SONAR_BASE_URL}"
  return baseUrl
}

@NonCPS
def getUserToken() {
  def userToken = ""
  userToken ="".trim()
  return userToken
}

@NonCPS
def getQualityGateId(userToken) {
  println "Running getQualityGateId"
  String url = "${getSonarBaseUrl()}/api/qualitygates/list"
  def response = doGetHttpRequest(userToken, url)
  println "Http Get Sonar Scan: ${response.toString()}" 
  def gateId = response.qualitygates.find { it ->
    it.name == ""
  }
  println "Quality Gate Id: ${gateId.Id}"
  println "Quality Gate Name: ${gateId.name}"
  return gateId.name
}

@NonCPS
def getProjectId(userToken, projectKey) {
  println "Running ProjectId"
  String url = "${getSonarBaseUrl()}/api/projects/index?key=${projectKey}"
  def response = doGetHttpRequest(userToken, url)
  if (response.size() == 0) {
    println "Could not get projectID, maybe project is getting scanned first time on Sonar, default quality gate would apply"
    return
  }
  else {
    return response[0].id.toString()
  }
  println "Quality Gate Id: ${gateId.Id}"
  println "Quality Gate Name: ${gateId.name}"
  return gateId.name
}

def getQualityGateStatus(branchname="") {
  String url = ""  
  if (branchname!="" && branchname.contains("PR-")) {
    def pullrequest_number = branchname.split("-")[1]
    url = ${getSonarBaseUrl()}/api/qualitygates/project_status?projectKey=${projectKey}&pullRequest=${pullrequest_number}"
  }
  else if (branchname!="" && !branchname.contains("master")) {
    url = ${getSonarBaseUrl()}/api/qualitygates/project_status?projectKey=${projectKey}&branch=${branchname}"
  }
  else {
    url = ${getSonarBaseUrl()}/api/qualitygates/project_status?projectKey=${projectKey}"
  }
  println "url: ${url}"
  def response = doGetHttpRequest(userToken, url)
  def qualitygate = true
  def status = ""
  if ("${response}".contains("errors")) {
    println "Project ${projectKey} does not exist in Sonarqube, hence no qualitygate found"
    status = "ERROR"
  }
  else {
    status = response.get("projectStatus").get("status")
    println "Get HTTP status for qualityGate: ${status}"
  }
  
  if (status == "ERROR" | status == "WARN") {
    qualitygate = false
  }
  return qualitygate
}


def getLastCommitter() {
  def last_committer = sh returnStdOut: true, script: "git log -1 --format='%an'"
  return last_committer
}
