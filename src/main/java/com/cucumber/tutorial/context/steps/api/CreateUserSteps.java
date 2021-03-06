package com.cucumber.tutorial.context.steps.api;

import com.cucumber.tutorial.context.RestScenario;
import com.cucumber.tutorial.services.http.mock.UserService;
import com.cucumber.utils.engineering.match.condition.MatchCondition;
import com.google.inject.Inject;
import io.cucumber.guice.ScenarioScoped;
import io.cucumber.java.en.Then;

@ScenarioScoped
public class CreateUserSteps extends RestScenario {
    @Inject
    private UserService userService;

    @Then("Create user with name={}, job={} and check response={}")
    public void createUserAndCompare(String name, String job, String expected) {
        executeAndCompare(userService.prepareCreate(
                scenarioProps.getAsString("reqresin.address"), name, job, scenarioProps.getAsString("token")), expected);
    }

    @Then("Create user with name={}, job={} and check response!={}")
    public void createUserAndCompareNegative(String name, String job, String expected) {
        executeAndCompare(userService.prepareCreate(
                scenarioProps.getAsString("reqresin.address"), name, job, scenarioProps.getAsString("token")), expected, MatchCondition.DO_NOT_MATCH_HTTP_RESPONSE_BY_BODY);
    }

    @Then("Create user with request={} and check response={}")
    public void createUserAndCompare(String request, String expected) {
        executeAndCompare(userService.prepareCreate(
                scenarioProps.getAsString("reqresin.address"), request, scenarioProps.getAsString("token")), expected);
    }
}