package pt.psoft.g1.psoftg1.externalapimanagement.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
public class IsbnResponse {
    private List<Book> docs;

    public List<Book> getDocs() {
        return docs;
    }

    public void setDocs(List<Book> docs) {
        this.docs = docs;
    }
    public static class Book {
        private List<String> isbn;

        public List<String> getIsbn() {
            return isbn;
        }

        public void setIsbn(List<String> isbn) {
            this.isbn = isbn;
        }
    }
}
