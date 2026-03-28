package com.rafael.nailspro.webapp.infrastructure.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void whenBusinessException_thenReturn400() throws Exception {
        mockMvc.perform(get("/test/business"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Erro de validação"))
                .andExpect(jsonPath("$.message", hasItem("Business error message")));
    }

    @Test
    void whenProfessionalBusyException_thenReturn409() throws Exception {
        mockMvc.perform(get("/test/busy"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Erro de validação"))
                .andExpect(jsonPath("$.message", hasItem("Professional is busy")));
    }

    @Test
    void whenUserAlreadyExistsException_thenReturn400() throws Exception {
        mockMvc.perform(get("/test/user-exists"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Usuário já cadastrado"))
                .andExpect(jsonPath("$.message", hasItem("User already exists")));
    }

    @Test
    void whenGenericException_thenReturn500() throws Exception {
        mockMvc.perform(get("/test/generic"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Erro interno do servidor"))
                .andExpect(jsonPath("$.message", hasItem("Ocorreu um erro inesperado. Contate o suporte.")));
    }

    @RestController
    static class TestController {
        @GetMapping("/test/business")
        public void business() {
            throw new BusinessException("Business error message");
        }

        @GetMapping("/test/busy")
        public void busy() {
            throw new ProfessionalBusyException("Professional is busy");
        }

        @GetMapping("/test/user-exists")
        public void userExists() {
            throw new UserAlreadyExistsException("User already exists");
        }

        @GetMapping("/test/generic")
        public void generic() {
            throw new RuntimeException("Unexpected error");
        }
    }
}
