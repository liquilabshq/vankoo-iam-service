package com.liquilabs.vankoo.iam.domain.model;

import com.liquilabs.vankoo.iam.domain.exceptions.RoleNotAllowedException;
import com.liquilabs.vankoo.iam.domain.model.aggregates.PasswordResetToken;
import com.liquilabs.vankoo.iam.domain.model.aggregates.User;
import com.liquilabs.vankoo.iam.domain.model.entities.Role;
import com.liquilabs.vankoo.iam.domain.model.valueobjects.Email;
import com.liquilabs.vankoo.iam.domain.model.valueobjects.Password;
import com.liquilabs.vankoo.iam.domain.model.valueobjects.RoleName;
import com.liquilabs.vankoo.iam.domain.model.valueobjects.TokenDigest;
import com.liquilabs.vankoo.iam.domain.model.valueobjects.UserId;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Reglas del modelo de IAM, sin Spring ni base de datos. */
class IamDomainModelTests {

    private static final Email EMAIL = new Email("mype@vankoo.test");
    private static final Password HASH = new Password("$2a$10$hash");

    @Test
    void emailRejectsBlankAndMalformedAddresses() {
        assertThatThrownBy(() -> new Email(" ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Email("no-at-sign")).isInstanceOf(IllegalArgumentException.class);
        assertThat(new Email("a.b+c@vankoo.pe").email()).isEqualTo("a.b+c@vankoo.pe");
    }

    @Test
    void passwordAndDigestCannotBeBlank() {
        assertThatThrownBy(() -> new Password("")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TokenDigest(null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void userIdIsAUuidV7AndNeverNull() {
        var first = new UserId();
        var second = new UserId();
        assertThat(first.id().version()).isEqualTo(7);
        assertThat(first).isNotEqualTo(second);
        assertThatThrownBy(() -> new UserId(null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void onlyMypeAndInvestorAreSelfAssignable() {
        assertThat(RoleName.selfAssignableFrom("ROLE_MYPE")).contains(RoleName.ROLE_MYPE);
        assertThat(RoleName.selfAssignableFrom("ROLE_INVESTOR")).contains(RoleName.ROLE_INVESTOR);
        assertThat(RoleName.selfAssignableFrom("ROLE_ADMIN")).isEmpty();
        assertThatThrownBy(() -> Role.toSelfAssignableRoleFromName("ROLE_ADMIN"))
                .isInstanceOf(RoleNotAllowedException.class);
    }

    @Test
    void aUserWithoutRolesGetsTheDefaultRole() {
        var user = new User(EMAIL, HASH, List.of());

        assertThat(user.getRoles()).extracting(Role::getName).containsExactly(RoleName.ROLE_USER);
        assertThat(user.isNew()).isTrue();
    }

    @Test
    void aUserKeepsTheRolesItIsGiven() {
        var user = new User(EMAIL, HASH, List.of(new Role(RoleName.ROLE_INVESTOR)));

        assertThat(user.getRoles()).extracting(Role::getStringName).containsExactly("ROLE_INVESTOR");
    }

    @Test
    void changingThePasswordReplacesTheHash() {
        var user = new User(EMAIL, HASH);
        user.changePassword(new Password("$2a$10$other"));

        assertThat(user.getPassword().password()).isEqualTo("$2a$10$other");
    }

    @Test
    void aResetTokenIsUsableUntilItExpiresOrIsConsumed() {
        var now = new Date();
        var token = new PasswordResetToken(new UserId(), new TokenDigest("abc"),
                new Date(now.getTime() + 60_000));

        assertThat(token.isUsable(now)).isTrue();
        assertThat(token.isUsable(new Date(now.getTime() + 120_000))).isFalse();

        token.consume(now);
        assertThat(token.isUsable(now)).isFalse();
        assertThat(token.getConsumedAt()).isEqualTo(now);
    }
}
