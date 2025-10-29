package pt.psoft.g1.psoftg1.externalapimanagement.api;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.psoft.g1.psoftg1.externalapimanagement.api.BookExternalView;
import pt.psoft.g1.psoftg1.externalapimanagement.service.ExternalApiService;

import java.nio.charset.StandardCharsets;

@Tag(name = "Books", description = "Endpoints for managing Books")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/external")
public class ExternalApiController {
    private final ExternalApiService bookExternalService;
    private final BookExternalMapper bookExternalMapper;

    @GetMapping(value = "/book/isbn")
    public ResponseEntity<BookExternalView> receiveBooks(@RequestParam String title) {
        System.out.println("Received request for title: " + title);
        String isbn = bookExternalService.getIsbn(title);
        BookExternalView bookExternalView = bookExternalMapper.toBookExternal(isbn);
        return ResponseEntity.ok().body(bookExternalView);
    }

}
