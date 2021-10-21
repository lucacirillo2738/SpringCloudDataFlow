package com.stackabuse.job;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stackabuse.model.GetTaskResponse;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.impl.JobDetailImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.web.client.RestTemplate;

import java.util.stream.IntStream;

@Slf4j
@DisallowConcurrentExecution
public class SpringCloudDataFlowJob extends QuartzJobBean {

    @Value("${scdf.url}")
    private String scdfUrl;

    @SneakyThrows
    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        log.info("SpringCloudDataFlowJob Start................");
        String name = ((JobDetailImpl)context.getJobDetail()).getName();
        RestTemplate restTemplate = new RestTemplate();

        ObjectMapper om = new ObjectMapper();
        String json = restTemplate.getForObject(scdfUrl + "/tasks/executions?name="+name, String.class);
        GetTaskResponse task = om.readValue(json, GetTaskResponse.class);
        ResponseEntity<String> executeResponse = restTemplate.postForEntity(scdfUrl + "/tasks/executions?name="+name, null, String.class);
        log.info(context.getJobDetail().getDescription());

        log.info("SpringCloudDataFlowJob End................");
    }
}
