package com.arpanbom.orderworkflow;

import io.camunda.client.annotation.Deployment;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@Deployment(resources = {
        "classpath:bpmn/order-approval.bpmn",
        "classpath:dmn/approval-threshold.dmn"
})
public class OrderWorkflowApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderWorkflowApplication.class, args);
    }
}
