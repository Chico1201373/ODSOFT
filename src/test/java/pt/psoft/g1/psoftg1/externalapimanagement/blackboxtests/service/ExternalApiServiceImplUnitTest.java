package pt.psoft.g1.psoftg1.externalapimanagement.blackboxtests.service;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import pt.psoft.g1.psoftg1.externalapimanagement.model.BookIsbnAPI;
import pt.psoft.g1.psoftg1.externalapimanagement.service.ExternalApiServiceImpl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class ExternalApiServiceImplUnitTest {

    @Test
    void getIsbn_delegatesToBookIsbnAPI() {
        BookIsbnAPI mockApi = Mockito.mock(BookIsbnAPI.class);
        Mockito.when(mockApi.getIsbn("Some Title")).thenReturn("9781111111111");

        ExternalApiServiceImpl service = new ExternalApiServiceImpl(mockApi);

        String isbn = service.getIsbn("Some Title");

        assertThat(isbn, is("9781111111111"));
        Mockito.verify(mockApi, Mockito.times(1)).getIsbn("Some Title");
    }
}
