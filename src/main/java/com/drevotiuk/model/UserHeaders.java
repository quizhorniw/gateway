package com.drevotiuk.model;

import java.util.Map;

import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Represents a container for user-specific headers.
 * <p>
 * This class holds a map of headers where each entry represents a header name
 * and its associated value.
 * It is used to transfer and manage headers associated with user information.
 * </p>
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class UserHeaders {
  /** A map of header names and their corresponding values. */
  private Map<String, String> headers;
}
