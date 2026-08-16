package SmartHarvest360.api;

import SmartHarvest360.api.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/** Converts service exceptions into uniform JSON error bodies. */
@RestControllerAdvice
public class ApiExceptionHandler {

    /** Validation/flow errors (e.g. unknown crop, not set up yet). */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse badRequest(IllegalArgumentException exception) {
        return new ErrorResponse(exception.getMessage());
    }

    /** Server-side failures (e.g. crops.csv missing or unwritable). */
    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse serverError(IllegalStateException exception) {
        return new ErrorResponse(exception.getMessage());
    }

    /** Missing static resources (e.g. favicon.ico) are 404, not 500. */
    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse notFound(NoResourceFoundException exception) {
        return new ErrorResponse("Not found: " + exception.getResourcePath());
    }

    /** Catch-all so unexpected failures still return the uniform JSON error body. */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse unexpected(Exception exception) {
        return new ErrorResponse("Unexpected server error: " + exception.getMessage());
    }
}
