package com.drevotiuk.filter;

import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ServerWebExchange;

import com.drevotiuk.model.UserHeaders;
import com.drevotiuk.validator.RouteValidator;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * This filter is responsible for authorizing requests before they reach the
 * intended service.
 * <p>
 * It checks if a request is secured based on routes provided by the
 * {@link RouteValidator}. If the request is
 * secured, it verifies the authorization by querying the user service. If the
 * authorization is successful, it
 * enriches the request with user-specific headers and proceeds with the
 * request. If the authorization fails, it
 * responds with a 403 Forbidden status.
 * </p>
 */
@Component
@Slf4j
public class AuthFilter extends AbstractGatewayFilterFactory<AuthFilter.Config> {
  @Value("${services.user.uri}")
  private String userServiceUrl;
  @Value("${api.version}")
  private String apiVersion;

  private final RouteValidator routeValidator;
  private final RestTemplate restTemplate;

  public AuthFilter(RouteValidator routeValidator, RestTemplate restTemplate) {
    super(Config.class);
    this.routeValidator = routeValidator;
    this.restTemplate = restTemplate;
  }

  /**
   * Applies the filter logic for authorization.
   * <p>
   * This method checks if the request is secured. If not, it allows the request
   * to proceed. If the request is
   * secured, it attempts to authorize it by calling the user service. Based on
   * the authorization result, it
   * either proceeds with the request or responds with an access denied error.
   * </p>
   * 
   * @param config the configuration for the filter
   * @return a {@link GatewayFilter} that processes the request based on its
   *         authorization status
   */
  @Override
  public GatewayFilter apply(Config config) {
    log.info("Authorizing request");
    return (exchange, chain) -> {
      ServerHttpRequest request = exchange.getRequest();
      if (!routeValidator.isSecured.test(request))
        return chain.filter(exchange); // Allow unsecured routes

      HttpHeaders headers = new HttpHeaders();
      headers.addAll(request.getHeaders());

      try {
        Mono<Void> successfulAuthorization = authorizeRequest(exchange, chain, headers);
        log.info("Successful authorization");
        return successfulAuthorization;
      } catch (HttpClientErrorException e) {
        log.warn("Authorization failed: {}", e.getMessage());
        return handleAccessDenied(exchange);
      }
    };
  }

  /**
   * Authorizes the request by querying the user service and mutating the request
   * with user headers.
   * 
   * @param exchange the server exchange containing the request and response
   * @param chain    the gateway filter chain to proceed with the request
   * @param headers  the headers to include in the authorization request
   * @return a {@link Mono} representing the completion of the authorization
   *         process
   */
  private Mono<Void> authorizeRequest(ServerWebExchange exchange, GatewayFilterChain chain,
      HttpHeaders headers) {
    HttpEntity<String> httpEntity = new HttpEntity<>(headers);
    String url = String.format("%s/api/%s/users/auth", userServiceUrl, apiVersion);

    UserHeaders userHeaders = restTemplate.exchange(url, HttpMethod.GET, httpEntity,
        UserHeaders.class).getBody();

    ServerHttpRequest mutatedRequest = mutateRequestWithUserHeaders(exchange.getRequest(), userHeaders);
    return chain.filter(exchange.mutate().request(mutatedRequest).build());
  }

  /**
   * Mutates the request by adding user headers.
   * 
   * @param request     the original request
   * @param userHeaders the user headers to add to the request
   * @return a mutated {@link ServerHttpRequest} with user headers added
   */
  private ServerHttpRequest mutateRequestWithUserHeaders(ServerHttpRequest request,
      UserHeaders userHeaders) {
    ServerHttpRequest.Builder mutatedRequestBuilder = request.mutate();
    userHeaders.getHeaders().forEach(mutatedRequestBuilder::header);

    return mutatedRequestBuilder.build();
  }

  /**
   * Handles an access denied situation by responding with a 403 Forbidden status
   * and an error message.
   * 
   * @param exchange the server exchange containing the request and response
   * @return a {@link Mono} representing the completion of the access denied
   *         response
   */
  private Mono<Void> handleAccessDenied(ServerWebExchange exchange) {
    ServerHttpResponse response = exchange.getResponse();
    response.setStatusCode(HttpStatus.FORBIDDEN);
    response.getHeaders().add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

    String errorMessage = "{\"error\": \"Access denied\"}";
    DataBuffer buffer = response.bufferFactory().wrap(errorMessage.getBytes(StandardCharsets.UTF_8));
    return response.writeWith(Mono.just(buffer));
  }

  /**
   * Configuration class for {@link AuthFilter}.
   */
  public static class Config {
  }
}
