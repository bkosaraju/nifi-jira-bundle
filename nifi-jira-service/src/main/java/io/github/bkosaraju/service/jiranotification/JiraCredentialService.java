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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.annotation.lifecycle.OnDisabled;
import org.apache.nifi.annotation.lifecycle.OnEnabled;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.controller.AbstractControllerService;
import org.apache.nifi.controller.ConfigurationContext;
import org.apache.nifi.processor.exception.ProcessException;
import org.apache.nifi.processor.util.StandardValidators;
import org.apache.nifi.reporting.InitializationException;
import org.apache.nifi.controller.ControllerServiceInitializationContext;

@Tags({ "notification","jira","custom"})
@CapabilityDescription("ControllerService implementation of notificationCredentialService.")
public class JiraCredentialService extends AbstractControllerService implements JiraCredentialServiceApi {

    public static final PropertyDescriptor PROP_JIRA_URL = new PropertyDescriptor
            .Builder().name("jiraURL")
            .displayName("Jira URL")
            .description("URL for Jira to post event")
            .required(true)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();

    public static final PropertyDescriptor PROP_JIRA_PROJECT = new PropertyDescriptor
            .Builder().name("jiraProject")
            .displayName("Jira Project")
            .description("Jira Project to post event")
            .required(true)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();

    public static final PropertyDescriptor PROP_JIRA_USER = new PropertyDescriptor
            .Builder().name("jiraUser")
            .displayName("Jira User name")
            .description("username for Jira to post event")
            .required(true)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();

    public static final PropertyDescriptor PROP_JIRA_PASSWORD = new PropertyDescriptor
            .Builder().name("jiraPassword")
            .displayName("Jira User Password / API Token")
            .description("password /  API token for Jira to post event")
            .required(true)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .sensitive(true)
            .build();


    @Override
    protected void init(final ControllerServiceInitializationContext context) throws InitializationException { }

    private static final List<PropertyDescriptor> properties;

    static {
        final List<PropertyDescriptor> props = new ArrayList<>();
        props.add(PROP_JIRA_URL);
        props.add(PROP_JIRA_USER);
        props.add(PROP_JIRA_PASSWORD);
        props.add(PROP_JIRA_PROJECT);
        properties = Collections.unmodifiableList(props);
    }

    @Override
    protected List<PropertyDescriptor> getSupportedPropertyDescriptors() {
        return properties;
    }

    private String getPropertyValue(PropertyDescriptor pd) {
        return getConfigurationContext().getProperty(pd).getValue();
    }
    @Override
    public Properties getConnectionProperties() throws ProcessException {
        Properties prps = new Properties();
        for (PropertyDescriptor pd : properties) {
            try {
                prps.setProperty(pd.getName(), getPropertyValue(pd));
            } catch (Exception e) {
                getLogger().error(pd.getName(), e);
            }
        }
        return prps;
    }

    @OnEnabled
    public void onEnabled(final ConfigurationContext context) throws InitializationException {

    }

    @OnDisabled
    public void shutdown() {

    }

    @Override
    public void execute() throws ProcessException {

    }

}
