package com.helpdesk.kb.web;

import com.helpdesk.kb.domain.KnowledgeArticle;
import com.helpdesk.kb.repository.KnowledgeArticleRepository;
import com.helpdesk.kb.web.dto.ArticleRequest;
import com.helpdesk.kb.web.dto.ArticleResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@RestController
@RequestMapping("/api/articles")
public class KnowledgeArticleController {

    private final KnowledgeArticleRepository repository;

    public KnowledgeArticleController(KnowledgeArticleRepository repository) {
        this.repository = repository;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ArticleResponse createArticle(@Valid @RequestBody ArticleRequest request) {
        KnowledgeArticle article = new KnowledgeArticle(request.getTitle(), request.getBody(), request.getCategory());
        return new ArticleResponse(repository.save(article));
    }

    @GetMapping("/{id}")
    @org.springframework.cache.annotation.Cacheable(value = "articles", key = "#id")
    public ArticleResponse getArticle(@PathVariable UUID id) {
        return repository.findById(id)
                .map(ArticleResponse::new)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Article not found"));
    }

    @GetMapping
    public Page<ArticleResponse> listArticles(Pageable pageable) {
        return repository.findAll(pageable).map(ArticleResponse::new);
    }

    @GetMapping("/search")
    public Page<ArticleResponse> searchArticles(@RequestParam String q, Pageable pageable) {
        if (q == null || q.trim().isEmpty()) {
            return listArticles(pageable);
        }
        return repository.searchByKeyword(q, pageable).map(ArticleResponse::new);
    }

    @PutMapping("/{id}")
    @org.springframework.cache.annotation.CachePut(value = "articles", key = "#id")
    public ArticleResponse updateArticle(@PathVariable UUID id, @Valid @RequestBody ArticleRequest request) {
        KnowledgeArticle article = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Article not found"));
        article.setTitle(request.getTitle());
        article.setBody(request.getBody());
        article.setCategory(request.getCategory());
        return new ArticleResponse(repository.save(article));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @org.springframework.cache.annotation.CacheEvict(value = "articles", key = "#id")
    public void deleteArticle(@PathVariable UUID id) {
        if (!repository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Article not found");
        }
        repository.deleteById(id);
    }
}
