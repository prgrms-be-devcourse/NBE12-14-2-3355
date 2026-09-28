package com.gamelog.nbe121423355.global.exceptionHandler;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Spring MVC가 던지는 클라이언트 오류(4xx)가 500으로 처리되지 않는지 확인
// 인증 없이 호출 가능한(permitAll) 경로로 테스트
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("존재하지 않는 경로 요청 시 404")
    void noResource_404() throws Exception {
        mockMvc.perform(get("/api/v1/games/1/not-exist-path"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.resultCode").value("404-0"));
    }

    @Test
    @DisplayName("PathVariable 타입 불일치 시 400")
    void pathVariableTypeMismatch_400() throws Exception {
        mockMvc.perform(get("/api/v1/games/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("400-0"));
    }

    @Test
    @DisplayName("RequestParam 타입 불일치 시 400")
    void requestParamTypeMismatch_400() throws Exception {
        mockMvc.perform(get("/api/v1/reviews/popular").param("size", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("400-0"));
    }

    @Test
    @DisplayName("지원하지 않는 HTTP 메서드 요청 시 405")
    void methodNotSupported_405() throws Exception {
        mockMvc.perform(delete("/api/v1/users/signup"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.resultCode").value("405-0"));
    }
}
