package com.main.triviatreckapp.repository;

import com.main.triviatreckapp.entities.Role;
import com.main.triviatreckapp.entities.RoleEnum;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends CrudRepository<Role, Integer> {
    Optional<Role> findByName(RoleEnum name);
}
