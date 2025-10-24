package pt.psoft.g1.psoftg1.externalapimanagement.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import pt.psoft.g1.psoftg1.externalapimanagement.model.BookIsbnAPI;

@Service
@AllArgsConstructor
public class ExternalApiServiceImpl implements ExternalApiService {
    private final BookIsbnAPI bookIsbnAPI;

    @Override
    public String getIsbn(String title) {
        return bookIsbnAPI.getIsbn(title);
    }
}
