package db.migration;

import com.appleetcode.backend.seed.SeedDtos.PatternDto;
import com.appleetcode.backend.seed.SeedDtos.PatternRefDto;
import com.appleetcode.backend.seed.SeedDtos.ProblemDto;
import com.appleetcode.backend.seed.SeedDtos.SeedProblemsFile;
import com.appleetcode.backend.seed.SeedDtos.SeedTopicsFile;
import com.appleetcode.backend.seed.SeedDtos.TopicDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Types;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Popula patterns/problems/problem_patterns/plan_items/topics a partir de
 * seed_problems.json / seed_topics.json.
 *
 * Isso PRECISA rodar como migration Flyway (Java-based), não como um
 * ApplicationRunner do Spring: o Tomcat embutido já aceita conexões antes
 * dos ApplicationRunner beans executarem (SpringApplication.run() só chama
 * callRunners() depois que refreshContext() -- que já sobe o servidor web --
 * termina). Uma versão anterior disso era um ApplicationRunner, e uma
 * requisição a /today chegando nesse intervalo via com o banco ainda vazio
 * fazia o SessionPlannerService concluir (errado) que não havia mais nada a
 * introduzir e travar a fase em CONSOLIDACAO permanentemente. Migrations
 * Flyway rodam durante a inicialização do DataSource, bem antes do servidor
 * web subir, então essa janela de corrida não existe aqui.
 */
public class V2__Seed_data extends BaseJavaMigration {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();

        SeedProblemsFile problemsFile = readJson("/seed/seed_problems.json", SeedProblemsFile.class);
        SeedTopicsFile topicsFile = readJson("/seed/seed_topics.json", SeedTopicsFile.class);

        Map<String, Long> patternIdBySlug = insertPatterns(connection, problemsFile.patterns());
        Map<String, Long> problemIdBySlug = insertProblems(connection, problemsFile.problems());
        insertProblemPatterns(connection, problemsFile.problems(), problemIdBySlug, patternIdBySlug);
        insertPlanItems(connection, problemsFile.problems(), problemsFile.patterns(), problemIdBySlug, patternIdBySlug);
        insertTopics(connection, topicsFile.topics());
    }

    private <T> T readJson(String classpathLocation, Class<T> type) throws Exception {
        try (InputStream in = getClass().getResourceAsStream(classpathLocation)) {
            if (in == null) {
                // O seed não é versionado (ver docs/etl.md). Falhar aqui desfaz a
                // migration (DDL transacional no Postgres), então o próximo boot
                // tenta de novo em vez de deixar o banco vazio pra sempre.
                throw new IllegalStateException("Seed não encontrado no classpath: " + classpathLocation
                        + ". Gere com `scripts/seed.sh` (dev local) ou suba via `docker compose up --build`,"
                        + " que roda o ETL no build.");
            }
            return objectMapper.readValue(in, type);
        }
    }

    private Map<String, Long> insertPatterns(Connection conn, List<PatternDto> patterns) throws Exception {
        Map<String, Long> idsBySlug = new HashMap<>();
        String sql = "INSERT INTO patterns (name, slug, description, order_in_plan) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            for (PatternDto p : patterns) {
                ps.setString(1, p.name());
                ps.setString(2, p.slug());
                ps.setString(3, p.description());
                if (p.orderInPlan() != null) {
                    ps.setInt(4, p.orderInPlan());
                } else {
                    ps.setNull(4, Types.INTEGER);
                }
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    keys.next();
                    idsBySlug.put(p.slug(), keys.getLong(1));
                }
            }
        }
        return idsBySlug;
    }

    private Map<String, Long> insertProblems(Connection conn, List<ProblemDto> problems) throws Exception {
        Map<String, Long> idsBySlug = new HashMap<>();
        String sql = "INSERT INTO problems (external_slug, title, leetcode_url, difficulty_label, difficulty_rating, is_core) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            for (ProblemDto p : problems) {
                ps.setString(1, p.externalSlug());
                ps.setString(2, p.title());
                ps.setString(3, p.leetcodeUrl());
                ps.setString(4, p.difficultyLabel());
                if (p.difficultyRating() != null) {
                    ps.setDouble(5, p.difficultyRating());
                } else {
                    ps.setNull(5, Types.DOUBLE);
                }
                ps.setBoolean(6, p.isCore());
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    keys.next();
                    idsBySlug.put(p.externalSlug(), keys.getLong(1));
                }
            }
        }
        return idsBySlug;
    }

    private void insertProblemPatterns(
            Connection conn, List<ProblemDto> problems, Map<String, Long> problemIds, Map<String, Long> patternIds
    ) throws Exception {
        String sql = "INSERT INTO problem_patterns (problem_id, pattern_id, is_primary) VALUES (?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (ProblemDto p : problems) {
                Long problemId = problemIds.get(p.externalSlug());
                for (PatternRefDto ref : p.patterns()) {
                    Long patternId = patternIds.get(ref.slug());
                    if (patternId == null) {
                        continue;
                    }
                    ps.setLong(1, problemId);
                    ps.setLong(2, patternId);
                    ps.setBoolean(3, ref.isPrimary());
                    ps.addBatch();
                }
            }
            ps.executeBatch();
        }
    }

    /**
     * Monta a sequência fixa da fase 1: ordena os problemas core por
     * (order_in_plan do padrão, core_order dentro do padrão) e atribui
     * day_number sequencial a partir de 1.
     */
    private void insertPlanItems(
            Connection conn, List<ProblemDto> problems, List<PatternDto> patterns,
            Map<String, Long> problemIds, Map<String, Long> patternIds
    ) throws Exception {
        Map<String, Integer> orderInPlanBySlug = new HashMap<>();
        for (PatternDto pattern : patterns) {
            if (pattern.orderInPlan() != null) {
                orderInPlanBySlug.put(pattern.slug(), pattern.orderInPlan());
            }
        }

        List<ProblemDto> coreSorted = problems.stream()
                .filter(ProblemDto::isCore)
                .sorted(Comparator
                        .<ProblemDto>comparingInt(d -> orderInPlanBySlug.getOrDefault(d.corePatternSlug(), Integer.MAX_VALUE))
                        .thenComparingInt(ProblemDto::coreOrder))
                .toList();

        String sql = "INSERT INTO plan_items (day_number, pattern_id, problem_id, slot) VALUES (?, ?, ?, 'novo')";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            int dayNumber = 1;
            for (ProblemDto p : coreSorted) {
                Long patternId = patternIds.get(p.corePatternSlug());
                Long problemId = problemIds.get(p.externalSlug());
                ps.setInt(1, dayNumber++);
                ps.setLong(2, patternId);
                ps.setLong(3, problemId);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private void insertTopics(Connection conn, List<TopicDto> topics) throws Exception {
        String sql = "INSERT INTO topics (name, search_query) VALUES (?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (TopicDto t : topics) {
                ps.setString(1, t.name());
                ps.setString(2, t.searchQuery());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }
}
