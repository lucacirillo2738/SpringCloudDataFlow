package com.stackabuse.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class GetTaskResponse {

    public Embedded _embedded;
    public Links _links;
    public Page page;

    @Data
    public static class AppProperties {
        @JsonProperty("management.metrics.tags.service")
        public String managementMetricsTagsService;
        @JsonProperty("spring.datasource.username")
        public String springDatasourceUsername;
        @JsonProperty("spring.datasource.url")
        public String springDatasourceUrl;
        @JsonProperty("spring.datasource.driverClassName")
        public String springDatasourceDriverClassName;
        @JsonProperty("management.metrics.tags.application")
        public String managementMetricsTagsApplication;
        @JsonProperty("spring.cloud.task.name")
        public String springCloudTaskName;
        @JsonProperty("spring.datasource.password")
        public String springDatasourcePassword;
    }

    @Data
    public static class DeploymentProperties {
        public String appMyTaskFoo;
        public String deployerMyTaskSomethingElse;
    }

    @Data
    public static class Self {
        public String href;
    }

    @Data
    public static class Links {
        public Self self;
    }

    @Data
    public static class TaskExecutionResourceList {
        public int executionId;
        public Object exitCode;
        public String taskName;
        public Object startTime;
        public Object endTime;
        public Object exitMessage;
        public List<Object> arguments;
        public List<Object> jobExecutionIds;
        public Object errorMessage;
        public String externalExecutionId;
        public Object parentExecutionId;
        public String resourceUrl;
        public AppProperties appProperties;
        public DeploymentProperties deploymentProperties;
        public String platformName;
        public String taskExecutionStatus;
        public Links _links;
    }

    @Data
    public static class Embedded {
        public List<TaskExecutionResourceList> taskExecutionResourceList;
    }

    @Data
    public static class Page {
        public int size;
        public int totalElements;
        public int totalPages;
        public int number;
    }

}


