package com.helpdesk.kb.repository;

import com.helpdesk.common.enums.TicketCategory;
import com.helpdesk.kb.domain.KnowledgeArticle;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class KnowledgeArticleRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            // Testcontainers will pull standard postgres for simple integration tests,
            // later we can use pgvector/pgvector:pg16 if semantic search functions are needed in tests.
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private KnowledgeArticleRepository repository;

    @Test
    void searchByKeyword_shouldReturnMatchingArticles() {
        // Given
        KnowledgeArticle article1 = new KnowledgeArticle("How to reset password", "Go to settings and click reset.", TicketCategory.ACCESS);
        KnowledgeArticle article2 = new KnowledgeArticle("Billing issues", "Check your invoice.", TicketCategory.BILLING);
        repository.save(article1);
        repository.save(article2);

        // When
        Page<KnowledgeArticle> results = repository.searchByKeyword("password", PageRequest.of(0, 10));

        // Then
        assertThat(results.getContent()).hasSize(1);
        assertThat(results.getContent().get(0).getTitle()).isEqualTo("How to reset password");
    }
}
