package ar.edu.grupo4.accesos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
public class AccesosApplication {

	// Zona horaria fija del sistema (opera en Argentina), sin depender de cómo esté configurada cada máquina.
	// Usa el nombre IANA actual: Windows informa "America/Buenos_Aires", un alias viejo que la imagen Docker
	// de PostgreSQL ya no reconoce y rechaza la conexión. En la BD las fechas se guardan en UTC (application.yml).
	// En los tests la misma zona la fija maven-surefire-plugin en el pom.xml (@SpringBootTest no ejecuta esta clase).
	static {
		TimeZone.setDefault(TimeZone.getTimeZone("America/Argentina/Buenos_Aires"));
	}

	public static void main(String[] args) {
		SpringApplication.run(AccesosApplication.class, args);
	}

}
