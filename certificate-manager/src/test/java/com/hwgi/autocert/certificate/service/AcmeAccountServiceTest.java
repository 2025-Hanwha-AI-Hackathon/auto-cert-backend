package com.hwgi.autocert.certificate.service;

import com.hwgi.autocert.certificate.config.AcmeProperties;
import com.hwgi.autocert.domain.model.AcmeAccount;
import com.hwgi.autocert.domain.repository.AcmeAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * ACME 계정 서비스 테스트
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AcmeAccountService 테스트")
class AcmeAccountServiceTest {

    @Mock
    private AcmeAccountRepository acmeAccountRepository;

    @Mock
    private AcmeProperties acmeProperties;

    @InjectMocks
    private AcmeAccountService acmeAccountService;

    private AcmeAccount testAccount;

    @BeforeEach
    void setUp() {
        testAccount = AcmeAccount.builder()
                .id(1L)
                .email("test@example.com")
                .acmeServerUrl("https://acme-staging-v02.api.letsencrypt.org/directory")
                .accountUrl("https://acme-staging-v02.api.letsencrypt.org/acme/acct/12345")
                .privateKeyPem("test-private-key")
                .publicKeyPem("test-public-key")
                .status("ACTIVE")
                .keyAlgorithm("RSA")
                .keySize(4096)
                .termsAgreed(true)
                .build();
    }

    @Test
    @DisplayName("기존 계정이 있을 때 조회 성공")
    void getOrCreateDefaultAccount_ExistingAccount_Success() {
        // Given
        when(acmeProperties.getAccountEmail()).thenReturn("test@example.com");
        when(acmeProperties.getDirectoryUrl())
                .thenReturn("https://acme-staging-v02.api.letsencrypt.org/directory");
        when(acmeAccountRepository.findByEmailAndAcmeServerUrl(anyString(), anyString()))
                .thenReturn(Optional.of(testAccount));
        when(acmeAccountRepository.save(any(AcmeAccount.class)))
                .thenReturn(testAccount);

        // When
        AcmeAccount result = acmeAccountService.getOrCreateDefaultAccount();

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo("test@example.com");
        assertThat(result.getStatus()).isEqualTo("ACTIVE");

        verify(acmeAccountRepository).findByEmailAndAcmeServerUrl(anyString(), anyString());
        verify(acmeAccountRepository).save(any(AcmeAccount.class));
    }

    @Test
    @DisplayName("ID로 계정 조회 성공")
    void findById_Success() {
        // Given
        Long accountId = 1L;
        when(acmeAccountRepository.findById(accountId))
                .thenReturn(Optional.of(testAccount));

        // When
        AcmeAccount result = acmeAccountService.findById(accountId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(accountId);
        assertThat(result.getEmail()).isEqualTo("test@example.com");

        verify(acmeAccountRepository).findById(accountId);
    }

    @Test
    @DisplayName("이메일로 계정 조회 성공")
    void findByEmail_Success() {
        // Given
        String email = "test@example.com";
        when(acmeAccountRepository.findByEmail(email))
                .thenReturn(Optional.of(testAccount));

        // When
        Optional<AcmeAccount> result = acmeAccountService.findByEmail(email);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getEmail()).isEqualTo(email);

        verify(acmeAccountRepository).findByEmail(email);
    }

    @Test
    @DisplayName("계정 비활성화 성공")
    void deactivateAccount_Success() {
        // Given
        Long accountId = 1L;
        when(acmeAccountRepository.findById(accountId))
                .thenReturn(Optional.of(testAccount));
        when(acmeAccountRepository.save(any(AcmeAccount.class)))
                .thenReturn(testAccount);

        // When
        acmeAccountService.deactivateAccount(accountId);

        // Then
        verify(acmeAccountRepository).findById(accountId);
        verify(acmeAccountRepository).save(argThat(account -> 
            "DEACTIVATED".equals(account.getStatus())
        ));
    }
}
