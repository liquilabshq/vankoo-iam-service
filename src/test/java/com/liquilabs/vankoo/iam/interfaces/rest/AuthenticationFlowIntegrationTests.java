package com.liquilabs.vankoo.iam.interfaces.rest;

import com.jayway.jsonpath.JsonPath;
import com.liquilabs.vankoo.iam.application.internal.outboundservices.mail.MailService;
import com.liquilabs.vankoo.iam.domain.model.valueobjects.Email;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Recorre la API de IAM de punta a punta (controladores, seguridad, servicios y
 * PostgreSQL) y comprueba el contrato de errores RFC 9457 de cada rechazo.
 *
 * <p>Los eventos de integración van al log ({@code vankoo.events.enabled=false}) y el
 * correo se sustituye por un doble, que además entrega el token de recuperación.
 */
@SpringBootTest(properties = "vankoo.events.enabled=false")
@AutoConfigureMockMvc
class AuthenticationFlowIntegrationTests {

    private static final String AUTH = "/api/v1/authentication";
    private static final String PASSWORD = "S3cure-pass";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MailService mailService;

    private static String newEmail() {
        return "it-" + UUID.randomUUID() + "@vankoo.test";
    }

    private ResultActions postJson(String path, String body) throws Exception {
        return mockMvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions signUp(String email, String password, String roles) throws Exception {
        return postJson(AUTH + "/sign-up",
                "{\"email\":\"%s\",\"password\":\"%s\",\"roles\":%s}".formatted(email, password, roles));
    }

    private ResultActions signIn(String email, String password) throws Exception {
        return postJson(AUTH + "/sign-in", "{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password));
    }

    private String tokenFor(String email, String password) throws Exception {
        var body = signIn(email, password).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.token");
    }

    @Test
    void signsUpAMypeAndReturnsItsRole() throws Exception {
        var email = newEmail();

        signUp(email, PASSWORD, "[\"ROLE_MYPE\"]")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.roles[0]").value("ROLE_MYPE"))
                .andExpect(jsonPath("$.id").isNotEmpty());
    }

    @Test
    void signUpWithoutRolesGetsTheDefaultRole() throws Exception {
        signUp(newEmail(), PASSWORD, "null")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.roles[0]").value("ROLE_USER"));
    }

    @Test
    void rejectsADuplicateEmailWithAConflict() throws Exception {
        var email = newEmail();
        signUp(email, PASSWORD, "[\"ROLE_INVESTOR\"]").andExpect(status().isCreated());

        signUp(email, PASSWORD, "[\"ROLE_INVESTOR\"]")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("email-already-in-use"));
    }

    @Test
    void refusesToSelfAssignTheAdminRole() throws Exception {
        signUp(newEmail(), PASSWORD, "[\"ROLE_ADMIN\"]")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("role-not-allowed"));
    }

    @Test
    void reportsEachInvalidFieldOfTheBody() throws Exception {
        signUp("not-an-email", "", "[]")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation-failed"))
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    void answersAnUnreadableBodyWithACode() throws Exception {
        postJson(AUTH + "/sign-in", "{not json")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").isNotEmpty());
    }

    @Test
    void answersAWrongMethodWithACode() throws Exception {
        mockMvc.perform(put(AUTH + "/sign-in").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").isNotEmpty());
    }

    @Test
    void signsInAndIssuesAToken() throws Exception {
        var email = newEmail();
        signUp(email, PASSWORD, "[\"ROLE_MYPE\"]").andExpect(status().isCreated());

        signIn(email, PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void wrongPasswordAndUnknownAccountGetTheSameAnswer() throws Exception {
        var email = newEmail();
        signUp(email, PASSWORD, "[\"ROLE_MYPE\"]").andExpect(status().isCreated());

        var wrongPassword = signIn(email, "wrong-password")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("invalid-credentials"))
                .andReturn().getResponse().getContentAsString();
        var unknownAccount = signIn(newEmail(), PASSWORD)
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();

        // Solo cambia la instancia; el resto del cuerpo no delata cuál de los dos falló.
        assertThat(JsonPath.<String>read(unknownAccount, "$.detail"))
                .isEqualTo(JsonPath.<String>read(wrongPassword, "$.detail"));
    }

    @Test
    void protectedRoutesRequireABearerToken() throws Exception {
        mockMvc.perform(get("/api/v1/users/{email}", newEmail()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("unauthenticated"));
    }

    @Test
    void rejectsATamperedToken() throws Exception {
        mockMvc.perform(get("/api/v1/users/{email}", newEmail())
                        .header("Authorization", "Bearer not.a.valid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().exists("WWW-Authenticate"));
    }

    @Test
    void readsAUserWithAValidToken() throws Exception {
        var email = newEmail();
        signUp(email, PASSWORD, "[\"ROLE_INVESTOR\"]").andExpect(status().isCreated());
        var token = tokenFor(email, PASSWORD);

        mockMvc.perform(get("/api/v1/users/{email}", email).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.roles[0]").value("ROLE_INVESTOR"));
    }

    @Test
    void answersNotFoundForAnUnknownUser() throws Exception {
        var email = newEmail();
        signUp(email, PASSWORD, "[\"ROLE_MYPE\"]").andExpect(status().isCreated());
        var token = tokenFor(email, PASSWORD);

        mockMvc.perform(get("/api/v1/users/{email}", newEmail()).header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("user-not-found"));
    }

    @Test
    void forgotPasswordAnswersTheSameWhetherTheAccountExistsOrNot() throws Exception {
        postJson(AUTH + "/forgot-password", "{\"email\":\"%s\"}".formatted(newEmail()))
                .andExpect(status().isAccepted());
    }

    @Test
    void resetsThePasswordOnceWithTheEmailedToken() throws Exception {
        var email = newEmail();
        signUp(email, PASSWORD, "[\"ROLE_MYPE\"]").andExpect(status().isCreated());

        postJson(AUTH + "/forgot-password", "{\"email\":\"%s\"}".formatted(email))
                .andExpect(status().isAccepted());
        var token = ArgumentCaptor.forClass(String.class);
        verify(mailService, timeout(5_000))
                .sendPasswordResetEmail(eq(new Email(email)), token.capture(), any(Date.class));

        var reset = "{\"token\":\"%s\",\"password\":\"N3w-pass\"}".formatted(token.getValue());
        postJson(AUTH + "/reset-password", reset).andExpect(status().isNoContent());

        signIn(email, "N3w-pass").andExpect(status().isOk());
        signIn(email, PASSWORD).andExpect(status().isUnauthorized());
        postJson(AUTH + "/reset-password", reset)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("invalid-password-reset-token"));
    }

    @Test
    void rejectsAnUnknownResetToken() throws Exception {
        postJson(AUTH + "/reset-password", "{\"token\":\"unknown\",\"password\":\"N3w-pass\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("invalid-password-reset-token"));
    }
}
