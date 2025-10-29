package pt.psoft.g1.psoftg1.externalapimanagement.api;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface BookExternalMapper {
    BookExternalMapper INSTANCE = Mappers.getMapper(BookExternalMapper.class);
    BookExternalView toBookExternal(String isbn);
}
