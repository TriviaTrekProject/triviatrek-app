package com.main.triviatreckapp.service;

import org.junit.jupiter.api.BeforeEach;
import org.mockito.InjectMocks;
import org.springframework.test.util.ReflectionTestUtils;

public abstract class AbstractJwtServiceTest {

  @InjectMocks
  protected JwtService jwtService;

  @BeforeEach
  void initJwtService() {
    ReflectionTestUtils.setField(jwtService, "secretKey",
        "Zm9vYmFyMTIzIT8kQCVeJiooKS1fKz09XQ==");
    ReflectionTestUtils.setField(jwtService, "jwtExpiration", 1_000_000L);
  }
}