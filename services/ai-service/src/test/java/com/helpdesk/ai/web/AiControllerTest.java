package com.helpdesk.ai.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Every endpoint here now requires authentication (see SecurityConfig) - previously this
 * service had none at all, and every call was a real OpenAI request charged to the
 * project's API key.
 *
 * AiController calls chatClientBuilder.build() once, in its constructor, at bean creation
 * time - which happens once when the (cached) Spring context is first built, before any
 * @Test method runs. A plain @MockBean stubbed inside a test method is too late: the
 * controller singleton has already captured whatever build() returned the first time
 * (null, for an unstubbed mock), so the fluent .prompt()... chain would NPE regardless of
 * what's stubbed afterwards. RealChatClientConfig pre-stubs it as part of the bean
 * definition itself, before the controller ever asks for it.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(AiControllerTest.RealChatClientConfig.class)
class AiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private VectorStore vectorStore;

    // Stops OAuth2ResourceServerAutoConfiguration from resolving a real JwtDecoder against
    // the configured Keycloak issuer-uri during context startup - jwt() injects a
    // pre-authenticated principal directly and never decodes a real token.
    @MockBean
    private JwtDecoder jwtDecoder;

    @TestConfiguration
    static class RealChatClientConfig {
        @Bean
        ChatClient.Builder chatClientBuilder() {
            ChatClient chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
            when(chatClient.prompt().system(anyString()).user(anyString()).call().content())
                    .thenReturn("{\"sentiment\":\"NEGATIVE\",\"category\":\"ACCESS\"}");
            ChatClient.Builder builder = mock(ChatClient.Builder.class);
            when(builder.build()).thenReturn(chatClient);
            return builder;
        }
    }

    @Test
    void authenticatedRequestReachesTheAnalyzeEndpoint() throws Exception {
        String jsonPayload = "{\"description\": \"I cannot login to my account!\"}";

        mockMvc.perform(post("/api/ai/ticket/analyze")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_customer")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isOk());
    }

    @Test
    void unauthenticatedRequestIsRejected() throws Exception {
        String jsonPayload = "{\"description\": \"I cannot login to my account!\"}";

        mockMvc.perform(post("/api/ai/ticket/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void customerCannotIngestIntoTheVectorStore() throws Exception {
        String jsonPayload = "{\"id\": \"1\", \"title\": \"t\", \"content\": \"c\"}";

        mockMvc.perform(post("/api/ai/ingest")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_customer")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isForbidden());
    }

    @Test
    void agentCanIngestIntoTheVectorStore() throws Exception {
        String jsonPayload = "{\"id\": \"1\", \"title\": \"t\", \"content\": \"c\"}";

        mockMvc.perform(post("/api/ai/ingest")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_agent")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isOk());
    }
}
