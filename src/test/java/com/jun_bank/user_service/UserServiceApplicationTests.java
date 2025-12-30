package com.jun_bank.user_service;

import com.jun_bank.user_service.domain.user.infrastructure.client.AuthServiceClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * 애플리케이션 컨텍스트 로드 테스트
 */
@SpringBootTest
@Import(TestKafkaConfig.class)
class UserServiceApplicationTests {

  // Feign Client Mock (외부 서비스)
  @MockitoBean
  private AuthServiceClient authServiceClient;

  @Test
  void contextLoads() {
    // Spring Context가 정상적으로 로드되는지 확인
  }
}