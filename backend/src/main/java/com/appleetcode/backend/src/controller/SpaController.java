package com.appleetcode.backend.src.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * O roteamento das telas (/hoje, /explorar, /stats, /leitura, /historico) é feito no
 * client-side pelo Angular Router. Sem isso, acessar uma dessas URLs
 * diretamente (ou dar refresh nelas) bate direto no Spring, que não tem
 * esses paths mapeados e devolve 404 em vez do index.html que bootstraria o
 * Angular (docs/spec/original-spec.md seção 7).
 */
@Controller
public class SpaController {

    @GetMapping({"/hoje", "/explorar", "/stats", "/leitura", "/historico"})
    public String forwardToIndex() {
        return "forward:/index.html";
    }
}
