package pt.psoft.g1.psoftg1.bookmanagement.api;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class BookExternalView {
    private String isbn;
    private String title;
}
