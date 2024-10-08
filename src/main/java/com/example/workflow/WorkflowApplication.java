package com.example.workflow;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import io.camunda.zeebe.spring.client.EnableZeebeClient;
import io.camunda.zeebe.spring.client.annotation.Deployment;


@SpringBootApplication
@EnableZeebeClient
@Deployment(resources = {"classpath*:*.bpmn", "classpath*:*.form"})
public class WorkflowApplication {

	
	public static void main(String[] args) {
		SpringApplication.run(WorkflowApplication.class, args);
	}
	

}
