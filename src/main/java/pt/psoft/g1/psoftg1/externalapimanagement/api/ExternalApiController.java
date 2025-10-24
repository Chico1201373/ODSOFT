package pt.psoft.g1.psoftg1.externalapimanagement.api;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pt.psoft.g1.psoftg1.externalapimanagement.api.BookExternalView;
import pt.psoft.g1.psoftg1.externalapimanagement.service.ExternalApiService;
import jakarta.validation.constraints.NotBlank;

@Tag(name = "Books", description = "Endpoints for managing Books")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/external")
public class ExternalApiController {
    private final ExternalApiService bookExternalService;
    private final BookExternalMapper bookExternalMapper;

    @GetMapping(value = "/book/isbn/{title}")
    public ResponseEntity<BookExternalView> receiveBooks(@PathVariable @NotBlank(message = "Title cannot be empty") String title){
        try {
        String isbn = bookExternalService.getIsbn(title);
        BookExternalView view = bookExternalMapper.toBookExternal(title, isbn);
        return ResponseEntity.ok(view);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
