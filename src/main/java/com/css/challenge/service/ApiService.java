package com.css.challenge.service;

import com.css.challenge.model.InputOrder;
import com.css.challenge.model.Response;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class ApiService {
  private final WebClient webClient = WebClient.create("https://api.cloudkitchens.com");

  private final ObjectMapper objectMapper = new ObjectMapper();

  private static final String READ_PATH = "/interview/challenge/new";
  private static final String SUBMIT_PATH = "/interview/challenge/solve";

  private static final String TEST_ID_HEADER = "x-test-id";
  private static final String AUTH = "ahsosfrgwygs";

  private int testId;

  public List<InputOrder> getInputOrders() {
    try {
      ResponseEntity<String> response =
          webClient
              .get()
              .uri(uriBuilder -> uriBuilder.path(READ_PATH).queryParam("auth", AUTH).build())
              .retrieve()
              .toEntity(String.class)
              .block();

      List<String> testIdValues =
          Objects.requireNonNull(response).getHeaders().entrySet().stream()
              .filter(entry -> entry.getKey().equals(TEST_ID_HEADER))
              .map(Map.Entry::getValue)
              .flatMap(List::stream)
              .toList();

      testId = Integer.parseInt(testIdValues.getFirst());
      log.info("Test ID: {}", testId);

      String inputJson = response.getBody();
      return objectMapper.readValue(inputJson, new TypeReference<>() {});
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  public void submitResponse(Response response) {
    ResponseEntity<String> submissionResponse =
        webClient
            .post()
            .uri(uriBuilder -> uriBuilder.path(SUBMIT_PATH).queryParam("auth", AUTH).build())
            .contentType(MediaType.APPLICATION_JSON)
            .header(TEST_ID_HEADER, String.valueOf(testId))
            .body(Mono.just(response), Response.class)
            .retrieve()
            .toEntity(String.class)
            .block();

    if (!submissionResponse.getStatusCode().is2xxSuccessful()) {
      throw new RuntimeException("Status code not 200" + submissionResponse);
    }
    if (!"pass".equals(submissionResponse.getBody())) {
      throw new RuntimeException("Invalid result" + submissionResponse);
    }

    log.info("Submission successful");
  }
}
