package br.com.anhembi.supplychainverde.application.usecase.certification;

import br.com.anhembi.supplychainverde.application.mapper.CertificationDtoMapper;
import br.com.anhembi.supplychainverde.domain.enums.CertificationStatus;
import br.com.anhembi.supplychainverde.domain.repository.CertificationRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ListCertificationsUseCaseTest {
    private final CertificationRepository repository = mock(CertificationRepository.class);
    private final CertificationDtoMapper mapper = mock(CertificationDtoMapper.class);
    private final ListCertificationsUseCase useCase = new ListCertificationsUseCase(repository, mapper);

    @Test
    void listsAllStatusesWhenStatusIsNotProvided() {
        when(repository.findPage(null, null, 1, 10)).thenReturn(List.of());
        when(repository.countPage(null, null)).thenReturn(0L);

        var response = useCase.execute(1, 10, null, null);

        verify(repository).findPage(null, null, 1, 10);
        verify(repository).countPage(null, null);
        assertThat(response.content()).isEmpty();
        assertThat(response.page()).isEqualTo(1);
        assertThat(response.totalPages()).isZero();
    }

    @Test
    void filtersByRequestedStatus() {
        when(repository.findPage(CertificationStatus.SUSPENDED, 7L, 0, 5)).thenReturn(List.of());
        when(repository.countPage(CertificationStatus.SUSPENDED, 7L)).thenReturn(6L);

        var response = useCase.execute(0, 5, CertificationStatus.SUSPENDED, 7L);

        verify(repository).findPage(CertificationStatus.SUSPENDED, 7L, 0, 5);
        verify(repository).countPage(CertificationStatus.SUSPENDED, 7L);
        assertThat(response.totalElements()).isEqualTo(6L);
        assertThat(response.totalPages()).isEqualTo(2);
    }
}
