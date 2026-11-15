package ar.edu.usal.mgnutri.common.crypto;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** Convierte columnas marcadas como sensibles: se guardan cifradas con AES-256-GCM. */
@Converter
public class CryptoConverter implements AttributeConverter<String, String> {

    @Override
    public String convertToDatabaseColumn(String atributo) {
        return atributo == null || atributo.isEmpty() ? null : CryptoService.instancia().cifrar(atributo);
    }

    @Override
    public String convertToEntityAttribute(String columna) {
        return columna == null ? null : CryptoService.instancia().descifrar(columna);
    }
}
