package com.bookmyshow.bootstrap;

import org.springframework.scheduling.annotation.EnableScheduling;

/** Turns on @Scheduled jobs. Jobs live in their modules and use JobLock so only one instance runs each job. */
@EnableScheduling
public class SchedulingConfig {}
