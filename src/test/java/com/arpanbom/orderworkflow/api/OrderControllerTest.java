package com.arpanbom.orderworkflow.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.arpanbom.orderworkflow.workflow.OrderWorkflow;
import com.arpanbom.orderworkflow.workflow.WorkflowUnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

class OrderControllerTest {

    private OrderWorkflow workflow;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        workflow = mock(OrderWorkflow.class);
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mvc = MockMvcBuilders.standaloneSetup(new OrderController(workflow))
                .setControllerAdvice(new ApiExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void healthReportsTheProcess() throws Exception {
        mvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("up"))
                .andExpect(jsonPath("$.process").value("order-approval"));
    }

    @Test
    void acceptsAnOrderAndReturnsTheProcessInstanceKeyAsText() throws Exception {
        when(workflow.start(any())).thenReturn(
                new StartOrderResponse("ord-1", "2251799813685249", "order-approval"));

        mvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderId":"ord-1","customerId":"cust-9","amount":1250.50,"currency":"EUR"}
                                """))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.orderId").value("ord-1"))
                .andExpect(jsonPath("$.processInstanceKey").value("2251799813685249"))
                .andExpect(jsonPath("$.bpmnProcessId").value("order-approval"));

        verify(workflow).start(any(StartOrderRequest.class));
    }

    @Test
    void rejectsAnIncompleteOrder() throws Exception {
        mvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderId":"","customerId":"cust 9","amount":0,"currency":"euro"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("The order request is not valid."))
                .andExpect(jsonPath("$.details").isArray());
    }

    @Test
    void rejectsMalformedJson() throws Exception {
        mvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("not-json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("The order request is not valid JSON."));
    }

    @Test
    void reportsTheEngineAsUnavailable() throws Exception {
        when(workflow.start(any())).thenThrow(new WorkflowUnavailableException(
                "The workflow engine did not accept the order. Start Zeebe and try again.",
                new IllegalStateException("down")));

        mvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderId":"ord-1","customerId":"cust-9","amount":10.00,"currency":"EUR"}
                                """))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value(
                        "The workflow engine did not accept the order. Start Zeebe and try again."));
    }
}
