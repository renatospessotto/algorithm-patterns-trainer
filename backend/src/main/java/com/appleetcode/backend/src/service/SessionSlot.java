package com.appleetcode.backend.src.service;

import com.appleetcode.backend.src.entity.Problem;

/**
 * Um dos 2 slots da sessão do dia.
 *
 * @param origin "novo" (problema novo da sequência fixa), "revisao" (topo da
 *               fila de urgência) ou "pool" (fallback de exploração na fase
 *               de consolidação, quando a fila de urgência não tem 2 itens
 *               relevantes).
 */
public record SessionSlot(Problem problem, String origin, Double urgency) {
}
