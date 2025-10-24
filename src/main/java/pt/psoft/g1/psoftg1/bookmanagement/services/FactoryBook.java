package pt.psoft.g1.psoftg1.bookmanagement.services;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import pt.psoft.g1.psoftg1.authormanagement.model.Author;
import pt.psoft.g1.psoftg1.bookmanagement.model.Book;
import pt.psoft.g1.psoftg1.genremanagement.model.Genre;
import pt.psoft.g1.psoftg1.idgenerator.IdGenerator;

import java.util.List;

@Service
@AllArgsConstructor
public class FactoryBook {

    private final IdGenerator idGenerator;


    public Book generateBook(CreateBookRequest createBookRequest, String isbn, Genre genre, List<Author> authors,String photoURI) {

        return  new Book(idGenerator.generateId(), isbn, createBookRequest.getTitle(), createBookRequest.getDescription(),genre,authors,photoURI);
    }
}
