package com.lumenml.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(LumenMlProperties.class)
public class LumenMlPropertiesConfig {}
