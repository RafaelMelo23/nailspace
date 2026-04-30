package com.rafael.agendanails.webapp.infrastructure.exception;

import io.sentry.Sentry;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public Object business(BusinessException e, HttpServletRequest request) {
        return buildResponse(e, request, HttpStatus.BAD_REQUEST, "Erro de validação", List.of(e.getMessage()));
    }

    @ExceptionHandler(DemoFeatureException.class)
    public Object demoRestriction(DemoFeatureException e, HttpServletRequest request) {
        return buildResponse(e, request, HttpStatus.FORBIDDEN, "Ambiente de demonstração", List.of(e.getMessage()));
    }

    @ExceptionHandler(TokenRefreshException.class)
    public Object auth(TokenRefreshException e, HttpServletRequest request) {
        return buildResponse(e, request, HttpStatus.UNAUTHORIZED, "Erro de autenticação", List.of(e.getMessage()));
    }

    @ExceptionHandler(LoginException.class)
    public Object auth(LoginException e, HttpServletRequest request) {
        return buildResponse(e, request, HttpStatus.UNAUTHORIZED, "Erro de autenticação", List.of(e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Object dtoValidation(MethodArgumentNotValidException e, HttpServletRequest request) {
        List<String> errorMessages = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .toList();

        if (errorMessages.isEmpty()) {
            errorMessages = List.of("Algum dos dados informados não é valido, revise e tente novamente.");
        }

        return buildResponse(e, request, HttpStatus.BAD_REQUEST, "Erro de validação", errorMessages);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public Object singleParamValidation(ConstraintViolationException e, HttpServletRequest request) {
        List<String> errorMessages = e.getConstraintViolations().stream()
                .map(cv -> cv.getPropertyPath() + ": " + cv.getMessage())
                .toList();

        if (errorMessages.isEmpty()) {
            errorMessages = List.of("Algum dos dados informados não é valido, revise e tente novamente.");
        }

        return buildResponse(e, request, HttpStatus.BAD_REQUEST, "Erro de validação", errorMessages);
    }

    @ExceptionHandler(ProfessionalBusyException.class)
    public Object busy(ProfessionalBusyException e, HttpServletRequest request) {
        return buildResponse(e, request, HttpStatus.CONFLICT, "Erro de validação", List.of(e.getMessage()));
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public Object userAlreadyExists(UserAlreadyExistsException e, HttpServletRequest request) {
        return buildResponse(e, request, HttpStatus.BAD_REQUEST, "Usuário já cadastrado", List.of(e.getMessage()));
    }

    @ExceptionHandler(TenantNotFoundException.class)
    public Object tenantNotFound(TenantNotFoundException e, HttpServletRequest request) {
        return buildResponse(e, request, HttpStatus.NOT_FOUND, "Salão não encontrado", List.of(e.getMessage()));
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public Object noHandlerFound(NoHandlerFoundException e, HttpServletRequest request) {
        return buildResponse(e, request, HttpStatus.NOT_FOUND, "Página não encontrada", List.of("A página que você está procurando não existe."));
    }

    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    public Object noResourceFound(org.springframework.web.servlet.resource.NoResourceFoundException e, HttpServletRequest request) {
        return buildResponse(e, request, HttpStatus.NOT_FOUND, "Recurso não encontrado", List.of("O recurso solicitado não pôde ser encontrado."));
    }

    @ExceptionHandler(NullPointerException.class)
    public Object handleNullPointer(NullPointerException e, HttpServletRequest request) {
        if (request.getRequestURI().startsWith("/api/")) {
            StackTraceElement[] stackTrace = e.getStackTrace();
            for (StackTraceElement element : stackTrace) {
                if (element.getClassName().contains("UserController") || element.getClassName().contains("Controller")) {
                     log.warn("NPE in Controller: likely missing @AuthenticationPrincipal. Returning 401.");
                     return buildResponse(e,
                             request,
                             HttpStatus.UNAUTHORIZED,
                             "Não autorizado",
                             List.of("Autenticação necessária para acessar este recurso."));
                }
            }
        }
        return buildResponse(e, request, HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno do servidor",
                List.of("Ocorreu um erro inesperado. Contate o suporte."));
    }

    @ExceptionHandler(org.springframework.security.authorization.AuthorizationDeniedException.class)
    public Object authorizationDenied(org.springframework.security.authorization.AuthorizationDeniedException e, HttpServletRequest request) {
        if (isUnauthenticated()) {
            return buildResponse(e, request, HttpStatus.UNAUTHORIZED, "Não autorizado", List.of("Sua sessão expirou ou você não está autenticado."));
        }
        return buildResponse(e, request, HttpStatus.FORBIDDEN, "Acesso negado", List.of("Você não tem permissão para acessar este recurso."));
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public Object accessDenied(org.springframework.security.access.AccessDeniedException e, HttpServletRequest request) {
        if (isUnauthenticated()) {
            return buildResponse(e, request, HttpStatus.UNAUTHORIZED, "Não autorizado", List.of("Sua sessão expirou ou você não está autenticado."));
        }
        return buildResponse(e, request, HttpStatus.FORBIDDEN, "Acesso negado", List.of("Você não tem permissão para acessar este recurso."));
    }

    private boolean isUnauthenticated() {
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        return auth == null || auth instanceof org.springframework.security.authentication.AnonymousAuthenticationToken;
    }

    @ExceptionHandler(org.springframework.web.bind.MissingServletRequestParameterException.class)
    public Object missingParam(org.springframework.web.bind.MissingServletRequestParameterException e, HttpServletRequest request) {
        return buildResponse(e, request, HttpStatus.BAD_REQUEST, "Erro de parâmetro", List.of("O parâmetro '" + e.getParameterName() + "' é obrigatório."));
    }

    @ExceptionHandler(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class)
    public Object typeMismatch(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException e, HttpServletRequest request) {
        String message = "O parâmetro '" + e.getName() + "' recebeu um valor inválido.";
        return buildResponse(e, request, HttpStatus.BAD_REQUEST, "Erro de parâmetro", List.of(message));
    }

    @ExceptionHandler(Exception.class)
    public Object genericUnknownError(Exception e, HttpServletRequest request) {
        Sentry.captureException(e);
        return buildResponse(e, request, HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno do servidor",
                List.of("Ocorreu um erro inesperado. Contate o suporte."));
    }

    private Object buildResponse(
            Exception e,
            HttpServletRequest request,
            HttpStatus status,
            String errorTitle,
            List<String> messages) {

        logError(e, request, status);

        if (isHtmlRequest(request)) {
            String viewName = switch (status) {
                case NOT_FOUND -> "error/404";
                case INTERNAL_SERVER_ERROR -> "error/500";
                default -> "error/error";
            };
            return new ModelAndView(viewName, Map.of(
                    "status", status.value(),
                    "error", errorTitle,
                    "messages", messages
            ));
        }

        StandardError err = new StandardError(
                Instant.now(),
                status.value(),
                errorTitle,
                messages,
                request.getRequestURI()
        );

        return ResponseEntity.status(status).body(err);
    }

    private boolean isHtmlRequest(HttpServletRequest request) {
        String accept = request.getHeader("Accept");
        String uri = request.getRequestURI();

        if (accept != null && accept.contains("text/html")) {
            return true;
        }
        return !uri.startsWith("/api/") && 
               !uri.contains(".") && 
               !uri.startsWith("/uploads/");
    }

    private void logError(Exception e, HttpServletRequest request, HttpStatus status) {
        String uri = request.getRequestURI();
        String method = request.getMethod();

        if (status.is5xxServerError()) {
            log.error(
                    "[INTERNAL ERROR]: STATUS: {} - PATH: {} - METHOD: {}",
                    status,
                    uri,
                    method,
                    e
            );
        } else {
            log.error(
                    "[APP-WARN]: STATUS: {} - PATH: {} - METHOD: {}",
                    status,
                    uri,
                    method,
                    e
            );
        }
    }
}