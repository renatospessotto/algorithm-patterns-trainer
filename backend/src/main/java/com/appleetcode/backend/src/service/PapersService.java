package com.appleetcode.backend.src.service;

import com.appleetcode.backend.src.controller.dto.PaperDto;
import com.appleetcode.backend.src.entity.Paper;
import com.appleetcode.backend.src.entity.Topic;
import com.appleetcode.backend.src.repository.PaperRepository;
import com.appleetcode.backend.src.repository.TopicRepository;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.time.Year;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Feed de papers acadêmicos por tema (banco de dados, IA, ML tradicional
 * etc), independente do estudo de padrões. Cache de 14 dias por tema em
 * `papers`; se a API falhar, devolve o cache mesmo vencido -- só fica vazio
 * se nunca conseguiu buscar nada pro tema. Critério de "bem-sucedido" é
 * citation_count entre papers recentes (~3 anos) -- não existe fonte aberta
 * confiável de "papers premiados".
 */
@Service
public class PapersService {

    private static final Logger log = LoggerFactory.getLogger(PapersService.class);
    private static final int RECENT_YEARS_WINDOW = 3;
    private static final int FETCH_LIMIT = 20;
    private static final int RESULT_LIMIT = 10;

    private final TopicRepository topicRepository;
    private final PaperRepository paperRepository;
    private final RestClient restClient;
    private final int cacheTtlDays;

    public PapersService(
            TopicRepository topicRepository,
            PaperRepository paperRepository,
            @Value("${app.papers.semantic-scholar-base-url}") String baseUrl,
            @Value("${app.papers.cache-ttl-days}") int cacheTtlDays
    ) {
        this.topicRepository = topicRepository;
        this.paperRepository = paperRepository;
        this.cacheTtlDays = cacheTtlDays;
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("User-Agent", "Mozilla/5.0 (compatible; app-leetcode/1.0)")
                .build();
    }

    @Transactional
    public List<PaperDto> feed(String topicName) {
        List<Paper> papers;
        if (topicName == null || topicName.isBlank()) {
            for (Topic topic : topicRepository.findAll()) {
                refreshIfStale(topic);
            }
            papers = paperRepository.findAllByOrderByCitationCountDescCachedAtDesc();
        } else {
            Topic topic = topicRepository.findByNameIgnoreCase(topicName)
                    .orElseThrow(() -> new EntityNotFoundException("Tópico não encontrado: " + topicName));
            refreshIfStale(topic);
            papers = paperRepository.findByTopicIdOrderByCitationCountDescCachedAtDesc(topic.getId());
        }
        return papers.stream().map(PaperDto::from).toList();
    }

    private void refreshIfStale(Topic topic) {
        List<Paper> cached = paperRepository.findByTopicIdOrderByCitationCountDescCachedAtDesc(topic.getId());
        boolean stale = cached.isEmpty() || cached.get(0).getCachedAt().isBefore(Instant.now().minus(cacheTtlDays, ChronoUnit.DAYS));
        if (!stale) {
            return;
        }

        int currentYear = Year.now().getValue();
        String yearRange = (currentYear - RECENT_YEARS_WINDOW) + "-" + currentYear;

        SemanticScholarPaperDto.SearchResponse response;
        try {
            response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .queryParam("query", topic.getSearchQuery())
                            .queryParam("year", yearRange)
                            .queryParam("fields", "title,url,year,citationCount,venue,authors")
                            .queryParam("limit", FETCH_LIMIT)
                            .build())
                    .retrieve()
                    .body(SemanticScholarPaperDto.SearchResponse.class);
        } catch (Exception e) {
            log.warn("Falha ao buscar papers do Semantic Scholar pro tema '{}': {}", topic.getName(), e.getMessage());
            // API fora do ar ou rate-limited: mantém o cache existente (mesmo vencido) intacto.
            return;
        }

        List<SemanticScholarPaperDto> results = response == null || response.data() == null ? List.of() : response.data();

        // A busca simples do Semantic Scholar não garante ordenação por citação,
        // então ordena em memória -- citação recente é o proxy de "bem-sucedido".
        Map<String, Paper> deduped = new LinkedHashMap<>();
        results.stream()
                .filter(dto -> dto.url() != null && dto.title() != null)
                .sorted(Comparator.comparingInt((SemanticScholarPaperDto dto) -> dto.citationCount() == null ? 0 : dto.citationCount()).reversed())
                .limit(RESULT_LIMIT)
                .forEach(dto -> deduped.putIfAbsent(dto.url(), toPaper(topic, dto)));

        paperRepository.deleteAll(cached);
        for (Paper paper : deduped.values()) {
            paperRepository.findByUrl(paper.getUrl()).ifPresentOrElse(
                    existing -> { /* já pertence a outro tópico, mantém como está */ },
                    () -> paperRepository.save(paper)
            );
        }
    }

    private Paper toPaper(Topic topic, SemanticScholarPaperDto dto) {
        Paper paper = new Paper();
        paper.setTopic(topic);
        paper.setTitle(dto.title());
        paper.setUrl(dto.url());
        paper.setSource("semanticscholar");
        paper.setVenue(dto.venue());
        paper.setPublicationYear(dto.year());
        paper.setCitationCount(dto.citationCount());
        paper.setAuthors(dto.authors() == null ? null : dto.authors().stream()
                .map(SemanticScholarPaperDto.AuthorDto::name)
                .filter(n -> n != null && !n.isBlank())
                .reduce((a, b) -> a + ", " + b)
                .orElse(null));
        paper.setCachedAt(Instant.now());
        return paper;
    }
}
