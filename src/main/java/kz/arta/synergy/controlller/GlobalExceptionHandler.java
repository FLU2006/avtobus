package kz.arta.synergy.controlller;

import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<String> handleJsonParseError(HttpMessageNotReadableException ex) {
        return ResponseEntity
                .badRequest()
                .body("Ошибка структуры JSON: " + ex.getMostSpecificCause().getMessage());
    }
}
