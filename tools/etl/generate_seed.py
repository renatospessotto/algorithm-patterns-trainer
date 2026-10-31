#!/usr/bin/env python3
"""
ETL do seed: gera seed_problems.json e seed_topics.json a partir de datasets
publicos. Nenhum dado gerado e versionado -- o resultado so existe localmente
ou dentro da imagem Docker (ver docs/etl.md).

  Extract   baixa (e cacheia em .cache/) as duas fontes, fixadas por commit:
              - neenza/leetcode-problems (merged_problems.json): titulo, slug,
                dificuldade, tags oficiais
              - zerotrac/leetcode_problem_rating (ratings.txt): rating
                numerico continuo (estilo Elo)
  Transform cruza por slug, mapeia tags oficiais -> 14 macro-padroes, marca
            os problemas core da fase 1
  Load      escreve os JSONs que a migration Flyway V2__Seed_data importa

Uso:
    python3 tools/etl/generate_seed.py                 # escreve em backend/src/main/resources/seed
    python3 tools/etl/generate_seed.py --out /seed     # usado pelo Dockerfile
    python3 tools/etl/generate_seed.py --offline       # falha se o cache nao existir

So usa a biblioteca padrao.
"""
import argparse
import json
import os
import sys
import urllib.request

HERE = os.path.dirname(os.path.abspath(__file__))
REPO_ROOT = os.path.normpath(os.path.join(HERE, "..", ".."))
DEFAULT_OUTPUT_DIR = os.path.join(REPO_ROOT, "backend", "src", "main", "resources", "seed")
DEFAULT_CACHE_DIR = os.path.join(HERE, ".cache")

# Fontes fixadas por commit pra o seed ser reproduzivel: o mesmo build gera o
# mesmo arquivo. Pra atualizar, troque o SHA (o nome do cache inclui o SHA,
# entao o arquivo novo e baixado sozinho).
PROBLEMS_SOURCE = {
    "url": "https://raw.githubusercontent.com/neenza/leetcode-problems/{sha}/merged_problems.json",
    "sha": "16c26d1bcfdc17a0290f7083e86c4e9387b2e248",
    "cache_name": "merged_problems-{sha}.json",
}
RATINGS_SOURCE = {
    "url": "https://raw.githubusercontent.com/zerotrac/leetcode_problem_rating/{sha}/ratings.txt",
    "sha": "cb2fb2fb2ecdd1b61e80ae5e0f7a4ffffcaf8421",
    "cache_name": "ratings-{sha}.txt",
}

# ---------------------------------------------------------------------------
# 1. Os 14 macro-padroes (definidos pelo usuario), na ordem didatica da fase 1.
# ---------------------------------------------------------------------------
PATTERNS = [
    {
        "slug": "hashmap-set",
        "name": "HashMap / Set",
        "order_in_plan": 1,
        "description": (
            "Trocar busca linear por acesso O(1), guardando o que ja foi visto pra "
            "consultar depois. Sinal: a pergunta pede complemento, duplicata, ou "
            "agrupamento por alguma chave derivada do elemento."
        ),
    },
    {
        "slug": "two-pointers",
        "name": "Two Pointers",
        "order_in_plan": 2,
        "description": (
            "Dois ponteiros percorrendo a estrutura (geralmente array ordenado ou "
            "string) de pontas opostas ou na mesma direcao, evitando um laco "
            "aninhado. Sinal: a pergunta pede par, tripla ou substring com alguma "
            "soma ou condicao, e o array ja esta ordenado ou pode ser ordenado sem "
            "perder informacao."
        ),
    },
    {
        "slug": "sliding-window",
        "name": "Sliding Window",
        "order_in_plan": 3,
        "description": (
            "Uma janela que expande e contrai sobre um array ou string, mantendo "
            "alguma invariante (soma, contagem de caracteres, tamanho). Sinal: a "
            "pergunta fala em substring, subarray contiguo, ou 'menor/maior janela "
            "que satisfaz X'."
        ),
    },
    {
        "slug": "fast-slow-pointers-linked-list",
        "name": "Fast/Slow Pointers em Linked List",
        "order_in_plan": 4,
        "description": (
            "Dois ponteiros andando em velocidades diferentes na lista encadeada, "
            "usados pra detectar ciclo, achar o meio, ou inverter a lista in-place. "
            "Sinal: qualquer manipulacao de linked list sem poder usar array "
            "auxiliar."
        ),
    },
    {
        "slug": "binary-search-on-answer",
        "name": "Binary Search on Answer",
        "order_in_plan": 5,
        "description": (
            "Busca binaria nao sobre um indice, mas sobre o espaco de respostas "
            "possiveis, testando se um valor candidato satisfaz a condicao do "
            "problema. Sinal: array ordenado (ou rotacionado) com busca por valor, "
            "ou pergunta do tipo 'menor/maior valor que satisfaz uma condicao "
            "monotonica'."
        ),
    },
    {
        "slug": "intervals",
        "name": "Intervals",
        "order_in_plan": 6,
        "description": (
            "Problemas sobre intervalos numericos que precisam ser ordenados, "
            "mesclados ou comparados por sobreposicao. Sinal: a entrada e uma lista "
            "de pares (inicio, fim) e a pergunta fala em sobreposicao, merge, ou "
            "insercao de novo intervalo."
        ),
    },
    {
        "slug": "monotonic-stack",
        "name": "Monotonic Stack",
        "order_in_plan": 7,
        "description": (
            "Pilha que mantem seus elementos sempre crescentes ou decrescentes, "
            "usada pra achar o proximo elemento maior/menor de forma linear. Sinal: "
            "pergunta pede 'proximo elemento maior', 'quantos dias ate subir a "
            "temperatura', ou maior retangulo num histograma."
        ),
    },
    {
        "slug": "heap-top-k",
        "name": "Heap / Top K",
        "order_in_plan": 8,
        "description": (
            "Manter os K maiores ou menores elementos vistos ate agora sem "
            "reordenar tudo a cada passo. Sinal: pergunta pede os 'K mais "
            "frequentes', 'K maior elemento', ou uma estrutura tipo cache com "
            "expiracao por uso."
        ),
    },
    {
        "slug": "dfs-bfs-graphs-matrices",
        "name": "DFS/BFS em Grafos e Matrizes",
        "order_in_plan": 9,
        "description": (
            "Percorrer grafo ou grid explorando vizinhos, contando componentes, ou "
            "verificando alcancabilidade. Sinal: menciona grid, ilhas, labirinto, "
            "ou grafo explicito com 'quantos grupos' ou 'existe caminho'."
        ),
    },
    {
        "slug": "topological-sort-union-find",
        "name": "Topological Sort / Union-Find",
        "order_in_plan": 10,
        "description": (
            "Ordenar nos respeitando dependencias, ou agrupar elementos que estao "
            "conectados transitivamente. Sinal: menciona pre-requisitos, ordem de "
            "execucao, ou pergunta se dois elementos pertencem ao mesmo grupo."
        ),
    },
    {
        "slug": "backtracking",
        "name": "Backtracking",
        "order_in_plan": 11,
        "description": (
            "Explorar todas as combinacoes possiveis construindo e desfazendo "
            "escolhas recursivamente, cortando ramos invalidos cedo. Sinal: "
            "pergunta pede 'todas as combinacoes/permutacoes/subconjuntos "
            "possiveis' ou resolver um tabuleiro tipo sudoku/palavras cruzadas."
        ),
    },
    {
        "slug": "dp-1d",
        "name": "DP 1D",
        "order_in_plan": 12,
        "description": (
            "Programacao dinamica onde o estado depende de um indice so, "
            "normalmente guardando o resultado dos passos anteriores num vetor ou "
            "so nas ultimas duas posicoes. Sinal: pergunta pede numero minimo/"
            "maximo de jeitos de chegar em algum resultado, com decisoes "
            "sequenciais."
        ),
    },
    {
        "slug": "dp-2d-strings",
        "name": "DP 2D / Strings",
        "order_in_plan": 13,
        "description": (
            "Igual ao anterior, mas o estado depende de dois indices, comum em "
            "comparacao de duas sequencias. Sinal: envolve duas strings ou dois "
            "arrays sendo comparados posicao a posicao."
        ),
    },
    {
        "slug": "trie-bits",
        "name": "Trie / Bits",
        "order_in_plan": 14,
        "description": (
            "Trie pra busca eficiente de prefixos em conjunto de palavras, e "
            "manipulacao bit a bit pra achar padroes numericos sem gastar memoria "
            "extra. Sinal: busca de palavras por prefixo, ou pergunta que menciona "
            "XOR, complemento binario, ou 'elemento que aparece numero impar de "
            "vezes'."
        ),
    },
]
PATTERN_SLUGS = {p["slug"] for p in PATTERNS}

# ---------------------------------------------------------------------------
# 2. Problemas core da fase 1 (sequencia fixa), na ordem em que aparecem
#    dentro de cada padrao. `number-of-connected-components-in-an-undirected-graph`
#    (LC 323) e premium/paywalled no dataset e foi trocado por
#    `number-of-provinces` (LC 547), que cobre o mesmo padrao (union-find /
#    componentes conexos) e e gratuito.
# ---------------------------------------------------------------------------
CORE_PROBLEMS_BY_PATTERN = {
    "hashmap-set": ["two-sum", "group-anagrams", "contains-duplicate"],
    "two-pointers": ["valid-palindrome", "container-with-most-water", "3sum"],
    "sliding-window": [
        "best-time-to-buy-and-sell-stock",
        "longest-substring-without-repeating-characters",
        "minimum-window-substring",
    ],
    "fast-slow-pointers-linked-list": [
        "reverse-linked-list",
        "linked-list-cycle",
        "merge-two-sorted-lists",
        "reorder-list",
    ],
    "binary-search-on-answer": [
        "binary-search",
        "search-in-rotated-sorted-array",
        "koko-eating-bananas",
    ],
    "intervals": ["merge-intervals", "insert-interval", "non-overlapping-intervals"],
    "monotonic-stack": [
        "valid-parentheses",
        "daily-temperatures",
        "largest-rectangle-in-histogram",
    ],
    "heap-top-k": [
        "kth-largest-element-in-an-array",
        "top-k-frequent-elements",
        "find-median-from-data-stream",
    ],
    "dfs-bfs-graphs-matrices": [
        "number-of-islands",
        "clone-graph",
        "course-schedule",
        "rotting-oranges",
    ],
    "topological-sort-union-find": [
        "course-schedule-ii",
        "number-of-provinces",
        "redundant-connection",
    ],
    "backtracking": ["subsets", "permutations", "combination-sum", "word-search"],
    "dp-1d": [
        "climbing-stairs",
        "house-robber",
        "coin-change",
        "longest-increasing-subsequence",
    ],
    "dp-2d-strings": ["longest-common-subsequence", "edit-distance", "unique-paths"],
    "trie-bits": ["implement-trie-prefix-tree", "single-number", "word-search-ii"],
}
CORE_SLUGS = {
    slug for slugs in CORE_PROBLEMS_BY_PATTERN.values() for slug in slugs
}

# ---------------------------------------------------------------------------
# 3. Mapeamento tags oficiais do LeetCode -> macro-padrao.
#    Ordem = prioridade de especificidade: a primeira tag da lista que o
#    problema tiver define `is_primary=true`; as demais batidas viram
#    patterns secundarios (problema pode cobrir mais de um padrao).
#    Tags genericas demais (Array, Math, String, Sorting, Design, Greedy,
#    Simulation, etc.) ficam de fora de proposito -- aparecem em quase tudo
#    e nao identificam um macro-padrao especifico (ver docs/spec/original-spec.md secao 2).
# ---------------------------------------------------------------------------
TAG_TO_PATTERN_PRIORITY = [
    ("Trie", "trie-bits"),
    ("Bit Manipulation", "trie-bits"),
    ("Bitmask", "trie-bits"),
    ("Backtracking", "backtracking"),
    ("Sliding Window", "sliding-window"),
    ("Monotonic Queue", "sliding-window"),
    ("Two Pointers", "two-pointers"),
    ("Monotonic Stack", "monotonic-stack"),
    ("Stack", "monotonic-stack"),
    ("Heap (Priority Queue)", "heap-top-k"),
    ("Quickselect", "heap-top-k"),
    ("Topological Sort", "topological-sort-union-find"),
    ("Union Find", "topological-sort-union-find"),
    ("Strongly Connected Component", "topological-sort-union-find"),
    ("Minimum Spanning Tree", "topological-sort-union-find"),
    ("Binary Search", "binary-search-on-answer"),
    ("Shortest Path", "dfs-bfs-graphs-matrices"),
    ("Depth-First Search", "dfs-bfs-graphs-matrices"),
    ("Breadth-First Search", "dfs-bfs-graphs-matrices"),
    ("Graph", "dfs-bfs-graphs-matrices"),
    ("Matrix", "dfs-bfs-graphs-matrices"),
    ("Binary Tree", "dfs-bfs-graphs-matrices"),
    ("Binary Search Tree", "dfs-bfs-graphs-matrices"),
    ("Tree", "dfs-bfs-graphs-matrices"),
    ("Linked List", "fast-slow-pointers-linked-list"),
    ("Doubly-Linked List", "fast-slow-pointers-linked-list"),
    ("Hash Table", "hashmap-set"),
    ("Hash Function", "hashmap-set"),
    ("Dynamic Programming", "dp-1d"),  # refinado abaixo p/ dp-2d-strings
]

# Tags que, combinadas com "Dynamic Programming", indicam estado de 2
# indices (string/matriz) em vez de 1D.
DP_2D_HINT_TAGS = {"String", "Matrix"}

# O LeetCode nao tem uma tag oficial "Intervals" (esses problemas vem so como
# Array/Sorting/Greedy, tags genericas demais pra mapear sozinhas). Por isso
# esse padrao usa uma lista curada de slugs conhecidos em vez de tag ->
# padrao, alem dos 3 problemas core.
EXTRA_POOL_SLUGS_BY_PATTERN = {
    "intervals": [
        "interval-list-intersections",
        "my-calendar-i",
        "my-calendar-ii",
        "my-calendar-iii",
        "minimum-number-of-arrows-to-burst-balloons",
        "car-pooling",
        "remove-covered-intervals",
        "partition-labels",
        "range-module",
        "data-stream-as-disjoint-intervals",
        "falling-squares",
        "video-stitching",
        "maximum-length-of-pair-chain",
    ],
}


# ---------------------------------------------------------------------------
# Extract
# ---------------------------------------------------------------------------
def fetch_cached(source: dict, cache_dir: str, offline: bool) -> str:
    """Devolve o caminho local da fonte, baixando so se ainda nao estiver no cache."""
    path = os.path.join(cache_dir, source["cache_name"].format(sha=source["sha"]))
    if os.path.exists(path):
        print(f"Usando cache local: {path}")
        return path
    if offline:
        raise SystemExit(f"--offline: cache ausente em {path}")
    url = source["url"].format(sha=source["sha"])
    print(f"Baixando {url} ...")
    os.makedirs(cache_dir, exist_ok=True)
    with urllib.request.urlopen(url, timeout=120) as resp:
        data = resp.read()
    tmp_path = path + ".part"
    with open(tmp_path, "wb") as f:
        f.write(data)
    os.replace(tmp_path, path)
    return path


def extract(cache_dir: str, offline: bool) -> tuple[list[dict], dict]:
    problems_path = fetch_cached(PROBLEMS_SOURCE, cache_dir, offline)
    ratings_path = fetch_cached(RATINGS_SOURCE, cache_dir, offline)
    with open(problems_path, encoding="utf-8") as f:
        questions = json.load(f)["questions"]
    return questions, load_ratings(ratings_path)


def load_ratings(path: str) -> dict:
    ratings = {}
    with open(path, encoding="utf-8") as f:
        header = f.readline()  # Rating\tID\tTitle\tTitle ZH\tTitle Slug\t...
        cols = header.rstrip("\n").split("\t")
        rating_idx = cols.index("Rating")
        slug_idx = cols.index("Title Slug")
        for line in f:
            parts = line.rstrip("\n").split("\t")
            if len(parts) <= max(rating_idx, slug_idx):
                continue
            slug = parts[slug_idx].strip()
            try:
                rating = round(float(parts[rating_idx]), 2)
            except ValueError:
                continue
            ratings[slug] = rating
    return ratings


def classify_patterns(topics: list[str]) -> list[dict]:
    """Retorna lista de {"slug", "is_primary"} para os padroes batidos pelas tags."""
    topic_set = set(topics)
    matched_slugs: list[str] = []
    for tag, pattern_slug in TAG_TO_PATTERN_PRIORITY:
        if tag in topic_set and pattern_slug not in matched_slugs:
            matched_slugs.append(pattern_slug)

    # Refina Dynamic Programming: se bateu dp-1d mas o problema tambem tem
    # sinal de string/matriz, promove pra dp-2d-strings (mais especifico).
    if "dp-1d" in matched_slugs and topic_set & DP_2D_HINT_TAGS:
        matched_slugs = [
            "dp-2d-strings" if s == "dp-1d" else s for s in matched_slugs
        ]

    if not matched_slugs:
        return []
    return [
        {"slug": slug, "is_primary": (i == 0)}
        for i, slug in enumerate(matched_slugs)
    ]


def slugify_url(slug: str) -> str:
    return f"https://leetcode.com/problems/{slug}/"


# ---------------------------------------------------------------------------
# Transform
# ---------------------------------------------------------------------------
def transform(questions: list[dict], ratings: dict, require_all_core: bool = True) -> list[dict]:
    """Cruza problemas e ratings por slug e classifica cada problema nos padroes."""
    core_order_by_pattern = {
        pattern_slug: {slug: idx for idx, slug in enumerate(slugs)}
        for pattern_slug, slugs in CORE_PROBLEMS_BY_PATTERN.items()
    }

    problems = []
    seen_slugs = set()
    missing_core = set(CORE_SLUGS)

    for q in questions:
        slug = q.get("problem_slug")
        if not slug or slug in seen_slugs:
            continue
        seen_slugs.add(slug)

        topics = q.get("topics", [])
        patterns = classify_patterns(topics)
        for pattern_slug, extra_slugs in EXTRA_POOL_SLUGS_BY_PATTERN.items():
            if slug in extra_slugs and not any(p["slug"] == pattern_slug for p in patterns):
                patterns.append({"slug": pattern_slug, "is_primary": not patterns})
        is_core = slug in CORE_SLUGS
        if is_core:
            missing_core.discard(slug)
            # garante que o problema core tenha, no minimo, o padrao em que
            # foi listado manualmente (mesmo se as tags oficiais nao bateram).
            for pattern_slug, order_map in core_order_by_pattern.items():
                if slug in order_map:
                    if not any(p["slug"] == pattern_slug for p in patterns):
                        patterns.insert(0, {"slug": pattern_slug, "is_primary": True})
                        for p in patterns[1:]:
                            p["is_primary"] = False

        entry = {
            "external_slug": slug,
            "title": q.get("title", slug),
            "leetcode_url": slugify_url(slug),
            "difficulty_label": q.get("difficulty", "Medium"),
            "difficulty_rating": ratings.get(slug),
            "is_core": is_core,
            "patterns": patterns,
        }
        if is_core:
            for pattern_slug, order_map in core_order_by_pattern.items():
                if slug in order_map:
                    entry["core_pattern_slug"] = pattern_slug
                    entry["core_order"] = order_map[slug]
                    break
        problems.append(entry)

    if missing_core and require_all_core:
        raise SystemExit(
            "Problemas core nao encontrados no dataset: " + ", ".join(sorted(missing_core))
        )
    return problems


# Temas de leitura: feed de papers academicos (Semantic Scholar), nao mais
# artigos de blog do dev.to. search_query e texto livre usado direto na
# busca da API; citation_count entre papers recentes (~3 anos) e o proxy
# de "bem-sucedido" usado pra ordenar (nao existe fonte aberta confiavel
# de "papers premiados").
TOPICS = [
    {"name": "Banco de Dados", "search_query": "database systems"},
    {"name": "Inteligência Artificial", "search_query": "artificial intelligence"},
    {"name": "Machine Learning Tradicional", "search_query": "classical machine learning"},
]


# ---------------------------------------------------------------------------
# Load
# ---------------------------------------------------------------------------
def load(problems: list[dict], output_dir: str) -> tuple[str, str]:
    os.makedirs(output_dir, exist_ok=True)

    seed_problems_path = os.path.join(output_dir, "seed_problems.json")
    with open(seed_problems_path, "w", encoding="utf-8") as f:
        json.dump({"patterns": PATTERNS, "problems": problems}, f, ensure_ascii=False, indent=2)

    seed_topics_path = os.path.join(output_dir, "seed_topics.json")
    with open(seed_topics_path, "w", encoding="utf-8") as f:
        json.dump({"topics": TOPICS}, f, ensure_ascii=False, indent=2)

    return seed_problems_path, seed_topics_path


def print_summary(problems: list[dict]) -> None:
    patterns_with_pool_count = {p["slug"]: 0 for p in PATTERNS}
    for entry in problems:
        for p in entry["patterns"]:
            if p["slug"] in patterns_with_pool_count:
                patterns_with_pool_count[p["slug"]] += 1

    print(f"\nTotal de problemas no seed: {len(problems)}")
    print(f"Core (fase 1): {sum(1 for p in problems if p['is_core'])}")
    print("Pool (exploracao) por padrao (contagem de problem_patterns):")
    for p in PATTERNS:
        print(f"  {p['order_in_plan']:>2}. {p['name']:<35} {patterns_with_pool_count[p['slug']]}")
    sem_padrao = sum(1 for p in problems if not p["patterns"])
    print(f"Problemas sem nenhum padrao mapeado (ficam so no pool geral): {sem_padrao}")


def main(argv: list[str] | None = None) -> None:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--out", default=DEFAULT_OUTPUT_DIR, help="diretorio de saida dos JSONs")
    parser.add_argument("--cache-dir", default=DEFAULT_CACHE_DIR, help="onde guardar os downloads")
    parser.add_argument("--offline", action="store_true", help="nao baixa nada; exige cache")
    args = parser.parse_args(argv)

    questions, ratings = extract(args.cache_dir, args.offline)
    problems = transform(questions, ratings)
    paths = load(problems, args.out)
    print_summary(problems)
    print("\nEscrito em:\n  " + "\n  ".join(paths))


if __name__ == "__main__":
    main(sys.argv[1:])
