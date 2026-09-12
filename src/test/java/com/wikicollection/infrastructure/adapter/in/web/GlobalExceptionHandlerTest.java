package com.wikicollection.infrastructure.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;

import com.wikicollection.application.exception.BookConflictException;
import com.wikicollection.application.exception.BookNotFoundException;
import com.wikicollection.application.exception.BoardGameNotFoundException;
import com.wikicollection.application.exception.DeckNotFoundException;
import com.wikicollection.application.exception.GameNotFoundException;
import com.wikicollection.application.exception.MagicCardNotFoundException;
import com.wikicollection.application.exception.MovieShowConflictException;
import com.wikicollection.application.exception.MovieShowNotFoundException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;

import com.wikicollection.infrastructure.adapter.in.web.dto.ErrorResponse;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Mock
    private HttpServletRequest request;

    @Mock
    private BindingResult bindingResult;

    @BeforeEach
    void setUp() {
        when(request.getRequestURI()).thenReturn("/api/test");
    }

    @Test
    void notFoundExceptions_return404() {
        assertThat(handler.handleBookNotFound(new BookNotFoundException("x"), request).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(handler.handleGameNotFound(new GameNotFoundException("x"), request).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(handler.handleBoardGameNotFound(new BoardGameNotFoundException("x"), request).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(handler.handleMagicCardNotFound(new MagicCardNotFoundException("x"), request).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(handler.handleDeckNotFound(new DeckNotFoundException("x"), request).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(handler.handleMovieShowNotFound(new MovieShowNotFoundException("x"), request).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void conflictExceptions_return409() {
        assertThat(handler.handleBookConflict(new BookConflictException("x"), request).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
        assertThat(handler.handleMovieShowConflict(new MovieShowConflictException("x"), request).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void constraintViolation_returns400() {
        ConstraintViolation<?> violation = org.mockito.Mockito.mock(ConstraintViolation.class);
        when(violation.getMessage()).thenReturn("La búsqueda no puede superar los 100 caracteres");
        ConstraintViolationException ex = new ConstraintViolationException(java.util.Set.of(violation));

        ResponseEntity<ErrorResponse> response = handler.handleConstraintViolation(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().message()).contains("100 caracteres");
    }

    @Test
    void illegalArgument_returns400() {        ResponseEntity<ErrorResponse> response =
                handler.handleBadRequest(new IllegalArgumentException("mal"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("mal");
        assertThat(response.getBody().status()).isEqualTo(400);
        assertThat(response.getBody().path()).isEqualTo("/api/test");
    }

    @Test
    void validationException_returns400WithFieldErrors() {
        when(bindingResult.getFieldErrors()).thenReturn(
                List.of(new FieldError("obj", "title", "obligatorio")));
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);

        ResponseEntity<ErrorResponse> response = handler.handleValidation(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().message()).contains("title").contains("obligatorio");
    }

    @Test
    void illegalState_returns500() {
        assertThat(handler.handleInternal(new IllegalStateException("x"), request).getStatusCode())
                .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void externalResponseError_returns502() {
        HttpClientErrorException ex = new HttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR);

        ResponseEntity<ErrorResponse> response = handler.handleExternalError(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
    }

    @Test
    void externalUnavailable_returns503() {
        ResponseEntity<ErrorResponse> response =
                handler.handleExternalUnavailable(new ResourceAccessException("down"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }
}
