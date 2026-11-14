package br.com.anhembi.supplychainverde.infrastructure.email;

import br.com.anhembi.supplychainverde.application.exception.EmailDeliveryException;
import br.com.anhembi.supplychainverde.domain.service.EmailSender;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;

/** Twilio SendGrid via API v3 (POST /v3/mail/send). Sem o SDK: é uma chamada só. */
@Component
public class SendGridEmailSender implements EmailSender {
    private final RestClient client;
    private final String from;

    @Autowired
    public SendGridEmailSender(@Value("${sendgrid.api-key}") String apiKey, @Value("${sendgrid.from}") String from) {
        this(RestClient.builder(), apiKey, from);
    }

    SendGridEmailSender(RestClient.Builder builder, String apiKey, String from) {
        this.client = builder
                .baseUrl("https://api.sendgrid.com")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .build();
        this.from = from;
    }

    @Override
    public void send(String to, String subject, String html) {
        Map<String, Object> body = Map.of(
                "personalizations", List.of(Map.of("to", List.of(Map.of("email", to)))),
                "from", Map.of("email", from),
                "subject", subject,
                "content", List.of(Map.of("type", "text/html", "value", html)));
        try {
            // Status fora de 2xx também lança RestClientException (RestClientResponseException).
            client.post().uri("/v3/mail/send").contentType(MediaType.APPLICATION_JSON).body(body).retrieve().toBodilessEntity();
        } catch (RestClientException exception) {
            throw new EmailDeliveryException("SendGrid recusou o envio para " + to + ".", exception);
        }
    }
}
