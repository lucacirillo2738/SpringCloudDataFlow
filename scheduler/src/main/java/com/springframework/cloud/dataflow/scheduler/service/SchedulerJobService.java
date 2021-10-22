package com.springframework.cloud.dataflow.scheduler.service;

import java.util.Date;
import java.util.List;

import com.springframework.cloud.dataflow.scheduler.component.JobScheduleCreator;
import com.springframework.cloud.dataflow.scheduler.job.SpringCloudDataFlowJob;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerMetaData;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;	
import org.quartz.TriggerKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.springframework.cloud.dataflow.scheduler.entity.SchedulerJobInfo;
import com.springframework.cloud.dataflow.scheduler.repository.SchedulerRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Transactional
@Service
public class SchedulerJobService {

	@Autowired
	private Scheduler scheduler;

	@Autowired
	private SchedulerFactoryBean schedulerFactoryBean;

	@Autowired
	private SchedulerRepository schedulerRepository;

	@Autowired
	private ApplicationContext context;

	@Autowired
	private JobScheduleCreator scheduleCreator;

	public SchedulerMetaData getMetaData() throws SchedulerException {
		SchedulerMetaData metaData = scheduler.getMetaData();
		return metaData;
	}

	public List<SchedulerJobInfo> getAllJobList() {
		return schedulerRepository.findAll();
	}

    public boolean deleteJob(SchedulerJobInfo jobInfo) {
        try {
        	SchedulerJobInfo getJobInfo = schedulerRepository.findBySchedulerName(jobInfo.getSchedulerName());
        	schedulerRepository.delete(getJobInfo);
        	log.info(">>>>> jobName = [" + jobInfo.getSchedulerName() + "]" + " deleted.");
            return schedulerFactoryBean.getScheduler().deleteJob(new JobKey(getJobInfo.getJobName(), getJobInfo.getJobGroup()));
        } catch (SchedulerException e) {
            log.error("Failed to delete scheduler - {}", jobInfo.getSchedulerName(), e);
            return false;
        }
    }

    public boolean pauseJob(SchedulerJobInfo jobInfo) {
        try {
        	SchedulerJobInfo getJobInfo = schedulerRepository.findBySchedulerName(jobInfo.getSchedulerName());
        	getJobInfo.setJobStatus("PAUSED");
        	schedulerRepository.save(getJobInfo);
            schedulerFactoryBean.getScheduler().pauseJob(new JobKey(getJobInfo.getJobName(), getJobInfo.getJobGroup()));
            log.info(">>>>> jobName = [" + getJobInfo.getJobName() + "]" + " paused.");
            return true;
        } catch (SchedulerException e) {
            log.error("Failed to pause scheduler - {}", jobInfo.getJobName(), e);
            return false;
        }
    }

    public boolean resumeJob(SchedulerJobInfo jobInfo) {
        try {
        	SchedulerJobInfo getJobInfo = schedulerRepository.findBySchedulerName(jobInfo.getSchedulerName());
        	getJobInfo.setJobStatus("RESUMED");
        	schedulerRepository.save(getJobInfo);
            schedulerFactoryBean.getScheduler().resumeJob(new JobKey(getJobInfo.getJobName(), getJobInfo.getJobGroup()));
            log.info(">>>>> jobName = [" + getJobInfo.getJobName() + "]" + " resumed.");
            return true;
        } catch (SchedulerException e) {
            log.error("Failed to resume scheduler - {}", jobInfo.getSchedulerName(), e);
            return false;
        }
    }

    public boolean startJobNow(SchedulerJobInfo jobInfo) {
        try {
        	SchedulerJobInfo getJobInfo = schedulerRepository.findBySchedulerName(jobInfo.getSchedulerName());
        	getJobInfo.setJobStatus("SCHEDULED & STARTED");
        	schedulerRepository.save(getJobInfo);
            schedulerFactoryBean.getScheduler().triggerJob(new JobKey(jobInfo.getJobName(), jobInfo.getJobGroup()));
            log.info(">>>>> jobName = [" + jobInfo.getJobName() + "]" + " scheduled and started now.");
            return true;
        } catch (SchedulerException e) {
            log.error("Failed to start new scheduler - {}", jobInfo.getJobName(), e);
            return false;
        }
    }

	@SuppressWarnings("deprecation")
	public void saveOrupdate(SchedulerJobInfo scheduleJob) throws Exception {
		scheduleJob.setJobClass(SpringCloudDataFlowJob.class.getName());
		scheduleJob.setCronJob(true);
		if (StringUtils.isEmpty(scheduleJob.getJobId())) {
			log.info("Job Info: {}", scheduleJob);
			scheduleNewJob(scheduleJob);
		} else {
			updateScheduleJob(scheduleJob);
		}
		scheduleJob.setDesc("i am job number " + scheduleJob.getJobId());
		scheduleJob.setInterfaceName("interface_" + scheduleJob.getJobId());
		log.info(">>>>> jobName = [" + scheduleJob.getJobName() + "]" + " created.");
	}

	@SuppressWarnings("unchecked")
	private void scheduleNewJob(SchedulerJobInfo jobInfo) {
		try {
			Scheduler scheduler = schedulerFactoryBean.getScheduler();

			JobDetail jobDetail = JobBuilder
					.newJob((Class<? extends QuartzJobBean>) Class.forName(jobInfo.getJobClass()))
					.withIdentity(jobInfo.getSchedulerName(), jobInfo.getJobGroup())
					.withDescription(jobInfo.getJobName())
					.build();
			if (!scheduler.checkExists(jobDetail.getKey())) {

				jobDetail = scheduleCreator.createJob(
						(Class<? extends QuartzJobBean>) Class.forName(jobInfo.getJobClass()), false, context,
						jobInfo.getJobName(), jobInfo.getJobGroup());

				Trigger trigger;
				if (jobInfo.getCronJob()) {
					trigger = scheduleCreator.createCronTrigger(jobInfo.getSchedulerName(), new Date(),
							jobInfo.getCronExpression(), SimpleTrigger.MISFIRE_INSTRUCTION_FIRE_NOW);
				} else {
					trigger = scheduleCreator.createSimpleTrigger(jobInfo.getSchedulerName(), new Date(),
							jobInfo.getRepeatTime(), SimpleTrigger.MISFIRE_INSTRUCTION_FIRE_NOW);
				}
				scheduler.scheduleJob(jobDetail, trigger);
				jobInfo.setJobStatus("SCHEDULED");
				schedulerRepository.save(jobInfo);
				log.info(">>>>> jobName = [" + jobInfo.getSchedulerName() + "]" + " scheduled.");
			} else {
				log.error("scheduleNewJobRequest.jobAlreadyExist");
			}
		} catch (ClassNotFoundException e) {
			log.error("Class Not Found - {}", jobInfo.getJobClass(), e);
		} catch (SchedulerException e) {
			log.error(e.getMessage(), e);
		}
	}

	private void updateScheduleJob(SchedulerJobInfo jobInfo) {
		Trigger newTrigger;
		if (jobInfo.getCronJob()) {
			newTrigger = scheduleCreator.createCronTrigger(jobInfo.getSchedulerName(), new Date(),
					jobInfo.getCronExpression(), SimpleTrigger.MISFIRE_INSTRUCTION_FIRE_NOW);
		} else {
			newTrigger = scheduleCreator.createSimpleTrigger(jobInfo.getSchedulerName(), new Date(), jobInfo.getRepeatTime(),
					SimpleTrigger.MISFIRE_INSTRUCTION_FIRE_NOW);
		}
		try {
			schedulerFactoryBean.getScheduler().rescheduleJob(TriggerKey.triggerKey(jobInfo.getSchedulerName()), newTrigger);
			jobInfo.setJobStatus("EDITED & SCHEDULED");
			schedulerRepository.save(jobInfo);
			log.info(">>>>> jobName = [" + jobInfo.getSchedulerName() + "]" + " updated and scheduled.");
		} catch (SchedulerException e) {
			log.error(e.getMessage(), e);
		}
	}
}
