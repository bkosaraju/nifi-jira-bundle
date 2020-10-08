/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.bkosaraju.service.jiranotification;

import org.apache.nifi.reporting.InitializationException;
import org.apache.nifi.util.TestRunner;
import org.apache.nifi.util.TestRunners;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;


public class TestJiraCredentialService {

    @Before
    public void init() {

    }

    @Test
    public void testService() throws InitializationException {
        final TestRunner runner = TestRunners.newTestRunner(TestProcessor.class);
        final JiraCredentialService service = new JiraCredentialService();

        runner.addControllerService("test-good", service);
        runner.setProperty(service, JiraCredentialService.PROP_JIRA_URL, "test-URL");
        runner.setProperty(service, JiraCredentialService.PROP_JIRA_USER, "test-USER");
        runner.setProperty(service, JiraCredentialService.PROP_JIRA_PASSWORD, "test-PASSWORD");
        runner.setProperty(service, JiraCredentialService.PROP_JIRA_PROJECT, "test-PROJECT");
        runner.enableControllerService(service);
        runner.assertValid(service);
        assertEquals("test-USER",service.getConnectionProperties().getProperty("jiraUser"));
    }


    @Test
    public void testNonValidService() throws InitializationException {
        final TestRunner runner = TestRunners.newTestRunner(TestProcessor.class);
        final JiraCredentialService service = new JiraCredentialService();
        runner.addControllerService("test-fail", service);
        runner.setProperty(service, JiraCredentialService.PROP_JIRA_URL, "test-URL");
        runner.setProperty(service, JiraCredentialService.PROP_JIRA_URL, "test-URL");
        try {
            runner.enableControllerService(service);
        } catch (Exception e) {
            runner.getLogger().info("Anticipated failure..");
        }
        runner.assertNotValid(service);
    }

}
