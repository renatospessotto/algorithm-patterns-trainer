# Treinador de Padrões de Algoritmos

*[Read in English](README.md)*

Um **treinador de repetição espaçada para padrões de algoritmos de entrevista**, rodando na sua máquina. Em vez de resolver problemas aleatórios, ele ensina **14 macro-padrões** (HashMap, Two Pointers, Sliding Window, …, Trie/Bits) por uma sequência curada de 46 problemas core. Depois mantém esses padrões frescos com uma **fila de revisão que respeita sua capacidade**: 2 exercícios por dia, sempre os que você tem mais chance de ter esquecido.

<!-- Screenshots: adicione imagens em docs/assets/ e referencie aqui. -->

## Destaques

- **Sem data de revisão, sem acúmulo.** Cada problema tem uma urgência pela curva de esquecimento, `1 − e^(−Δt/estabilidade)`, calculada na hora. A estabilidade se adapta a uma média móvel exponencial dos seus resultados e ao seu tempo em relação a uma meta por dificuldade. Veja [docs/algorithm.md](docs/algorithm.md).
- **Currículo em fases.** A fase de introdução percorre a sequência fixa. Depois vem a consolidação: só revisão, completada com problemas do seu padrão *mais fraco*. Por fim, a fase livre para explorar. Veja [docs/features/daily-session.md](docs/features/daily-session.md).
- **Pipeline de dados reproduzível, sem dados no git.** Um ETL pequeno em Python cruza dois datasets públicos (fixados por commit) e mapeia as tags oficiais do LeetCode para os 14 padrões. Ele roda dentro do build do Docker. Veja [docs/etl.md](docs/etl.md).
- **Um único container em produção.** O build do Angular vai dentro do jar do Spring Boot, e o Flyway cuida do schema e da importação do seed.

## Telas

| Tela | O que faz | Doc |
|---|---|---|
| **Hoje** | 2 exercícios fixos do dia, cronômetro, registro do resultado e barra opcional de revisão | [daily-session](docs/features/daily-session.md), [attempt-recording](docs/features/attempt-recording.md) |
| **Explorar** | Problema aleatório entre cerca de 2.900, filtrado por padrão, priorizando os nunca tentados | [explore](docs/features/explore.md) |
| **Stats** | Heatmap de consistência, sequências, tempo médio por dificuldade e padrões fracos/fortes | [stats](docs/features/stats.md), [patterns-progress](docs/features/patterns-progress.md) |
| **Histórico** | Log completo de tentativas, com filtro por dificuldade e por dia | [history](docs/features/history.md) |
| **Leitura** | Papers recentes e bem citados por tema (Semantic Scholar, com cache de 14 dias) | [reading-feed](docs/features/reading-feed.md) |

A documentação técnica em `docs/` está em inglês.

## Como rodar

Requisitos: Docker com Compose. O primeiro build precisa de internet para baixar os datasets públicos.

```bash
git clone <este repo> && cd <repo>
cp .env.example .env          # opcional, todo valor tem default
docker compose up --build -d
```

Abra <http://localhost:8080>. Seu progresso fica no volume Docker `pgdata`; `docker compose down -v` apaga tudo.

### Desenvolvimento

```bash
docker compose up -d db                  # só o Postgres
scripts/seed.sh                          # gera o seed localmente
cd backend && SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5435/app_leetcode mvn spring-boot:run
cd frontend && npm install && npx ng serve   # http://localhost:4200 (proxy de /api)
```

### Testes

```bash
cd backend && mvn test
cd frontend && npx ng build
python3 -m unittest discover -s tools/etl
```

## Stack

Java 21, Spring Boot 3.3, Flyway, PostgreSQL 16, JUnit 5 · Angular 18 (standalone), Angular Material · ETL em Python (só biblioteca padrão) · Dockerfile multi-stage, Docker Compose.

Mais detalhes em [docs/architecture.md](docs/architecture.md). Para agentes de IA, comece por [`CLAUDE.md`](CLAUDE.md).

## Dados e licença

O código está sob a [licença MIT](LICENSE). **Este repositório não contém dados de problemas.** O catálogo é gerado no build a partir de [`neenza/leetcode-problems`](https://github.com/neenza/leetcode-problems) e [`zerotrac/leetcode_problem_rating`](https://github.com/zerotrac/leetcode_problem_rating); confira as licenças deles. LeetCode é marca registrada do seu dono; este projeto não tem afiliação com o LeetCode.
