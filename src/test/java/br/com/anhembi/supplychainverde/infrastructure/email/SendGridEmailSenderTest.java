package br.com.anhembi.supplychainverde.infrastructure.email;

import br.com.anhembi.supplychainverde.application.exception.EmailDeliveryException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

class SendGridEmailSenderTest {
    private final RestClient.Builder builder = RestClient.builder();
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    private final SendGridEmailSender sender = new SendGridEmailSender(builder, "chave", "noreply@verde.test");

    @Test
    void postsTheMailToSendGridWithTheApiKey() {
        server.expect(requestTo("https://api.sendgrid.com/v3/mail/send"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer chave"))
                .andExpect(jsonPath("$.personalizations[0].to[0].email").value("ana@it.test"))
                .andExpect(jsonPath("$.from.email").value("noreply@verde.test"))
                .andExpect(jsonPath("$.subject").value("Assunto"))
                .andExpect(jsonPath("$.content[0].type").value("text/html"))
                .andExpect(jsonPath("$.content[0].value").value("<p>oi</p>"))
                .andExpect(jsonPath("$.attachments[0].content_id").value("logo"))
                .andExpect(jsonPath("$.attachments[0].disposition").value("inline"))
                .andExpect(jsonPath("$.attachments[0].content").isNotEmpty())
                .andRespond(withStatus(HttpStatus.ACCEPTED));

        sender.send("ana@it.test", "Assunto", "<p>oi</p>");

        server.verify();
    }

    @Test
    void rejectedMailBecomesEmailDeliveryException() {
        server.expect(requestTo("https://api.sendgrid.com/v3/mail/send")).andRespond(withStatus(HttpStatus.FORBIDDEN));

        assertThatThrownBy(() -> sender.send("ana@it.test", "Assunto", "<p>oi</p>"))
                .isInstanceOf(EmailDeliveryException.class);
    }
}
