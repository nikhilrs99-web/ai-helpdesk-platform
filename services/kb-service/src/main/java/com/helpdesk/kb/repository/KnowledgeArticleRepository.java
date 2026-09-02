package com.helpdesk.kb.repository;

import com.helpdesk.kb.domain.KnowledgeArticle;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface KnowledgeArticleRepository extends JpaRepository<KnowledgeArticle, UUID> {

    @Query(value = "SELECT * FROM knowledge_article " +
                   "WHERE search_vector @@ plainto_tsquery('english', :query) " +
                   "ORDER BY ts_rank(search_vector, plainto_tsquery('english', :query)) DESC",
           countQuery = "SELECT count(*) FROM knowledge_article " +
                        "WHERE search_vector @@ plainto_tsquery('english', :query)",
           nativeQuery = true)
    Page<KnowledgeArticle> searchByKeyword(@Param("query") String query, Pageable pageable);
}
