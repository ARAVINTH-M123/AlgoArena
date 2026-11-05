package com.problemsolving.platform.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Enables @Scheduled (used by the presence tick and the SSE keep-alive). */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
