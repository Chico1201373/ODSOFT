package pt.psoft.g1.psoftg1.authormanagement.services;

import org.springframework.stereotype.Service;

import lombok.AllArgsConstructor;
import pt.psoft.g1.psoftg1.authormanagement.model.Author;
import pt.psoft.g1.psoftg1.idgenerator.IdGenerator;

@Service
@AllArgsConstructor
public class FactoryAuthor {

    private final IdGenerator idGenerator;

    public Author generateAuthor(CreateAuthorRequest createAuthorRequest) {
        String id=idGenerator.generateId();
        return new Author(id,createAuthorRequest.getName(), createAuthorRequest.getBio(), createAuthorRequest.getPhotoURI());
    }
}
