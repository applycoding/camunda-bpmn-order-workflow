package com.arpanbom.orderworkflow.api;

import com.arpanbom.orderworkflow.OrderProcess;
import com.arpanbom.orderworkflow.workflow.OrderWorkflow;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class OrderController {

    private final OrderWorkflow orders;

    public OrderController(OrderWorkflow orders) {
        this.orders = orders;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "up", "process", OrderProcess.BPMN_PROCESS_ID);
    }

    @PostMapping("/orders")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public StartOrderResponse start(@Valid @RequestBody StartOrderRequest request) {
        return orders.start(request);
    }
}
