/*
 * Copyright 2015-2017 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hawkular.agent.test;

import java.io.File;
import java.util.Collection;

import org.hawkular.cmdgw.ws.test.TestWebSocketClient;
import org.hawkular.cmdgw.ws.test.TestWebSocketClient.MessageAnswer;
import org.hawkular.dmrclient.Address;
import org.hawkular.inventory.api.model.Resource;
import org.hawkular.javaagent.itest.util.AbstractITest;
import org.hawkular.javaagent.itest.util.WildFlyClientConfig;
import org.jboss.as.controller.client.ModelControllerClient;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = AbstractDomainITestSuite.SUITE)
public class DomainDeployApplicationITest extends AbstractITest {
    public static final String GROUP = "DomainDeployApplicationITest";

    @Test(groups = { GROUP }, dependsOnGroups = { DomainResourcesITest.GROUP })
    public void testAddDeployment() throws Throwable {
        waitForHawkularServerToBeReady();

        Collection<Resource> hostControllers = testHelper.getResourceByType(hawkularFeedId, "Host Controller", 1);
        Assert.assertEquals(1, hostControllers.size());
        Resource hostController = hostControllers.iterator().next();

        File applicationFile = getTestApplicationFile();
        final String deploymentName = applicationFile.getName();

        String req = "DeployApplicationRequest={\"authentication\":" + authentication + ", "
                + "\"feedId\":\"" + hostController.getFeedId() + "\","
                + "\"resourceId\":\"" + hostController.getId() + "\","
                + "\"destinationFileName\":\"" + deploymentName + "\","
                + "\"serverGroups\":\"main-server-group,other-server-group\""
                + "}";
        String response = "DeployApplicationResponse={"
                + "\"destinationFileName\":\"" + deploymentName + "\","
                + "\"feedId\":\"" + hostController.getFeedId() + "\","
                + "\"resourceId\":\"" + hostController.getId() + "\","
                + "\"destinationSessionId\":\"{{sessionId}}\","
                + "\"status\":\"OK\","
                + "\"message\":\"Performed [Deploy] on a [Application] given by Feed Id ["
                + hostController.getFeedId() + "] Resource Id [" + hostController.getId() + "]\""
                + "}";
        try (TestWebSocketClient testClient = TestWebSocketClient.builder()
                .url(baseGwUri + "/ui/ws")
                .expectWelcome(new MessageAnswer(req, applicationFile.toURI().toURL(), 0))
                .expectGenericSuccess(hostController.getFeedId())
                .expectText(response, TestWebSocketClient.Answer.CLOSE)
                .expectClose()
                .build()) {
            testClient.validate(30000);
        }

        // check that the app was really deployed on the two server groups
        WildFlyClientConfig clientConfig = getPlainWildFlyClientConfig();
        try (ModelControllerClient mcc = newPlainWildFlyModelControllerClient(clientConfig)) {
            assertNodeAttributeEquals(mcc,
                    Address.parse(
                            "/server-group=main-server-group/deployment=hawkular-javaagent-helloworld-war.war")
                            .getAddressNode(),
                    "enabled",
                    "true");
            assertNodeAttributeEquals(mcc,
                    Address.parse(
                            "/server-group=other-server-group/deployment=hawkular-javaagent-helloworld-war.war")
                            .getAddressNode(),
                    "enabled",
                    "true");
        }
    }

    @Test(groups = { GROUP }, dependsOnMethods = { "testAddDeployment" })
    public void testDisableDeployment() throws Throwable {
        // check that the app is enabled on the two server groups
        WildFlyClientConfig clientConfig = getPlainWildFlyClientConfig();
        try (ModelControllerClient mcc = newPlainWildFlyModelControllerClient(clientConfig)) {
            assertNodeAttributeEquals(mcc,
                    Address.parse(
                            "/server-group=main-server-group/deployment=hawkular-javaagent-helloworld-war.war")
                            .getAddressNode(),
                    "enabled",
                    "true");
            assertNodeAttributeEquals(mcc,
                    Address.parse(
                            "/server-group=other-server-group/deployment=hawkular-javaagent-helloworld-war.war")
                            .getAddressNode(),
                    "enabled",
                    "true");
        }

        Collection<Resource> hostControllers = testHelper.getResourceByType(hawkularFeedId, "Host Controller", 1);
        Assert.assertEquals(1, hostControllers.size());
        Resource hostController = hostControllers.iterator().next();

        File applicationFile = getTestApplicationFile();
        final String deploymentName = applicationFile.getName();

        String req = "DisableApplicationRequest={\"authentication\":" + authentication + ", "
                + "\"feedId\":\"" + hostController.getFeedId() + "\","
                + "\"resourceId\":\"" + hostController.getId() + "\","
                + "\"destinationFileName\":\"" + deploymentName + "\","
                + "\"serverGroups\":\"main-server-group,other-server-group\""
                + "}";
        String response = "DisableApplicationResponse={"
                + "\"destinationFileName\":\"" + deploymentName + "\","
                + "\"feedId\":\"" + hostController.getFeedId() + "\","
                + "\"resourceId\":\"" + hostController.getId() + "\","
                + "\"destinationSessionId\":\"{{sessionId}}\","
                + "\"status\":\"OK\","
                + "\"message\":\"Performed [Disable Deployment] on a [Application] given by Feed Id ["
                + hostController.getFeedId() + "] Resource Id [" + hostController.getId() + "]\""
                + "}";
        try (TestWebSocketClient testClient = TestWebSocketClient.builder()
                .url(baseGwUri + "/ui/ws")
                .expectWelcome(new MessageAnswer(req, applicationFile.toURI().toURL(), 0))
                .expectGenericSuccess(hostController.getFeedId())
                .expectText(response, TestWebSocketClient.Answer.CLOSE)
                .expectClose()
                .build()) {
            testClient.validate(30000);
        }

        // check that the app is disabled on the two server groups
        try (ModelControllerClient mcc = newPlainWildFlyModelControllerClient(clientConfig)) {
            assertNodeAttributeEquals(mcc,
                    Address.parse(
                            "/server-group=main-server-group/deployment=hawkular-javaagent-helloworld-war.war")
                            .getAddressNode(),
                    "enabled",
                    "false");
            assertNodeAttributeEquals(mcc,
                    Address.parse(
                            "/server-group=other-server-group/deployment=hawkular-javaagent-helloworld-war.war")
                            .getAddressNode(),
                    "enabled",
                    "false");
        }
    }

    @Test(groups = { GROUP }, dependsOnMethods = { "testDisableDeployment" })
    public void testEnableDeployment() throws Throwable {
        // check that the app is disabled on the two server groups
        WildFlyClientConfig clientConfig = getPlainWildFlyClientConfig();
        try (ModelControllerClient mcc = newPlainWildFlyModelControllerClient(clientConfig)) {
            assertNodeAttributeEquals(mcc,
                    Address.parse(
                            "/server-group=main-server-group/deployment=hawkular-javaagent-helloworld-war.war")
                            .getAddressNode(),
                    "enabled",
                    "false");
            assertNodeAttributeEquals(mcc,
                    Address.parse(
                            "/server-group=other-server-group/deployment=hawkular-javaagent-helloworld-war.war")
                            .getAddressNode(),
                    "enabled",
                    "false");
        }

        Collection<Resource> hostControllers = testHelper.getResourceByType(hawkularFeedId, "Host Controller", 1);
        Assert.assertEquals(1, hostControllers.size());
        Resource hostController = hostControllers.iterator().next();

        File applicationFile = getTestApplicationFile();
        final String deploymentName = applicationFile.getName();

        String req = "EnableApplicationRequest={\"authentication\":" + authentication + ", "
                + "\"feedId\":\"" + hostController.getFeedId() + "\","
                + "\"resourceId\":\"" + hostController.getId() + "\","
                + "\"destinationFileName\":\"" + deploymentName + "\","
                + "\"serverGroups\":\"main-server-group,other-server-group\""
                + "}";
        String response = "EnableApplicationResponse={"
                + "\"destinationFileName\":\"" + deploymentName + "\","
                + "\"feedId\":\"" + hostController.getFeedId() + "\","
                + "\"resourceId\":\"" + hostController.getId() + "\","
                + "\"destinationSessionId\":\"{{sessionId}}\","
                + "\"status\":\"OK\","
                + "\"message\":\"Performed [Enable Deployment] on a [Application] given by Feed Id ["
                + hostController.getFeedId() + "] Resource Id [" + hostController.getId() + "]\""
                + "}";
        try (TestWebSocketClient testClient = TestWebSocketClient.builder()
                .url(baseGwUri + "/ui/ws")
                .expectWelcome(new MessageAnswer(req, applicationFile.toURI().toURL(), 0))
                .expectGenericSuccess(hostController.getFeedId())
                .expectText(response, TestWebSocketClient.Answer.CLOSE)
                .expectClose()
                .build()) {
            testClient.validate(30000);
        }

        // check that the app is enabled on the two server groups
        try (ModelControllerClient mcc = newPlainWildFlyModelControllerClient(clientConfig)) {
            assertNodeAttributeEquals(mcc,
                    Address.parse(
                            "/server-group=main-server-group/deployment=hawkular-javaagent-helloworld-war.war")
                            .getAddressNode(),
                    "enabled",
                    "true");
            assertNodeAttributeEquals(mcc,
                    Address.parse(
                            "/server-group=other-server-group/deployment=hawkular-javaagent-helloworld-war.war")
                            .getAddressNode(),
                    "enabled",
                    "true");
        }
    }

    @Test(groups = { GROUP }, dependsOnMethods = { "testEnableDeployment" })
    public void testRestartDeployment() throws Throwable {
        // check that the app is enabled on the two server groups
        WildFlyClientConfig clientConfig = getPlainWildFlyClientConfig();
        try (ModelControllerClient mcc = newPlainWildFlyModelControllerClient(clientConfig)) {
            assertNodeAttributeEquals(mcc,
                    Address.parse(
                            "/server-group=main-server-group/deployment=hawkular-javaagent-helloworld-war.war")
                            .getAddressNode(),
                    "enabled",
                    "true");
            assertNodeAttributeEquals(mcc,
                    Address.parse(
                            "/server-group=other-server-group/deployment=hawkular-javaagent-helloworld-war.war")
                            .getAddressNode(),
                    "enabled",
                    "true");
        }

        Collection<Resource> hostControllers = testHelper.getResourceByType(hawkularFeedId, "Host Controller", 1);
        Assert.assertEquals(1, hostControllers.size());
        Resource hostController = hostControllers.iterator().next();

        File applicationFile = getTestApplicationFile();
        final String deploymentName = applicationFile.getName();

        String req = "RestartApplicationRequest={\"authentication\":" + authentication + ", "
                + "\"feedId\":\"" + hostController.getFeedId() + "\","
                + "\"resourceId\":\"" + hostController.getId() + "\","
                + "\"destinationFileName\":\"" + deploymentName + "\","
                + "\"serverGroups\":\"main-server-group,other-server-group\""
                + "}";
        String response = "RestartApplicationResponse={"
                + "\"destinationFileName\":\"" + deploymentName + "\","
                + "\"feedId\":\"" + hostController.getFeedId() + "\","
                + "\"resourceId\":\"" + hostController.getId() + "\","
                + "\"destinationSessionId\":\"{{sessionId}}\","
                + "\"status\":\"OK\","
                + "\"message\":\"Performed [Restart Deployment] on a [Application] given by Feed Id ["
                + hostController.getFeedId() + "] Resource Id [" + hostController.getId() + "]\""
                + "}";
        try (TestWebSocketClient testClient = TestWebSocketClient.builder()
                .url(baseGwUri + "/ui/ws")
                .expectWelcome(new MessageAnswer(req, applicationFile.toURI().toURL(), 0))
                .expectGenericSuccess(hostController.getFeedId())
                .expectText(response, TestWebSocketClient.Answer.CLOSE)
                .expectClose()
                .build()) {
            testClient.validate(30000);
        }

        // check that the app is again enabled on the two server groups
        try (ModelControllerClient mcc = newPlainWildFlyModelControllerClient(clientConfig)) {
            assertNodeAttributeEquals(mcc,
                    Address.parse(
                            "/server-group=main-server-group/deployment=hawkular-javaagent-helloworld-war.war")
                            .getAddressNode(),
                    "enabled",
                    "true");
            assertNodeAttributeEquals(mcc,
                    Address.parse(
                            "/server-group=other-server-group/deployment=hawkular-javaagent-helloworld-war.war")
                            .getAddressNode(),
                    "enabled",
                    "true");
        }
    }

    // this does a full undeploy via the host controller - removing from all server groups
    @Test(groups = { GROUP }, dependsOnMethods = { "testRestartDeployment" })
    public void testUndeploy() throws Throwable {
        Collection<Resource> hostControllers = testHelper.getResourceByType(hawkularFeedId, "Host Controller", 1);
        Assert.assertEquals(1, hostControllers.size());
        Resource hostController = hostControllers.iterator().next();

        File applicationFile = getTestApplicationFile();
        final String deploymentName = applicationFile.getName();

        String req = "UndeployApplicationRequest={\"authentication\":" + authentication + ", "
                + "\"feedId\":\"" + hostController.getFeedId() + "\","
                + "\"resourceId\":\"" + hostController.getId() + "\","
                + "\"destinationFileName\":\"" + deploymentName + "\","
                + "\"serverGroups\":\"main-server-group,other-server-group\""
                + "}";
        String response = "UndeployApplicationResponse={"
                + "\"destinationFileName\":\"" + deploymentName + "\","
                + "\"feedId\":\"" + hostController.getFeedId() + "\","
                + "\"resourceId\":\"" + hostController.getId() + "\","
                + "\"destinationSessionId\":\"{{sessionId}}\","
                + "\"status\":\"OK\","
                + "\"message\":\"Performed [Undeploy] on a [Application] given by Feed Id ["
                + hostController.getFeedId() + "] Resource Id [" + hostController.getId() + "]\""
                + "}";
        try (TestWebSocketClient testClient = TestWebSocketClient.builder()
                .url(baseGwUri + "/ui/ws")
                .expectWelcome(new MessageAnswer(req, applicationFile.toURI().toURL(), 0))
                .expectGenericSuccess(hostController.getFeedId())
                .expectText(response, TestWebSocketClient.Answer.CLOSE)
                .expectClose()
                .build()) {
            testClient.validate(30000);
        }

        // check that the app was really undeployed on the two server groups
        WildFlyClientConfig clientConfig = getPlainWildFlyClientConfig();
        try (ModelControllerClient mcc = newPlainWildFlyModelControllerClient(clientConfig)) {
            assertResourceExists(mcc,
                    Address.parse(
                            "/server-group=main-server-group/deployment=hawkular-javaagent-helloworld-war.war/")
                            .getAddressNode(),
                    false);
            assertResourceExists(mcc,
                    Address.parse(
                            "/server-group=other-server-group/deployment=hawkular-javaagent-helloworld-war.war/")
                            .getAddressNode(),
                    false);
        }
    }
}
