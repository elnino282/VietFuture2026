package org.example.QuanLyMuaVu.module.shared.pattern.Factory;


/**
 * Factory Method Pattern: Entity Factory Interface.
 * <p>
 * Defines the contract for creating domain entities from request DTOs.
 * Encapsulates entity construction logic including:
 * - Default value assignment
 * - Initial status setting
 * - Audit field population (createdBy, createdAt)
 * <p>
 * Benefits:
 * - Centralizes entity construction logic
 * - Ensures consistent entity initialization
 * - Easy to add new entity types
 * - Simplifies testing through factory injection
 *
 * @param <E> The entity type to create
 * @param <R> The request DTO type
 */
public interface EntityFactory<E, R> {

    /**
     * Creates a new entity from the request DTO.
     *
     * @param request The creation request
     * @param creator The user creating the entity
     * @return The newly created entity (not yet persisted)
     */
    E create(R request, org.example.QuanLyMuaVu.module.identity.entity.User creator);
}
