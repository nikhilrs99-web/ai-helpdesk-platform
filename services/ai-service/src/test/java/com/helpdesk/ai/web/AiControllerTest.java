package com.helpdesk.ai.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ChatClient.Builder chatClientBuilder;

    @MockBean
    private VectorStore vectorStore;

    @Test
    void testAnalyzeEndpointReturnsOk() throws Exception {
        String jsonPayload = "{\"description\": \"I cannot login to my account!\"}";
        
        // Given mocked beans, just testing context loads and routing is valid
        // Real testing would use WireMock for OpenAI API
        mockMvc.perform(post("/api/ai/ticket/analyze")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonPayload))
                .andExpect(status().isOk());
    }
}
