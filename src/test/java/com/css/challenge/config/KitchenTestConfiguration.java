package com.css.challenge.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackages = {"com.css.challenge.model", "com.css.challenge.service"})
public class KitchenTestConfiguration {}
