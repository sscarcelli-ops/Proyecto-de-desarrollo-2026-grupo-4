package ar.edu.grupo4.accesos.common.paging;

import ar.edu.grupo4.accesos.common.exception.BusinessException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;

import java.util.Set;

/**
 * Columnas por las que un listado se puede ordenar con ?sort=. Sin esto, ?sort= acepta cualquier atributo de la
 * entidad, incluidos los de entidades relacionadas: ?sort=usuario.passwordHash ordenaría por el hash de la
 * contraseña (y deja deducir información de él), y un atributo que no existe da un error 500.
 *
 * <pre>trabajadorService.listar(OrdenPermitido.validar(pageable, Set.of("nombre", "apellido", "dni")));</pre>
 */
public final class OrdenPermitido {

    private OrdenPermitido() {
    }

    /** Devuelve el mismo Pageable si solo ordena por columnas permitidas; si no, 400 ORDEN_INVALIDO. */
    public static Pageable validar(Pageable pageable, Set<String> permitidas) {
        for (Sort.Order orden : pageable.getSort()) {
            if (!permitidas.contains(orden.getProperty())) {
                throw new BusinessException("ORDEN_INVALIDO", "No se puede ordenar el listado por ese campo.",
                    HttpStatus.BAD_REQUEST);
            }
        }
        return pageable;
    }
}
