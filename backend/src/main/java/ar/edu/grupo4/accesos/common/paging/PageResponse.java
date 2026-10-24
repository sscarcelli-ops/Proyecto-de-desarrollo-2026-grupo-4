package ar.edu.grupo4.accesos.common.paging;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * Respuesta estándar de todos los listados paginados (RNF-08). Nunca se devuelve un Page de Spring
 * directamente: su JSON no es estable entre versiones.
 *
 * <pre>
 * &#64;GetMapping
 * public PageResponse&lt;BarrioResponse&gt; listar(Pageable pageable) {
 *     return PageResponse.from(barrioRepository.findAll(pageable), BarrioResponse::from);
 * }
 * </pre>
 *
 * @param page número de página, empezando en 0 (igual que el parámetro ?page= de Pageable)
 */
public record PageResponse<T>(
    List<T> content,
    int page,
    int size,
    long totalElements,
    int totalPages
) {

    /** Para un Page que ya contiene DTOs. */
    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(),
            page.getTotalElements(), page.getTotalPages());
    }

    /** Para un Page de entidades: convierte cada una a su DTO (las entidades nunca salen del backend). */
    public static <E, T> PageResponse<T> from(Page<E> page, Function<E, T> mapper) {
        return from(page.map(mapper));
    }
}
