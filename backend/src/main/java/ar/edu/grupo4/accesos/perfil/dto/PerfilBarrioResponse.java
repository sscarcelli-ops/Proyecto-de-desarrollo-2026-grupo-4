package ar.edu.grupo4.accesos.perfil.dto;

import ar.edu.grupo4.accesos.perfil.Perfil;
import ar.edu.grupo4.accesos.perfil.TipoPerfil;

/**
 * US-33 y US-34: Empresa o Monotributista aprobado, visto por el Operador, con su habilitación en el barrio.
 * nombre: razón social de la Empresa o nombre completo del Monotributista (RF-15).
 * Solo lleva lo que muestra la pantalla: el Operador no necesita el email ni el teléfono (RNF-06).
 */
public record PerfilBarrioResponse(
    Long id,
    TipoPerfil tipo,
    String nombre,
    String cuitCuil,
    boolean habilitado
) {
    public static PerfilBarrioResponse from(Perfil p, boolean habilitado) {
        String nombre = p.getTipo() == TipoPerfil.EMPRESA
            ? p.getRazonSocial()
            : p.getNombre() + " " + p.getApellido();
        return new PerfilBarrioResponse(p.getId(), p.getTipo(), nombre, p.getCuitCuil(), habilitado);
    }
}
