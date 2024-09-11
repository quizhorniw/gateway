package com.drevotiuk.validator;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

import org.springframework.stereotype.Component;
import org.springframework.http.server.reactive.ServerHttpRequest;

/**
 * This component provides functionality to determine whether a given route is
 * secured or open.
 * <p>
 * It maintains a list of open API endpoints that do not require authentication.
 * Routes that are not
 * included in this list are considered secured.
 * </p>
 */
@Component
public class RouteValidator {
  /**
   * A list of open API endpoints that do not require authentication.
   */
  public static final List<String> openApiEndpoints = Arrays.asList(
      "/users/register",
      "/users/login",
      "/users/confirm");

  /**
   * A predicate to determine if a given request is secured or open.
   * <p>
   * This predicate checks if the request URI path is not contained in the list of
   * open API endpoints.
   * If the path is not in the list, the request is considered secured.
   * </p>
   */
  public Predicate<ServerHttpRequest> isSecured = request -> openApiEndpoints.stream()
      .noneMatch(uri -> request.getURI().getPath().startsWith(uri));
}
