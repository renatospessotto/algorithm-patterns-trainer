package com.appleetcode.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * Fonte única de "que dia é hoje" pro app inteiro (app.timezone no
 * application.yml). Antes disso, SessionPlannerService e StatsController
 * cada um chamava `ZoneId.systemDefault()` direto -- correto em tese, mas
 * dependente do TZ do container, que por padrão vem em UTC. Um horário de
 * verão/fuso divergente fazia tentativas feitas à noite (horário local)
 * caírem no dia seguinte em UTC, e tanto "current_day" quanto o heatmap de
 * stats pareciam travados/desatualizados.
 */
@Component
public class AppClock {

    private final ZoneId zone;

    public AppClock(@Value("${app.timezone:America/Sao_Paulo}") String timezone) {
        this.zone = ZoneId.of(timezone);
    }

    public ZoneId zone() {
        return zone;
    }

    public LocalDate today() {
        return LocalDate.now(zone);
    }

    public Instant startOfToday() {
        return today().atStartOfDay(zone).toInstant();
    }

    public Instant startOfTomorrow() {
        return today().plusDays(1).atStartOfDay(zone).toInstant();
    }
}
