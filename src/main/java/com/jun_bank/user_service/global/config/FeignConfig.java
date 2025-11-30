package com.jun_bank.user_service.global.config;

import com.jun_bank.user_service.global.feign.FeignErrorDecoder;
import com.jun_bank.user_service.global.feign.FeignRequestInterceptor;
import feign.Logger;
import feign.codec.ErrorDecoder;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Feign Client 설정
 */
@Configuration
@EnableFeignClients(basePackages = "com.jun_bank.user_service")
public class FeignConfig {

    /**
     * Feign 로깅 레벨
     * - NONE: 로깅 없음
     * - BASIC: 요청 메서드, URL, 응답 코드, 실행 시간
     * - HEADERS: BASIC + 헤더
     * - FULL: HEADERS + 요청/응답 바디
     */
    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.BASIC;
    }

    /**
     * Feign 에러 디코더
     */
    @Bean
    public ErrorDecoder errorDecoder() {
        return new FeignErrorDecoder();
    }

    /**
     * Feign 요청 인터셉터 (인증 헤더 전파)
     */
    @Bean
    public FeignRequestInterceptor feignRequestInterceptor() {
        return new FeignRequestInterceptor();
    }
}