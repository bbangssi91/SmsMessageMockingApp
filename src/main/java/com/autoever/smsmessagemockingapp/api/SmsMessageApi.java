package com.autoever.smsmessagemockingapp.api;

import com.autoever.smsmessagemockingapp.dto.MessageRequestDto;
import com.autoever.smsmessagemockingapp.service.RateLimiterService;
import io.github.resilience4j.ratelimiter.RateLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.function.Supplier;

@Slf4j
@RequiredArgsConstructor
@RestController
public class SmsMessageApi {

    private final RateLimiterService rateLimiterService;
    @PostMapping(value = "/sms", produces = "application/x-www-form-urlencoded", consumes = "application/x-www-form-urlencoded")
    public ResponseEntity<String> sendSmsMessage(
            @RequestHeader(value = "API_KEY") String apiKey,
            @RequestParam("phone") String phone,
            @ModelAttribute MessageRequestDto request
    ) {

        if(apiKey == null || apiKey.isEmpty()) {
            log.error("API Key is required");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("resultCode=99");
        }

        log.info("SMS Message Received: {}, {}", phone, request.message());

        // 토큰별로 RateLimiter 가져오기
        RateLimiter rateLimiter = rateLimiterService.getRateLimiterForToken(apiKey);

        // RateLimiter 실행
        Supplier<ResponseEntity<String>> decoratedSupplier = RateLimiter
                .decorateSupplier(rateLimiter, () -> ResponseEntity.ok("resultCode=00"));

        try {
            // RateLimiter를 통과한 경우 메시지를 보냄
            return decoratedSupplier.get();
        } catch (Exception e) {
            log.warn("Rate limit exceeded for token: {}", apiKey);
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body("resultCode=99");
        }
    }
}