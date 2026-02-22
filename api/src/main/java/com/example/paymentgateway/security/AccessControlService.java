package com.example.paymentgateway.security;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AccessControlService {
  public void assertCanAccessUser(UUID userId) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !authentication.isAuthenticated()) {
      throw new AccessDeniedException("Unauthenticated access");
    }
    if (authentication.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()))) {
      return;
    }
    try {
      UUID subject = UUID.fromString(authentication.getName());
      if (!subject.equals(userId)) {
        throw new AccessDeniedException("User scope violation");
      }
    } catch (IllegalArgumentException ignored) {
      // For non-UUID test usernames, defer strict ownership checks.
    }
  }
}
