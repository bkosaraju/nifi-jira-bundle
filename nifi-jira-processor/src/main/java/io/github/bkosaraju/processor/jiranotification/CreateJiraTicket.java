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
package io.github.bkosaraju.processor.jiranotification;

import org.apache.nifi.components.AllowableValue;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.expression.ExpressionLanguageScope;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.annotation.behavior.ReadsAttribute;
import org.apache.nifi.annotation.behavior.ReadsAttributes;
import org.apache.nifi.annotation.behavior.WritesAttribute;
import org.apache.nifi.annotation.behavior.WritesAttributes;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.SeeAlso;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.processor.exception.ProcessException;
import org.apache.nifi.processor.AbstractProcessor;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.processor.ProcessorInitializationContext;
import org.apache.nifi.processor.Relationship;
import org.apache.nifi.processor.util.StandardValidators;


import org.apache.nifi.logging.ComponentLog;
import io.github.bkosaraju.service.jiranotification.JiraCredentialServiceApi;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;

@Tags({"notification", "jira", "jiranotification", "custom"})
@CapabilityDescription("Processor to post the errors to notification service")
@SeeAlso({})
@ReadsAttributes({@ReadsAttribute(attribute = "", description = "")})
@WritesAttributes({@WritesAttribute(attribute = "", description = "")})
public class CreateJiraTicket extends AbstractProcessor {

    public static final PropertyDescriptor PROP_JIRA_CREDENTIAL_SERVICE = new PropertyDescriptor.Builder()
            .name("Jira Credentials Service")
            .description("credentials loader for jira")
            .required(false)
            .identifiesControllerService(JiraCredentialServiceApi.class)
            .build();
    public static final PropertyDescriptor PROP_SUMMARY = new PropertyDescriptor
            .Builder().name("Summary")
            .displayName("Ticket Summary")
            .description("Jira Ticket Summary")
            .required(true)
            .expressionLanguageSupported(ExpressionLanguageScope.FLOWFILE_ATTRIBUTES)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();

    public static final PropertyDescriptor PROP_DESCRIPTION = new PropertyDescriptor
            .Builder().name("Description")
            .displayName("Ticket Description")
            .description("Jira Ticket description")
            .required(true)
            .expressionLanguageSupported(ExpressionLanguageScope.FLOWFILE_ATTRIBUTES)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();

    public static final AllowableValue PRIORITYRATING4 = new AllowableValue("Lowest", "Lowest",
            "Lowest");
    public static final AllowableValue PRIORITYRATING3 = new AllowableValue("Low", "Low",
            "Low");
    public static final AllowableValue PRIORITYRATING2 = new AllowableValue("High", "High",
            "High");
    public static final AllowableValue PRIORITYRATING1 = new AllowableValue("Highest", "Highest",
            "Highest");


    public static final AllowableValue IT1 = new AllowableValue("Task", "Task", "Task Applies to All types of projects");
    public static final AllowableValue IT2 = new AllowableValue("Epic", "Epic", "Epic to Software Project types");
    public static final AllowableValue IT3 = new AllowableValue("Bug", "Bug", "Bug applies to Software Project types");
    public static final AllowableValue IT4 = new AllowableValue("Story", "Story", "Story applies to Software Project types");
    public static final AllowableValue IT5 = new AllowableValue("Subtask", "Subtask", "Subtask applies to Software and Business Project types");
    public static final AllowableValue IT6 = new AllowableValue("Change", "Change", "Change Applies to Service Desk Projects");
    public static final AllowableValue IT7 = new AllowableValue("IT help", "IT help", "IT help Applies to Service Desk Projects");
    public static final AllowableValue IT8 = new AllowableValue("Incident", "Incident", "Incident Applies to Service Desk Projects");
    public static final AllowableValue IT9 = new AllowableValue("New feature", "New feature", "New feature Applies to Service Desk Projects");
    public static final AllowableValue IT10 = new AllowableValue("Problem", "Problem", "Problem Applies to Service Desk Projects");
    public static final AllowableValue IT11 = new AllowableValue("Service request", "Service request", "Service request Applies to Service Desk Projects");
    public static final AllowableValue IT12 = new AllowableValue("Service request with approval", "Service request with approval", "Service request with approval Applies to Service Desk Projects");
    public static final AllowableValue IT13 = new AllowableValue("Support", "Support", "Support Applies to Service Desk Projects");


    public static final PropertyDescriptor PROP_PRIORITY_RATING = new PropertyDescriptor
            .Builder().name("priorityRating")
            .displayName("Priority")
            .description("Incident/Ticket Priority in Jira")
            .allowableValues(PRIORITYRATING1, PRIORITYRATING2, PRIORITYRATING3, PRIORITYRATING4)
            .required(false)
            .defaultValue(PRIORITYRATING4.getValue())
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();

    public static final PropertyDescriptor PROP_ISSUE_TYPE = new PropertyDescriptor
            .Builder().name("issueType")
            .displayName("Issue Type")
            .description("Jira Incident Type")
            .allowableValues(IT1, IT2, IT3, IT4, IT5, IT6, IT7, IT8, IT9, IT10, IT11, IT12, IT13)
            .required(true)
            .defaultValue(IT1.getValue())
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();

    public static final Relationship REL_SUCCESS = new Relationship.Builder()
            .name("Success")
            .description("when successfully posted the notification")
            .build();

    public static final Relationship REL_FAIL = new Relationship.Builder()
            .name("Failure")
            .description("when not able posted the notification")
            .build();

    private List<PropertyDescriptor> descriptors;

    private Set<Relationship> relationships;

    private ComponentLog logger;


    @Override
    protected void init(final ProcessorInitializationContext context) {
        final List<PropertyDescriptor> descriptors = new ArrayList<PropertyDescriptor>();
        descriptors.add(PROP_JIRA_CREDENTIAL_SERVICE);
        descriptors.add(PROP_SUMMARY);
        descriptors.add(PROP_DESCRIPTION);
        descriptors.add(PROP_PRIORITY_RATING);
        descriptors.add(PROP_ISSUE_TYPE);
        this.descriptors = Collections.unmodifiableList(descriptors);

        final Set<Relationship> relationships = new HashSet<Relationship>();
        relationships.add(REL_SUCCESS);
        relationships.add(REL_FAIL);
        this.relationships = Collections.unmodifiableSet(relationships);

        logger = context.getLogger();
    }

    @Override
    public Set<Relationship> getRelationships() {
        return this.relationships;
    }

    @Override
    public final List<PropertyDescriptor> getSupportedPropertyDescriptors() {
        return descriptors;
    }

    @Override
    protected PropertyDescriptor getSupportedDynamicPropertyDescriptor(final String propertyDescriptorName) {
        return new PropertyDescriptor.Builder()
                .required(false)
                .name(propertyDescriptorName)
                .expressionLanguageSupported(ExpressionLanguageScope.FLOWFILE_ATTRIBUTES)
                .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
                .dynamic(true)
                .build();
    }

//    @OnScheduled
//    public void onScheduled(final ProcessContext context) {
//
//    }

    @Override
    public void onTrigger(final ProcessContext context, final ProcessSession session) throws ProcessException {
        FlowFile flowFile = session.get();
        if (flowFile == null) {
            flowFile = session.create();
        }

        JiraService jiraTicket = new JiraService();
        Properties ps, jiraprops;
        ps = jiraprops = new Properties();
        FlowFile updatedFlowFile = flowFile;
        try {
            final JiraCredentialServiceApi cs = context.getProperty(PROP_JIRA_CREDENTIAL_SERVICE).asControllerService(JiraCredentialServiceApi.class);
            jiraprops = cs.getConnectionProperties();
        } catch (Exception e) {
            logger.error("Unable to retrive the Jira properties form Controller service", e);
            updatedFlowFile = session.putAttribute(updatedFlowFile, "NotifacionError", e.getLocalizedMessage());
            session.transfer(updatedFlowFile, REL_FAIL);
        }

        jiraTicket.setJiraUrl(jiraprops.getProperty("jiraURL"));
        jiraTicket.setJiraUser(jiraprops.getProperty("jiraUser"));
        jiraTicket.setJiraPassword(jiraprops.getProperty("jiraPassword"));
        jiraTicket.setPriorityName(context.getProperty(PROP_PRIORITY_RATING).evaluateAttributeExpressions(flowFile).getValue());
        jiraTicket.setIssueTypeName(context.getProperty(PROP_ISSUE_TYPE).evaluateAttributeExpressions(flowFile).getValue());
        jiraTicket.setSummary(context.getProperty(PROP_SUMMARY).evaluateAttributeExpressions(flowFile).getValue());
        jiraTicket.setProjectName(jiraprops.getProperty("jiraProject"));


        boolean additionalPropertiesFlag = false;
        for (Map.Entry<PropertyDescriptor, String> custProp : context.getProperties().entrySet()) {
            if (custProp.getKey().isDynamic()) {
                additionalPropertiesFlag = true;
                ps.setProperty(custProp.getKey().getName(), context.getProperty(custProp.getKey()).evaluateAttributeExpressions(flowFile).getValue());
            }
        }
        if (ps.containsKey("labels")) {
            jiraTicket.setTags(Arrays.asList(ps.getProperty("labels").split(",")));
        }
        StringWriter writer = new StringWriter();
        try {
            ps.store(new PrintWriter(writer), "");
        } catch (IOException e) {
            getLogger().error("Unable to read additonal atrributes from processor");
            e.printStackTrace();
        }
        String strProps = writer.getBuffer().toString();
        String mdTable = strProps.substring(strProps.indexOf('\n') + 1)
                .replaceAll("^#(.+)", "Event Time=$1")
                .replaceAll("\\\\", "")
                .replaceAll("(?m)^|=|$", "|");

        jiraTicket.setDescription(context.getProperty(PROP_DESCRIPTION).evaluateAttributeExpressions(flowFile).getValue() + ((additionalPropertiesFlag) ? "\n\nh1. Additional Attributes \nh1.\n" + mdTable : ""));

        try {
            String ticketId = jiraTicket.createJiraTicket();
            getLogger().info("Successfully create jira ticket: " + ticketId);
        } catch (Exception e) {
            getLogger().error("Unable to Create Ticket with given configurations");
            getLogger().error("project Name: "+ jiraTicket.getProjectKey()+
                            "\nJira URL:"+jiraTicket.getJiraUrl()+
                            "\nJira User"+jiraTicket.getJiraUser()+
                            "\nPriority"+jiraTicket.getPriorityName()+
                            "\nIssue Type"+jiraTicket.getIssueTypeName()+
                            "\nSummary:"+jiraTicket.getSummary()+
                            "\nDescription:"+jiraTicket.getDescription()
            );
            e.printStackTrace();
            throw e;
        }
        @SuppressWarnings({"unchecked", "rawtypes"})
        Map<String, String> psMap = (Map) ps;

        updatedFlowFile = session.putAllAttributes(flowFile, psMap);
        try {
            session.transfer(updatedFlowFile, REL_SUCCESS);
        } catch (Exception e) {
            updatedFlowFile = session.putAttribute(updatedFlowFile, "NotificationError", e.getLocalizedMessage());
            session.transfer(updatedFlowFile, REL_FAIL);
        }
    }
}
