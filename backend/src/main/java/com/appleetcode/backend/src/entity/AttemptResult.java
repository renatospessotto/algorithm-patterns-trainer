package com.appleetcode.backend.src.entity;

/**
 * Resultado categórico de uma tentativa. Cada valor mapeia diretamente pra
 * escala de qualidade q (0-5) usada no algoritmo de repetição (docs/spec/original-spec.md
 * seção 4: "0 desistiu, 5 resolveu confortável e rápido nomeando o
 * padrão"). O `confidence` (1-5) do request é um campo separado,
 * autoavaliado, guardado só pra contexto/nota — não entra na fórmula.
 */
public enum AttemptResult {
    GAVE_UP(0),
    FAILED(1),
    SOLVED_WITH_HEAVY_HINT(2),
    SOLVED_WITH_HINT(3),
    SOLVED(4),
    SOLVED_FAST_NAMED_PATTERN(5);

    private final int q;

    AttemptResult(int q) {
        this.q = q;
    }

    public int qualityScore() {
        return q;
    }
}
