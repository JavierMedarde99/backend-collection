package com.wikicollection.domain.model;

/**
 * Propietario de una imagen subida al hosting externo (Catbox). El nombre del
 * archivo es la clave: Catbox genera nombres únicos y el registro es lo que
 * permite exigir que solo su propietario —o un admin— pueda borrarla (#481).
 *
 * <p>Sin este registro, el servicio delegaba el borrado en el cliente externo y
 * cualquier usuario autenticado podía borrar cualquier imagen de la colección.
 */
public record ImageOwnership(String filename, String ownerId, String ownerName) {
}
