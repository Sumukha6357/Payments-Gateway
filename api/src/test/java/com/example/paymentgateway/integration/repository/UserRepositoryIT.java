package com.example.paymentgateway.integration.repository;

import com.example.paymentgateway.domain.entity.User;
import com.example.paymentgateway.integration.support.IntegrationTestBase;
import com.example.paymentgateway.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

public class UserRepositoryIT extends IntegrationTestBase {

  @Autowired
  private UserRepository userRepository;

  @Test
  void shouldSaveAndFetchUser() {
    User user = new User();
    user.setEmail("test@example.com");
    user.setName("Test User");

    User saved = userRepository.save(user);

    assertThat(saved.getId()).isNotNull();

    var found = userRepository.findById(saved.getId());

    assertThat(found).isPresent();
    assertThat(found.get().getEmail()).isEqualTo("test@example.com");
  }
}

