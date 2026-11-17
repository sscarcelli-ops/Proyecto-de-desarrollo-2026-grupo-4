package ar.edu.grupo4.accesos.common.correo;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * T-04: envío de correo. El servidor SMTP se configura con variables de entorno (MAIL_HOST, MAIL_PORT, ...):
 * <ul>
 *   <li>Ambientes de prueba en Docker: Mailpit, un servidor de prueba que no manda nada afuera y muestra los
 *       correos en http://localhost:8025. No necesita credenciales.</li>
 *   <li>Producción: el SMTP real de la institución (o Gmail con contraseña de aplicación).</li>
 *   <li>Sin MAIL_HOST (por ejemplo, el backend desde el IDE sin Mailpit): no se envía y el contenido se muestra en el
 *       log, la alternativa que aceptó el profesor. Como puede traer un enlace de un solo uso, con el perfil prod el
 *       contenido no se muestra (app.correo.mostrar-en-log en application-prod.yml).</li>
 * </ul>
 * Quien llama registra en el log a quién se le mandó (por id): acá no se escribe el email del destinatario (T-10).
 */
@Service
@Slf4j
public class CorreoService {

    private final ObjectProvider<JavaMailSender> mailSender;
    private final String servidor;
    private final String remitente;
    private final boolean mostrarEnLog;

    public CorreoService(ObjectProvider<JavaMailSender> mailSender,
                         @Value("${spring.mail.host:}") String servidor,
                         @Value("${app.correo.remitente}") String remitente,
                         @Value("${app.correo.mostrar-en-log:true}") boolean mostrarEnLog) {
        this.mailSender = mailSender;
        this.servidor = servidor;
        this.remitente = remitente;
        this.mostrarEnLog = mostrarEnLog;
    }

    /** Si hay un servidor de correo configurado. */
    public boolean configurado() {
        return StringUtils.hasText(servidor);
    }

    /** Envía un correo de texto. Ver {@link #enviar(String, String, String, String)}. */
    public boolean enviar(String destinatario, String asunto, String cuerpo) {
        return enviar(destinatario, asunto, cuerpo, null);
    }

    /**
     * Envía un correo con versión de texto y, si se pasa, versión HTML (el programa de correo muestra la que puede).
     * Un fallo del servidor no corta la operación de quien llama (pedir el recupero responde igual, para no revelar
     * nada): se registra en el log y devuelve false. En el log va siempre la versión de texto.
     */
    public boolean enviar(String destinatario, String asunto, String cuerpo, String cuerpoHtml) {
        if (!configurado()) {
            if (mostrarEnLog) {
                log.warn("Sin servidor de correo (MAIL_HOST vacío): el correo \"{}\" no se envía. Contenido:\n{}",
                    asunto, cuerpo);
            } else {
                log.error("Sin servidor de correo (MAIL_HOST vacío): el correo \"{}\" no se envía", asunto);
            }
            return false;
        }
        try {
            JavaMailSender sender = mailSender.getObject();
            if (cuerpoHtml == null) {
                SimpleMailMessage mensaje = new SimpleMailMessage();
                mensaje.setFrom(remitente);
                mensaje.setTo(destinatario);
                mensaje.setSubject(asunto);
                mensaje.setText(cuerpo);
                sender.send(mensaje);
            } else {
                MimeMessage mensaje = sender.createMimeMessage();
                MimeMessageHelper armado = new MimeMessageHelper(mensaje, true, "UTF-8");
                armado.setFrom(remitente);
                armado.setTo(destinatario);
                armado.setSubject(asunto);
                armado.setText(cuerpo, cuerpoHtml);
                sender.send(mensaje);
            }
            return true;
        } catch (MailException | MessagingException e) {
            // Fuera de prod el contenido va al log, así un enlace no se pierde si Mailpit no está levantado
            if (mostrarEnLog) {
                log.error("No se pudo enviar el correo \"{}\" ({}). Contenido:\n{}", asunto, e.getMessage(), cuerpo);
            } else {
                log.error("No se pudo enviar el correo \"{}\": {}", asunto, e.getMessage());
            }
            return false;
        }
    }
}
