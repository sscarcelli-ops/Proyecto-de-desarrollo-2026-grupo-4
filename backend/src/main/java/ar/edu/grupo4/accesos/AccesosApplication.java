package ar.edu.grupo4.accesos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.boot.support.EnvironmentPostProcessorApplicationListener;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;

import java.time.ZoneId;
import java.util.TimeZone;

@SpringBootApplication
public class AccesosApplication {

	public static void main(String[] args) {
		SpringApplication app = new SpringApplication(AccesosApplication.class);
		app.addListeners(new ZonaHoraria());
		app.run(args);
	}

	/**
	 * Zona horaria del sistema (app.zona-horaria, variable ZONA_HORARIA), sin depender de cómo esté configurada cada
	 * máquina. Define qué es "hoy" para los vencimientos y las fechas que se muestran; en la BD las fechas se guardan
	 * en UTC (application.yml). PostgreSQL toma esta zona para CURRENT_DATE en la función de la garita.
	 *
	 * Se aplica apenas está cargada la configuración (incluido backend/.env) y antes de que arranquen los logs, que
	 * toman la zona al iniciarse: así la hora de los logs también es la del sistema.
	 * Usar el nombre IANA actual: Windows informa "America/Buenos_Aires", un alias viejo que la imagen Docker de
	 * PostgreSQL ya no reconoce y rechaza la conexión. Una zona inválida corta el arranque con el error de ZoneId.
	 * En los tests la zona la fija maven-surefire-plugin en el pom.xml (@SpringBootTest no ejecuta este main).
	 */
	static class ZonaHoraria implements ApplicationListener<ApplicationEnvironmentPreparedEvent>, Ordered {

		@Override
		public void onApplicationEvent(ApplicationEnvironmentPreparedEvent evento) {
			String zona = evento.getEnvironment().getRequiredProperty("app.zona-horaria");
			TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.of(zona)));
		}

		// Después de cargar la configuración y antes de iniciar los logs (LoggingApplicationListener)
		@Override
		public int getOrder() {
			return EnvironmentPostProcessorApplicationListener.DEFAULT_ORDER + 1;
		}
	}

}
