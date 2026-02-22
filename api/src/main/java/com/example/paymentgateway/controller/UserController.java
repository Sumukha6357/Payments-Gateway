package com.example.paymentgateway.controller;

import com.example.paymentgateway.dto.UserCreateRequest;
import com.example.paymentgateway.dto.UserResponse;
import com.example.paymentgateway.service.UserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserController {
  private final UserService userService;

  public UserController(UserService userService) {
    this.userService = userService;
  }

  @PostMapping
  public UserResponse create(@Valid @RequestBody UserCreateRequest request) {
    return userService.createUser(request);
  }

  @GetMapping("/{id}")
  public UserResponse get(@PathVariable UUID id) {
    return userService.getUser(id);
  }

  @GetMapping
  public List<UserResponse> list(Pageable pageable) {
    return userService.listUsers(pageable).getContent();
  }
}
