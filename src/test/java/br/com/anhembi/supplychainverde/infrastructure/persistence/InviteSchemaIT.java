package br.com.anhembi.supplychainverde.infrastructure.persistence;

import br.com.anhembi.supplychainverde.TestcontainersConfiguration;
import br.com.anhembi.supplychainverde.domain.entity.Invite;
import br.com.anhembi.supplychainverde.domain.enums.UserRole;
import br.com.anhembi.supplychainverde.domain.repository.InviteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

import static br.com.anhembi.supplychainverde.JdbcInserts.insert;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Feature 011: expiração gerada pelo banco, token único, auditoria sem o hash e a noção de "ativo". */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Transactional
class InviteSchemaIT {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private InviteRepository inviteRepository;

    private long adminId;
    private long companyId;

    @BeforeEach
    void setUp() {
        adminId = jdbcTemplate.queryForObject(
                "SELECT MIN(id) FROM users WHERE role = 'ADMIN' AND company_id IS NOT NULL", Long.class);
        companyId = jdbcTemplate.queryForObject("SELECT company_id FROM users WHERE id = ?", Long.class, adminId);
    }

    @Test
    void saveReadsBackCreatedAtAndExpiresAtTwentyFourHoursLater() {
        Invite saved = inviteRepository.save(Invite.builder()
                .invitedBy(adminId).invitedFor("novo@it.test").role(UserRole.ADMIN).tokenHash(hash('a')).build());

        assertThat(saved.getInviteId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(Duration.between(saved.getCreatedAt(), saved.getExpiresAt())).isEqualTo(Duration.ofHours(24));
    }

    @Test
    void expiresAtCannotBeWritten() {
        assertThatThrownBy(() -> jdbcTemplate.update("""
                INSERT INTO invite (invited_by, invited_for, role, token_hash, expires_at)
                VALUES (?, 'x@it.test', 'ADMIN', ?, NOW(6) + INTERVAL 1 YEAR)
                """, adminId, hash('b'))).hasMessageContaining("expires_at");
    }

    @Test
    void tokenHashIsUnique() {
        invite("um@it.test", hash('c'), "NOW(6)");
        assertThatThrownBy(() -> invite("dois@it.test", hash('c'), "NOW(6)")).hasMessageContaining("uq_invite_token_hash");
    }

    @Test
    void auditLeavesTokenHashOut() {
        long id = invite("auditado@it.test", hash('d'), "NOW(6)");

        String after = jdbcTemplate.queryForObject(
                "SELECT after_data FROM audit_log WHERE affected_table = 'invite' AND affected_entity_id = ? AND action = 'INSERT'",
                String.class, id);
        assertThat(after).contains("auditado@it.test").doesNotContain("token_hash").doesNotContain(hash('d'));
    }

    @Test
    void onlyNotAcceptedAndNotExpiredInvitesAreActive() {
        long before = inviteRepository.countActiveByCompanyId(companyId);
        invite("ativo@it.test", hash('e'), "NOW(6)");
        invite("vencido@it.test", hash('f'), "NOW(6) - INTERVAL 25 HOUR");
        long accepted = invite("aceito@it.test", hash('g'), "NOW(6)");
        jdbcTemplate.update("UPDATE invite SET accepted = TRUE WHERE id = ?", accepted);

        assertThat(inviteRepository.findActiveByTokenHash(hash('e'))).get()
                .extracting(Invite::getInvitedFor, Invite::getRole).containsExactly("ativo@it.test", UserRole.ADMIN);
        assertThat(inviteRepository.findActiveByTokenHash(hash('f'))).isEmpty();
        assertThat(inviteRepository.findActiveByTokenHash(hash('g'))).isEmpty();
        assertThat(inviteRepository.existsActiveByEmail("ativo@it.test")).isTrue();
        assertThat(inviteRepository.existsActiveByEmail("vencido@it.test")).isFalse();
        assertThat(inviteRepository.existsActiveByEmail("aceito@it.test")).isFalse();
        assertThat(inviteRepository.countActiveByCompanyId(companyId)).isEqualTo(before + 1);
    }

    @Test
    void acceptPersists() {
        invite("aceitar@it.test", hash('h'), "NOW(6)");
        Invite invite = inviteRepository.findActiveByTokenHash(hash('h')).orElseThrow();

        invite.accept();
        inviteRepository.save(invite);

        assertThat(jdbcTemplate.queryForObject("SELECT accepted FROM invite WHERE id = ?", Boolean.class, invite.getInviteId())).isTrue();
        assertThat(inviteRepository.findActiveByTokenHash(hash('h'))).isEmpty();
    }

    private long invite(String email, String tokenHash, String createdAtSql) {
        return insert(jdbcTemplate, """
                INSERT INTO invite (invited_by, invited_for, role, token_hash, created_at)
                VALUES (?, ?, 'ADMIN', ?, %s)
                """.formatted(createdAtSql), adminId, email, tokenHash);
    }

    private static String hash(char c) {
        return String.valueOf(c).repeat(64);
    }
}
