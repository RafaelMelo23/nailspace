package com.rafael.agendanails.webapp.infrastructure.controller.pages;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class PagesController {

    @RequestMapping({
            "/",
            "/agendar",
            "/entrar",
            "/cadastro",
            "/perfil",
            "/admin/servicos",
            "/admin/configuracoes",
            "/profissional/agenda",
            "/redefinir-senha",
            "/offline"
    })
    public String index() {
        return "forward:/index.html";
    }
}