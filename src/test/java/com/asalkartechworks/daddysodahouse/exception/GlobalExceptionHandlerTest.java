package com.asalkartechworks.daddysodahouse.exception;

import com.asalkartechworks.daddysodahouse.dto.ApiResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("ResourceNotFoundException should return HTTP 404 with error ApiResponse")
    void handleResourceNotFoundException_returns404() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Outlet", "id", "101");
        ResponseEntity<ApiResponse<Void>> response = handler.handleResourceNotFoundException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getMessage()).isEqualTo("Outlet not found with id: '101'");
    }

    @Test
    @DisplayName("BadRequestException should return HTTP 400 with error ApiResponse")
    void handleBadRequestException_returns400() {
        BadRequestException ex = new BadRequestException("Invalid franchise parameter");
        ResponseEntity<ApiResponse<Void>> response = handler.handleBadRequestException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getMessage()).isEqualTo("Invalid franchise parameter");
    }

    @Test
    @DisplayName("Generic Exception should return HTTP 500 with generic error message")
    void handleGenericException_returns500() {
        Exception ex = new RuntimeException("Unexpected disk failure");
        ResponseEntity<ApiResponse<Void>> response = handler.handleGenericException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getMessage()).contains("unexpected error occurred");
    }
}
