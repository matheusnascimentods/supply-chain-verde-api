package br.com.anhembi.supplychainverde.application.mapper;
import br.com.anhembi.supplychainverde.application.dto.chain.ChainResponseDTO;
import br.com.anhembi.supplychainverde.domain.entity.CarbonEmission;
import br.com.anhembi.supplychainverde.domain.entity.Chain;
import br.com.anhembi.supplychainverde.domain.entity.Transport;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR,
        uses = {AddressDtoMapper.class, TransportDtoMapper.class, CarbonEmissionDtoMapper.class})
public interface ChainDtoMapper {
    @Mapping(target = "chainId", source = "chain.chainId")
    @Mapping(target = "batchId", source = "chain.batch.batchId")
    @Mapping(target = "originAddress", source = "chain.originAddress")
    @Mapping(target = "destinationAddress", source = "chain.destinationAddress")
    @Mapping(target = "responsibleUserId", source = "chain.responsibleUser.userId")
    @Mapping(target = "responsibleUserName", source = "chain.responsibleUser.name")
    @Mapping(target = "stageType", source = "chain.stageType")
    @Mapping(target = "startedAt", source = "chain.startedAt")
    @Mapping(target = "endedAt", source = "chain.endedAt")
    ChainResponseDTO toResponse(Chain chain, Transport transport, CarbonEmission emission);
}
