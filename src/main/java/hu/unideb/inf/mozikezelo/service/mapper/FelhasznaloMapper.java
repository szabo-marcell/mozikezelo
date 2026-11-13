package hu.unideb.inf.mozikezelo.service.mapper;

import hu.unideb.inf.mozikezelo.data.entity.FelhasznaloEntity;
import hu.unideb.inf.mozikezelo.service.dto.FelhasznaloResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface FelhasznaloMapper {

    @Mapping(target = "jogNev", source = "jog.nev")
    FelhasznaloResponse toResponse(FelhasznaloEntity entity);
}
