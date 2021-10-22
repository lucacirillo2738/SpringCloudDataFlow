/*
 * Copyright 2018-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.cloud.dataflow.autoconfigure.local;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.dataflow.autoconfigure.model.SchedulerJobInfo;
import org.springframework.cloud.dataflow.server.config.OnLocalPlatform;
import org.springframework.cloud.dataflow.server.config.features.SchedulerConfiguration;
import org.springframework.cloud.dataflow.server.service.SchedulerService;
import org.springframework.cloud.deployer.spi.core.AppDefinition;
import org.springframework.cloud.deployer.spi.scheduler.ScheduleInfo;
import org.springframework.cloud.deployer.spi.scheduler.ScheduleRequest;
import org.springframework.cloud.deployer.spi.scheduler.Scheduler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

/**
 * @author Luca Cirillo
 */
@Configuration
@Conditional({OnLocalPlatform.class, SchedulerConfiguration.SchedulerConfigurationPropertyChecker.class})
public class LocalSchedulerAutoConfiguration {

    @Value("${spring.cloud.dataflow.scheduler.url}")
    private String schedulerUrl;

    private static final String SCHEDULER_CRON_EXP_NAME = "scheduler.cron.expression";
    private static final String SCHEDULER_TASK_DEFINITION_NAME = "scheduler.task.definition";

    @Bean
    @ConditionalOnMissingBean
    public Scheduler localScheduler() {
        return new Scheduler() {
            public void schedule(ScheduleRequest scheduleRequest) {
                String cron = scheduleRequest.getSchedulerProperties().get(SCHEDULER_CRON_EXP_NAME);
                String taskDefinition = scheduleRequest.getSchedulerProperties().get(SCHEDULER_TASK_DEFINITION_NAME);
                String scheduleName = scheduleRequest.getScheduleName();
                RestTemplate restTemplate = new RestTemplate();
                ResponseEntity<String> executeResponse = restTemplate.postForEntity(schedulerUrl + "/api/saveOrUpdate?schedulerName="+scheduleName+"&jobName="+taskDefinition+"&jobGroup=sisal&jobClass=batch-job&cronExpression=" + cron, null, String.class);
                System.out.println(executeResponse.getBody());
            }

            public void unschedule(String scheduleName) {
                RestTemplate restTemplate = new RestTemplate();
                ResponseEntity<String> executeResponse = restTemplate.postForEntity(schedulerUrl + "/api/deleteJob?schedulerName="+scheduleName+"&jobGroup=sisal&jobClass=batch-job", null, String.class);
            }

            public List<ScheduleInfo> list(String taskDefinitionName) {
                return list();
            }

            public List<ScheduleInfo> list() {

                RestTemplate restTemplate = new RestTemplate();
                ResponseEntity<SchedulerJobInfo[]> executeResponse = restTemplate.getForEntity(schedulerUrl + "/api/getAllJobs", SchedulerJobInfo[].class);

                if(executeResponse != null && executeResponse.getBody() != null) {
                    return java.util.Arrays.asList(executeResponse.getBody()).stream().map(s -> {
                        ScheduleInfo scheduleInfo = new ScheduleInfo();
                        scheduleInfo.setScheduleName(s.getSchedulerName());
                        scheduleInfo.setTaskDefinitionName(s.getJobName());
                        scheduleInfo.setScheduleProperties(new java.util.HashMap<>());

                        return scheduleInfo;
                    }).collect(Collectors.toList());
                }else{
                    return Collections.emptyList();
                }
            }
        };
    }

    @Bean
    public SchedulerService schedulerService(Resource resource){
        Scheduler scheduler = localScheduler();
        return new SchedulerService() {

            @Override
            public void schedule(String scheduleName, String taskDefinitionName, Map<String, String> taskProperties, List<String> commandLineArgs, String platformName) {
                taskProperties.put(SCHEDULER_TASK_DEFINITION_NAME, taskDefinitionName);
                AppDefinition appDefinition = new AppDefinition(platformName, null);
                ScheduleRequest scheduleRequest = new ScheduleRequest(appDefinition, taskProperties, new java.util.HashMap<>(), scheduleName, resource);
                scheduler.schedule(scheduleRequest);
            }

            @Override
            public void schedule(String scheduleName, String taskDefinitionName, Map<String, String> taskProperties, List<String> commandLineArgs) {
                AppDefinition appDefinition = new AppDefinition("DAFAULT", null);
                ScheduleRequest scheduleRequest = new ScheduleRequest(appDefinition, taskProperties, new java.util.HashMap<>(), scheduleName, resource);
                scheduler.schedule(scheduleRequest);
            }

            @Override
            public void unschedule(String scheduleName) {
                scheduler.unschedule(scheduleName);
            }

            @Override
            public void unschedule(String scheduleName, String platformName) {
                scheduler.unschedule(scheduleName);
            }

            @Override
            public void unscheduleForTaskDefinition(String taskDefinitionName) {

            }

            @Override
            public List<ScheduleInfo> list(Pageable pageable, String taskDefinitionName, String platformName) {
                return null;
            }

            @Override
            public Page<ScheduleInfo> list(Pageable pageable, String platformName) {
                return null;
            }

            @Override
            public Page<ScheduleInfo> list(Pageable pageable) {
                return null;
            }

            @Override
            public List<ScheduleInfo> list(String taskDefinitionName, String platformName) {
                return scheduler.list();
            }

            @Override
            public List<ScheduleInfo> list(String taskDefinitionName) {
                return scheduler.list();
            }

            @Override
            public List<ScheduleInfo> listForPlatform(String platformName) {
                return scheduler.list();
            }

            @Override
            public List<ScheduleInfo> list() {
                return scheduler.list();
            }

            @Override
            public ScheduleInfo getSchedule(String scheduleName, String platformName) {
                return null;
            }

            @Override
            public ScheduleInfo getSchedule(String scheduleName) {
                return null;
            }
        };
    }

    @Bean
    public Resource resource(){
        return new Resource() {
            @Override
            public boolean exists() {
                return true;
            }

            @Override
            public URL getURL() throws IOException {
                return null;
            }

            @Override
            public URI getURI() throws IOException {
                return null;
            }

            @Override
            public File getFile() throws IOException {
                return null;
            }

            @Override
            public long contentLength() throws IOException {
                return 0;
            }

            @Override
            public long lastModified() throws IOException {
                return 0;
            }

            @Override
            public Resource createRelative(String s) throws IOException {
                return null;
            }

            @Override
            public String getFilename() {
                return null;
            }

            @Override
            public String getDescription() {
                return null;
            }

            @Override
            public InputStream getInputStream() throws IOException {
                return null;
            }
        };
    }
}
