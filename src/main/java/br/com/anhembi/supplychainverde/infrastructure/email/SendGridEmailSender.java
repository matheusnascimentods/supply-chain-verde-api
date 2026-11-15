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

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * Twilio SendGrid via API v3 (POST /v3/mail/send). Sem o SDK: é uma chamada só.
 * Todo email leva o logo como anexo inline, referenciado no HTML por cid:logo (Gmail e Outlook não mostram SVG
 * nem data URI). ponytail: logo fixo para todos os emails; passar anexos pela interface quando um email não quiser.
 */
@Component
public class SendGridEmailSender implements EmailSender {
    private static final Map<String, String> LOGO = Map.of(
            "content", Base64.getEncoder().encodeToString(load("/templates/logo.png")),
            "type", "image/png",
            "filename", "logo.png",
            "disposition", "inline",
            "content_id", "logo");

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
                "content", List.of(Map.of("type", "text/html", "value", html)),
                "attachments", List.of(LOGO));
        try {
            // Status fora de 2xx também lança RestClientException (RestClientResponseException).
            client.post().uri("/v3/mail/send").contentType(MediaType.APPLICATION_JSON).body(body).retrieve().toBodilessEntity();
        } catch (RestClientException exception) {
            throw new EmailDeliveryException("SendGrid recusou o envio para " + to + ".", exception);
        }
    }

    private static byte[] load(String path) {
        try (InputStream in = SendGridEmailSender.class.getResourceAsStream(path)) {
            if (in == null) throw new IllegalStateException("Recurso não encontrado: " + path);
            return in.readAllBytes();
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }
}
