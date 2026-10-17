package com.arpanbom.orderworkflow.workflow;

import java.util.Map;

public interface ProcessStarter {

    StartedProcess start(String bpmnProcessId, Map<String, Object> variables);

    record StartedProcess(long processInstanceKey, String bpmnProcessId) {}
}
