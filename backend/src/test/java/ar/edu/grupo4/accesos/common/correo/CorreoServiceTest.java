package ar.edu.grupo4.accesos.common.correo;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CorreoServiceTest {

    private final JavaMailSender mailSender = mock(JavaMailSender.class);
    @SuppressWarnings("unchecked")
    private final ObjectProvider<JavaMailSender> proveedor = mock(ObjectProvider.class);

    private CorreoService servicio(String servidor) {
        when(proveedor.getObject()).thenReturn(mailSender);
        return new CorreoService(proveedor, servidor, "no-responder@accesos.local", true);
    }

    @Test
    void conServidorConfiguradoMandaElCorreo() {
        boolean enviado = servicio("mailpit").enviar("guardia@seed.test", "Asunto", "Cuerpo");

        ArgumentCaptor<SimpleMailMessage> mensaje = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(mensaje.capture());
        assertThat(enviado).isTrue();
        assertThat(mensaje.getValue().getTo()).containsExactly("guardia@seed.test");
        assertThat(mensaje.getValue().getFrom()).isEqualTo("no-responder@accesos.local");
        assertThat(mensaje.getValue().getSubject()).isEqualTo("Asunto");
        assertThat(mensaje.getValue().getText()).isEqualTo("Cuerpo");
    }

    @Test
    void conVersionHtmlMandaUnCorreoConTextoYHtml() throws Exception {
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));

        boolean enviado = servicio("mailpit").enviar("guardia@seed.test", "Asunto", "Texto", "<p>Html</p>");

        ArgumentCaptor<MimeMessage> mensaje = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(mensaje.capture());
        assertThat(enviado).isTrue();
        assertThat(mensaje.getValue().getAllRecipients()).extracting(Object::toString).containsExactly("guardia@seed.test");
        assertThat(mensaje.getValue().getSubject()).isEqualTo("Asunto");
    }

    @Test
    void sinServidorNoIntentaMandarNada() {
        boolean enviado = servicio("").enviar("guardia@seed.test", "Asunto", "Cuerpo");

        assertThat(enviado).isFalse();
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void siElServidorFallaDevuelveFalseSinCortarLaOperacion() {
        CorreoService servicio = servicio("mailpit");
        doThrow(new MailSendException("sin conexión")).when(mailSender).send(any(SimpleMailMessage.class));

        assertThat(servicio.enviar("guardia@seed.test", "Asunto", "Cuerpo")).isFalse();
    }
}
