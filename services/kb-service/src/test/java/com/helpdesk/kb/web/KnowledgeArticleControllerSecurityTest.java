package com.helpdesk.kb.web;

import com.helpdesk.common.enums.TicketCategory;
import com.helpdesk.kb.domain.KnowledgeArticle;
import com.helpdesk.kb.repository.KnowledgeArticleRepository;
import com.helpdesk.kb.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * This service used to have zero authentication: anything that could reach its port
 * directly (any container on the same docker network, any pod in the same Kubernetes
 * cluster without a NetworkPolicy) could create, overwrite, or delete knowledge base
 * articles. Proves the fix actually gates each operation as intended: reads open to any
 * authenticated role, writes restricted to agent/admin - and that a customer token
 * specifically gets 403, not just "any non-agent gets rejected somehow".
 */
@WebMvcTest(KnowledgeArticleController.class)
@Import(SecurityConfig.class)
class KnowledgeArticleControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private KnowledgeArticleRepository repository;

    // Stops OAuth2ResourceServerAutoConfiguration from resolving a real JwtDecoder against
    // the configured Keycloak issuer-uri during context startup - jwt() injects a
    // pre-authenticated principal directly and never decodes a real token.
    @MockBean
    private JwtDecoder jwtDecoder;

    private String articlePayload() {
        return """
                {"title": "How to reset password", "body": "Click forgot password.", "category": "ACCESS"}
                """;
    }

    @Test
    void listingArticlesRequiresNoRoleBeyondBeingAuthenticated() throws Exception {
        when(repository.findAll(org.mockito.ArgumentMatchers.<org.springframework.data.domain.Pageable>any()))
                .thenReturn(org.springframework.data.domain.Page.empty());

        mockMvc.perform(get("/api/articles")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_customer"))))
                .andExpect(status().isOk());
    }

    @Test
    void listingArticlesWithNoTokenIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/articles"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void customerCannotCreateAnArticle() throws Exception {
        mockMvc.perform(post("/api/articles")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_customer")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(articlePayload()))
                .andExpect(status().isForbidden());
    }

    @Test
    void agentCanCreateAnArticle() throws Exception {
        when(repository.save(any(KnowledgeArticle.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(post("/api/articles")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_agent")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(articlePayload()))
                .andExpect(status().isCreated());
    }

    @Test
    void customerCannotDeleteAnArticle() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/articles/" + id)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_customer"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanDeleteAnArticle() throws Exception {
        UUID id = UUID.randomUUID();
        when(repository.existsById(id)).thenReturn(true);

        mockMvc.perform(delete("/api/articles/" + id)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_admin"))))
                .andExpect(status().isNoContent());
    }
}
