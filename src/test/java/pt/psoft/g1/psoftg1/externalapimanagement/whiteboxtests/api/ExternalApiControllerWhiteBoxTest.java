package pt.psoft.g1.psoftg1.externalapimanagement.whiteboxtests.api;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.ResponseEntity;
import pt.psoft.g1.psoftg1.externalapimanagement.api.BookExternalView;
import pt.psoft.g1.psoftg1.externalapimanagement.api.ExternalApiController;
import pt.psoft.g1.psoftg1.externalapimanagement.service.ExternalApiService;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class ExternalApiControllerWhiteBoxTest {

    @Test
    void receiveBooks_returnsMappedView() {
        ExternalApiService service = Mockito.mock(ExternalApiService.class);
        pt.psoft.g1.psoftg1.externalapimanagement.api.BookExternalMapper mapper = Mockito.mock(pt.psoft.g1.psoftg1.externalapimanagement.api.BookExternalMapper.class);

        Mockito.when(service.getIsbn("Title"))
                .thenReturn("9780000000000");

        BookExternalView view = new BookExternalView();
        view.setIsbn("9780000000000");
        Mockito.when(mapper.toBookExternal("9780000000000")).thenReturn(view);

        ExternalApiController controller = new ExternalApiController(service, mapper);

        ResponseEntity<BookExternalView> resp = controller.receiveBooks("Title");

    assertThat(resp.getStatusCode().value(), is(200));
        assertThat(resp.getBody().getIsbn(), is("9780000000000"));
        Mockito.verify(service, Mockito.times(1)).getIsbn("Title");
        Mockito.verify(mapper, Mockito.times(1)).toBookExternal("9780000000000");
    }

    @Test
    void receiveBooks_whenServiceReturnsNull_mapperReceivesNull() {
        ExternalApiService service = Mockito.mock(ExternalApiService.class);
        pt.psoft.g1.psoftg1.externalapimanagement.api.BookExternalMapper mapper = Mockito.mock(pt.psoft.g1.psoftg1.externalapimanagement.api.BookExternalMapper.class);

        Mockito.when(service.getIsbn("NoTitle")).thenReturn(null);
        BookExternalView view = new BookExternalView();
        view.setIsbn(null);
        Mockito.when(mapper.toBookExternal(null)).thenReturn(view);

        ExternalApiController controller = new ExternalApiController(service, mapper);
        ResponseEntity<BookExternalView> resp = controller.receiveBooks("NoTitle");

    assertThat(resp.getStatusCode().value(), is(200));
        assertThat(resp.getBody().getIsbn(), is((String) null));
        Mockito.verify(service, Mockito.times(1)).getIsbn("NoTitle");
        Mockito.verify(mapper, Mockito.times(1)).toBookExternal(null);
    }
}
