package com.arpanbom.orderworkflow.workflow;

import io.camunda.client.CamundaClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Deploys the BPMN and DMN once the client exists. A missing broker is logged
 * and does not stop the HTTP API: health still responds, and starting an order
 * returns 503 until Zeebe is up and this process is restarted.
 */
@Component
public class ProcessModelDeployer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ProcessModelDeployer.class);

    private final CamundaClient client;

    public ProcessModelDeployer(CamundaClient client) {
        this.client = client;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            client.newDeployResourceCommand()
                    .addResourceFromClasspath("bpmn/order-approval.bpmn")
                    .addResourceFromClasspath("dmn/approval-threshold.dmn")
                    .send()
                    .join();
            log.info("Deployed order-approval and approval-threshold.");
        } catch (RuntimeException ex) {
            log.warn(
                    "The order process was not deployed. Start Zeebe, then restart this application. Cause: {}",
                    ex.getMessage());
        }
    }
}
