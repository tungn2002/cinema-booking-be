package com.personal.cinemabooking.repositories;

import com.personal.cinemabooking.entities.ComponentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

// repo for component types (used for master data/lookup values)
@Repository
public interface ComponentTypeRepository extends JpaRepository<ComponentType, Integer> {
    // find component type by name
    Optional<ComponentType> findByName(String name); // for lookups
}
