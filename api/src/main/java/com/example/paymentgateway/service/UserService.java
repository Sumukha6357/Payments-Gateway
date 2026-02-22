package com.example.paymentgateway.service;

import com.example.paymentgateway.domain.entity.User;
import com.example.paymentgateway.dto.UserCreateRequest;
import com.example.paymentgateway.dto.UserResponse;
import com.example.paymentgateway.exception.NotFoundException;
import com.example.paymentgateway.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class UserService {
  private final UserRepository userRepository;
  private final AuditService auditService;
  private final ObjectMapper objectMapper;

  public UserService(UserRepository userRepository, AuditService auditService, ObjectMapper objectMapper) {
    this.userRepository = userRepository;
    this.auditService = auditService;
    this.objectMapper = objectMapper;
  }

  public UserResponse createUser(UserCreateRequest request) {
    User user = new User();
    user.setName(request.getName());
    user.setEmail(request.getEmail());
    User saved = userRepository.save(user);
    auditService.log("User", saved.getId().toString(), "CREATE", toJson(saved));
    return toResponse(saved);
  }

  public User getUserEntity(String id) {
    return userRepository.findById(java.util.UUID.fromString(id))
      .orElseThrow(() -> new NotFoundException("User not found"));
  }

  public UserResponse getUser(java.util.UUID id) {
    User user = userRepository.findById(id)
      .orElseThrow(() -> new NotFoundException("User not found"));
    return toResponse(user);
  }

  public Page<UserResponse> listUsers(Pageable pageable) {
    return userRepository.findAll(pageable).map(this::toResponse);
  }

  private UserResponse toResponse(User user) {
    UserResponse response = new UserResponse();
    response.setId(user.getId());
    response.setName(user.getName());
    response.setEmail(user.getEmail());
    response.setStatus(user.getStatus().name());
    response.setCreatedAt(user.getCreatedAt());
    return response;
  }

  private String toJson(Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (JsonProcessingException e) {
      return "{}";
    }
  }
}
