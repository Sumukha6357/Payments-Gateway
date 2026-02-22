package com.example.paymentgateway.integration.controller;

import com.example.paymentgateway.integration.support.IntegrationTestBase;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc(addFilters = false)
public class ValidationErrorIT extends IntegrationTestBase {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Test
  @WithMockUser(username = "user1")
  void shouldReturn422ForInvalidRequest() throws Exception {
    String response = mockMvc.perform(post("/transactions/transfer")
        .header("Idempotency-Key", "idem-invalid")
        .contentType("application/json")
        .content("{}"))
      .andExpect(status().isUnprocessableEntity())
      .andReturn()
      .getResponse()
      .getContentAsString();

    JsonNode body = objectMapper.readTree(response);
    assertThat(body.get("status").asInt()).isEqualTo(422);
    assertThat(body.get("errorCode").asText()).isEqualTo("VALIDATION_ERROR");
    assertThat(body.get("path").asText()).isEqualTo("/transactions/transfer");
  }
}

