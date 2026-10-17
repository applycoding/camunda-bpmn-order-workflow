package com.arpanbom.orderworkflow.workflow;

import io.camunda.client.CamundaClient;
import io.camunda.client.api.response.ProcessInstanceEvent;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class CamundaProcessStarter implements ProcessStarter {

    private final CamundaClient client;

    public CamundaProcessStarter(CamundaClient client) {
        this.client = client;
    }

    @Override
    public StartedProcess start(String bpmnProcessId, Map<String, Object> variables) {
        ProcessInstanceEvent event = client
                .newCreateInstanceCommand()
                .bpmnProcessId(bpmnProcessId)
                .latestVersion()
                .variables(variables)
                .send()
                .join();
        return new StartedProcess(event.getProcessInstanceKey(), event.getBpmnProcessId());
    }
}
