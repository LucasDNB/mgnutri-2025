package ar.edu.usal.mgnutri.common;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.function.Function;

/** Respuesta paginada estándar (docs/04-convenciones-api.md). */
public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    public static <E, T> PageResponse<T> de(Page<E> pagina, Function<E, T> mapper) {
        return new PageResponse<>(pagina.getContent().stream().map(mapper).toList(), pagina.getNumber(),
                pagina.getSize(), pagina.getTotalElements(), pagina.getTotalPages());
    }

    public static <T> PageResponse<T> de(List<T> todos, int page, int size) {
        int desde = Math.min(page * size, todos.size());
        int hasta = Math.min(desde + size, todos.size());
        int paginas = size == 0 ? 0 : (int) Math.ceil(todos.size() / (double) size);
        return new PageResponse<>(todos.subList(desde, hasta), page, size, todos.size(), paginas);
    }

    /** Normaliza page/size (size máximo 100). */
    public static Pageable pageable(Integer page, Integer size, Sort sort) {
        int p = page == null || page < 0 ? 0 : page;
        int s = size == null || size <= 0 ? 20 : Math.min(size, 100);
        return PageRequest.of(p, s, sort == null ? Sort.unsorted() : sort);
    }
}
