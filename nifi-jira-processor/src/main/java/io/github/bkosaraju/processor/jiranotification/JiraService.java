package io.github.bkosaraju.processor.jiranotification;

import com.atlassian.jira.rest.client.api.IssueRestClient;
import com.atlassian.jira.rest.client.api.JiraRestClient;
import com.atlassian.jira.rest.client.api.MetadataRestClient;
import com.atlassian.jira.rest.client.api.ProjectRestClient;
import com.atlassian.jira.rest.client.api.domain.*;
import com.atlassian.jira.rest.client.api.domain.input.IssueInputBuilder;
import com.atlassian.jira.rest.client.internal.async.AsynchronousJiraRestClientFactory;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.concurrent.ExecutionException;


public class JiraService {

    public String priorityName;
    private MetadataRestClient jiraMetadataClient;
    public String issueTypeName;
    public String jiraUser;
    public String jiraPassword;
    public String jiraUrl;
    public String projectKey;
    public String projectName;
    public List<String> tags;
    public String Summary;
    public String Description;

    private Long priorityId;
    private Long issueTypeId;
    private Object IOException;

    public String getSummary() {
        return Summary;
    }

    public void setSummary(String summary) {
        Summary = summary;
    }

    public String getDescription() {
        return Description;
    }

    public void setDescription(String description) {
        Description = description;
    }


    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }


    public String getProjectKey() {
        return projectKey;
    }

    public void setProjectKey(String projectKey) {
        this.projectKey = projectKey;
    }


    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }


    public String getIssueTypeName() {
        return issueTypeName;
    }

    public void setIssueTypeName(String issueTypeName) {
        this.issueTypeName = issueTypeName;
    }

    public String getPriorityName() {
        return priorityName;
    }

    public void setPriorityName(String priorityName) {
        this.priorityName = priorityName;
    }

    public MetadataRestClient getJiraMetadataClient() {
        return jiraMetadataClient;
    }

    public void setJiraMetadataClient(MetadataRestClient jiraMetadataClient) {
        this.jiraMetadataClient = jiraMetadataClient;
    }

    public String getJiraUser() {
        return jiraUser;
    }

    public void setJiraUser(String jiraUser) {
        this.jiraUser = jiraUser;
    }

    public String getJiraPassword() {
        return jiraPassword;
    }

    public void setJiraPassword(String jiraPassword) {
        this.jiraPassword = jiraPassword;
    }

    public String getJiraUrl() {
        return jiraUrl;
    }

    public void setJiraUrl(String jiraUrl) {
        this.jiraUrl = jiraUrl;
    }


    public Long getPriority() {
        this.setPriorityName((this.priorityName == null) ? this.priorityName = "Low" : this.priorityName);
        try {
            for (Priority priority : getJiraMetadataClient().getPriorities().get()) {
                if (priority.getName().equalsIgnoreCase(this.getPriorityName())) {
                    priorityId = priority.getId();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return priorityId;
    }

    public Long getIssueType() {
        this.issueTypeName = (this.issueTypeName == null) ? this.issueTypeName = "Bug" : this.issueTypeName;
        try {
            for (IssueType issueTypes : getJiraMetadataClient().getIssueTypes().get()) {
                if (issueTypes.getName().equalsIgnoreCase(this.getIssueTypeName())) {
                    issueTypeId = issueTypes.getId();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return issueTypeId;
    }

    public String createJiraTicket()  {
        String ticketId = null;
        try {
            URI jiraURI = URI.create(this.getJiraUrl());
            AsynchronousJiraRestClientFactory jiraRestClientFactory = new AsynchronousJiraRestClientFactory();
            JiraRestClient jiraRestClient = jiraRestClientFactory.createWithBasicHttpAuthentication(jiraURI, this.getJiraUser(), this.getJiraPassword());
            this.setJiraMetadataClient(jiraRestClient.getMetadataClient());
            ProjectRestClient projectClient = jiraRestClient.getProjectClient();
            for (BasicProject projects : projectClient.getAllProjects().get()) {
                if (projects.getName().equalsIgnoreCase(this.getProjectName())) {
                    this.setProjectKey(projects.getKey());
                }
            }
            IssueRestClient issueClient = jiraRestClient.getIssueClient();

            IssueInputBuilder issueInputBuilder= new IssueInputBuilder()
                    .setProjectKey(projectKey)
                    .setIssueTypeId(getIssueType())
                    .setPriorityId(getPriority())
                    .setSummary(getSummary())
                    .setDescription(getDescription());

            if (getTags() != null && getTags().toArray().length >0 ) {
            issueInputBuilder.setFieldValue("labels", tags);
            }
            ticketId = issueClient.createIssue(issueInputBuilder.build()).claim().getKey();
            jiraRestClient.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ticketId;
    }
}



