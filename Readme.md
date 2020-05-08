# Cucumber Utils tutorial
Here is a small tutorial on how to use [**cucumber-utils**](https://github.com/fslev/cucumber-utils) library inside a test framework.  
**Test target**: HTTP REST APIs, hosted by [reqres](https://reqres.in/).   

## Summary
[*Reqres*](https://reqres.in/) provides a series of HTTPs REST APIs that accept fake test data and returns a limited set of static responses.  
This tutorial describes how to setup a basic test-framework which executes Cucumber acceptance/integration tests that call these APIs and compares actual responses with expected data.    

You will see some tips and tricks on how to use [**cucumber-utils**](https://github.com/fslev/cucumber-utils) library, as well on how to use **Cucumber** native *parallelization* feature. This will ease your work as a test engineer / developer.  
This library contains many features such as:
 - easy to use HTTP clients  
 - database clients
 - Mechanisms for comparing HTTP responses, JSONs, XMLs and strings using REGEX patterns.    
 - predefined Cucumber steps for:
   - instantiating Scenario properties (sharing state between steps within a Scenario)  
   - defining and comparing Dates
   - querying and updating databases and compare results
   - loading Scenario properties directly from external resources  
 - etc.  
More details you will find on the main Github [**cucumber-utils**](https://github.com/fslev/cucumber-utils) page.  

_Finally_, you will learn how to generate test reports with [**maven-cucumber-reporting**](https://github.com/damianszczepanik/maven-cucumber-reporting) plugin.  

* [Test cases](#test-cases)
* [Run tests from Intellij Idea](#run-idea)
* [Run tests from Maven serially or in parallel](#run-maven)
* [Test Reports](#test-reports)

<a name="test-cases"></a>

<a name="run-idea"></a>
## Configure Intellij Idea to run Cucumber feature files 
### Requirements
- __Intellij Idea__ version >= 2019.3
- Latest version of __Cucumber for Java__ and __Gherkin__ plugins

### Cucumber for Java Plugin Configuration
Setup _Glue_ packages and _Program arguments_:
- **Run -> Edit Configurations**:  
  - Clean any "Cucumber java" configuration instances that ran in the past
  - Inside **Templates -> Cucumber java**, setup the followings:
    - **Glue**: _com.cucumber.utils com.cucumber.tutorial_
    - **Program arguments**: _--plugin junit:output_
    - Optional: for parallelization add "--threads 5" at the beginning inside **Program arguments**
    - Rest of the fields, leave them as they are


## Test cases
### Test Login API 
```
POST: /api/login
Body:
{
    "email": "eve.holt@reqres.in",
    "password": "cityslicka"
}
```  

1. Define Cucumber Java step definitions:
```java
@ScenarioScoped
public class LoginSteps extends RestScenario {
    private LoginService loginService = new LoginService();

    @Inject
    public LoginSteps(Cucumbers cucumbers) {
        cucumbers.loadScenarioPropsFromFile("templates/login/login.yaml");
    }

    @Then("Login with requestBody={} and check response={}")
    public void login(String requestBody, String expected) {
        executeAndCompare(loginService.prepare(scenarioProps.getAsString("reqresin.address"), requestBody), expected);
    }

    @Then("Login with email={}, password={} and check response={}")
    public void login(String email, String password, String expected) {
        executeAndCompare(loginService.prepare(scenarioProps.getAsString("reqresin.address"), email, password), expected);
    }

    @Then("Login with email={}, password={} and extract token")
    public void loginAndExtractToken(String email, String password) {
        executeAndCompare(loginService.prepare(
                scenarioProps.getAsString("reqresin.address"), email, password), scenarioProps.getAsString("loginResponseTemplate"));
    }
}
```
, where _login.yaml_ file looks like:
```
loginRequestTemplate: |
  {
      "email": "#[email]",
      "password": "#[password]"
  }
loginResponseTemplate: |
  {   "status": 200,
      "body": {
          "token": "~[token]"
      }
  }
```

As you can see, we also defined a step which doesn't use the loginRequestTemplate:  
```
    @Then("Login with requestBody={} and check response={}")
    public void login(String requestBody, String expected) {
        executeAndCompare(loginService.prepare(scenarioProps.getAsString("reqresin.address"), requestBody), expected);
    }
```
That way, we can use any type of JSON request body or any String value for that matter, directly from the Gherkin Scenario.  
This has several advantages, such as when we have to deal with big JSON requests, which are hard to represent as a template in a separate file, or when we have to deal with negative test cases, such as: Call the API with an invalid JSON.  
Define these kind of steps how you think they are suitable for your scenarios.      
  
2. Define the test Gherkin scenarios:  
```gherkin
@all @login
Feature: Test Login feature

  Scenario Template: Call login API with invalid data <request> and check for correct error message
    Then Login with requestBody=<request> and check response=<response>
    Examples:
      | request                                        | response                                                        |
      | { "email": "peter@klaven" }                    | {"status": 400, "body": {"error": "Missing password"}}          |
      | { "email": "peter", "password": "cityslicka" } | {"status": 400, "body": {"error": "user not found"}}            |
      | { "password": "12345" }                        | {"status": 400, "body": {"error": "Missing email or username"}} |
      | []                                             | {"status": 400, "body": {"error": "Missing email or username"}} |


  Scenario Template: Call login API with valid username <email> and password and check for correct response
    Then Login with email=<email>, password=<password> and check response=<response>
    Examples:
      | email              | password   | response        |
      | eve.holt@reqres.in | cityslicka | {"status": 200} |
```

### Test Create User API

```
POST: /api/create
Body:
{
    "name": "morpheus",
    "job": "leader"
}
```  
1. Define Cucumber Java step definitions:
```java
@ScenarioScoped
public class CreateUserSteps extends RestScenario {
    private UserService userService = new UserService();

    @Inject
    public CreateUserSteps(Cucumbers cucumbers) {
        cucumbers.loadScenarioPropsFromFile("templates/users/create.yaml");
    }

    @Then("Create user with name={}, job={} and check response={}")
    public void createUserAndCompare(String name, String job, String expected) {
        executeAndCompare(userService.prepareCreate(
                scenarioProps.getAsString("reqresin.address"), name, job, scenarioProps.getAsString("token")), expected);
    }

    @Then("Create user with name={}, job={} and check response!={}")
    public void createUserAndCompareNegative(String name, String job, String expected) {
        executeAndNegativeCompare(userService.prepareCreate(
                scenarioProps.getAsString("reqresin.address"), name, job, scenarioProps.getAsString("token")), expected);
    }

    @Then("Create user with request={} and check response={}")
    public void createUserAndCompare(String request, String expected) {
        executeAndCompare(userService.prepareCreate(
                scenarioProps.getAsString("reqresin.address"), request, scenarioProps.getAsString("token")), expected);
    }
}
```
, where _create.yaml_ file looks like:
```
createUserRequestTemplate: |
  {
      "name": "#[name]",
      "job": "#[job]"
  }
```
2. Define the Cucumber test scenarios:
```gherkin
@all @create
Feature: Create User feature

  Scenario Template: Create user with valid data and check for correct response
    Given param expectedCreateUserResponse=
    """
    {
      "status": 201,
      "body": {
         "name": "<name>",
         "job": "<job>",
         "id": "[0-9]*",
         "createdAt": ".*"
      }
    }
    """
    # login and compare response (if comparison passes, token is automatically set inside scenario properties)
    When Login with email=eve.holt@reqres.in, password=cityslicka and extract token
    # token is set as "authorization" header for Create user API
    Then Create user with name=<name>, job=<job> and check response=#[expectedCreateUserResponse]
    Examples:
      | name   | job     |
      | florin | tester  |
      | john   | blogger |

  Scenario: Create user with valid data and check for correct response from file
  Same scenario as above, but define 'expectedCreateUserResponse' scenario property inside file
    * load all scenario props from dir "create"
    When Login with email=eve.holt@reqres.in, password=cityslicka and extract token
    Then Create user with name=David Jones, job=pirate and check response=#[expectedCreateUserResponse]
```
 
You can see that we used a pre-defined step from Cucumber-Utils:
```gherkin
* load all scenario props from dir "create"
```  
By loading values from separate files or directories, we do not burden the Gherkin scenario with bulky Strings representing our expected values. We do this with scenario properties.   
Behind the scenes, Cucumber-Utils sets new scenario properties, each one having as property name the file name, and as property value the file content.  

Taking the example from above, '#[expectedCreateUserResponse]' represents a scenario property, which has the name of a file (without extension) from 'create' directory and its value is actually the content of the file.     
Cucumber-Utils has a special mechanism for parsing these variables '#[]' present inside the Gherkin steps. It replaces these variables with their values, before passing them to the parameters from the corresponding Java step definition methods.  


## Comparing
In current tutorial project, we compare JSONs.  
Behind the scenes, Cucumber-Utils does this by using [**json-compare**](https://github.com/fslev/json-compare)

## General best practices for writing Cucumber scenarios
- Defined steps should be simple and reusable. Otherwise, you will end up writing both Java code and Gherkin syntax for each scenario  
- One step should do two things: call an API and compare response  
- Log scenario steps (Ex: log API call details; log compared values)  
- Use comprehensive helper methods as much as possible (Ex: a single method which calls an API, compares the response and also logs the whole thing)   

<a name="run-maven"></a>
## Run Cucumber tests with Maven in serial or parallel mode
_Maven command_:  
mvn clean verify -P{environment} (optional -Dtags=@foo -Dconcurrent=true)
```
mvn clean verify -Pprod -Dtags=@all -Dconcurrent=true
```   

<a name="test-reports"></a>
## Cucumber Test Report
The report is generated in HTML format inside target/cucumber-html-reports:  

![Features overview](https://github.com/fslev/cucumber-utils-tutorial/blob/master/reports/1a.png)
![Scenario overview](https://github.com/fslev/cucumber-utils-tutorial/blob/master/reports/1b.png)