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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Luca Cirillo
 */
@Configuration
@Conditional({OnLocalPlatform.class, SchedulerConfiguration.SchedulerConfigurationPropertyChecker.class})
public class LocalSchedulerAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    public Scheduler localScheduler() {
        return new Scheduler() {
            public void schedule(ScheduleRequest scheduleRequest) {
                RestTemplate restTemplate = new RestTemplate();
                ResponseEntity<String> executeResponse = restTemplate.postForEntity("http://dataflow-scheduler:8080/api/saveOrUpdate?jobName=batch-job&jobGroup=sisal&jobClass=batch-job&cronExpression=0/15 * * * * ?", null, String.class);
                System.out.println(executeResponse.getBody());
            }

            public void unschedule(String scheduleName) {
                throw new UnsupportedOperationException("Implementing...");
            }

            public List<ScheduleInfo> list(String taskDefinitionName) {
                return Collections.emptyList();
            }

            public List<ScheduleInfo> list() {

                RestTemplate restTemplate = new RestTemplate();
                ResponseEntity<List<SchedulerJobInfo>> executeResponse = restTemplate.getForEntity("http://dataflow-scheduler:8080/api/getAllJobs", null, String.class);

                if(executeResponse != null && executeResponse.getBody() != null) {
                    return executeResponse.getBody().stream().map(s -> {
                        ScheduleInfo scheduleInfo = new ScheduleInfo();
                        scheduleInfo.setScheduleName(s.getJobName());
                        scheduleInfo.setTaskDefinitionName(s.getJobName());
                        return scheduleInfo;
                    }).collect(Collectors.toList());
                }else{
                    return Collections.emptyList();
                }
            }
        };
    }

    @Bean
    public SchedulerService schedulerService(){
        Scheduler scheduler = localScheduler();
        return new SchedulerService() {

            @Override
            public void schedule(String scheduleName, String taskDefinitionName, Map<String, String> taskProperties, List<String> commandLineArgs, String platformName) {
                AppDefinition appDefinition = new AppDefinition(platformName, null);
                ScheduleRequest scheduleRequest = new ScheduleRequest(appDefinition, taskProperties, null, scheduleName, null);
                scheduler.schedule(scheduleRequest);
            }

            @Override
            public void schedule(String scheduleName, String taskDefinitionName, Map<String, String> taskProperties, List<String> commandLineArgs) {
                AppDefinition appDefinition = new AppDefinition("DAFAULT", null);
                ScheduleRequest scheduleRequest = new ScheduleRequest(appDefinition, taskProperties, null, scheduleName, null);
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
}
