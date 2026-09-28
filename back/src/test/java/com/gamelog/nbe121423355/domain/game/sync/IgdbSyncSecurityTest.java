package com.gamelog.nbe121423355.domain.game.sync;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class IgdbSyncSecurityTest {
    @Autowired MockMvc mvc;

    @Test
    void anonymousCannotReadOrStart() throws Exception {
        mvc.perform(get("/api/v1/admin/igdb-sync")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/admin/igdb-sync")).andExpect(status().isUnauthorized());
    }

    @Test
    void ordinaryUsersCannotReadOrStart() throws Exception {
        mvc.perform(get("/api/v1/admin/igdb-sync").with(user("user").roles("USER"))).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/admin/igdb-sync").with(user("user").roles("USER"))).andExpect(status().isForbidden());
    }

    @Test
    void administratorCanReadStatus() throws Exception {
        mvc.perform(get("/api/v1/admin/igdb-sync").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("IDLE"));
    }
}
