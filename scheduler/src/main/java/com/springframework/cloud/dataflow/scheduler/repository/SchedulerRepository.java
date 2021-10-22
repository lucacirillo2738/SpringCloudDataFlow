package com.springframework.cloud.dataflow.scheduler.repository;

import com.springframework.cloud.dataflow.scheduler.entity.SchedulerJobInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SchedulerRepository extends JpaRepository<SchedulerJobInfo, Long> {

	SchedulerJobInfo findByJobName(String jobName);
	SchedulerJobInfo findBySchedulerName(String jobName);
}
