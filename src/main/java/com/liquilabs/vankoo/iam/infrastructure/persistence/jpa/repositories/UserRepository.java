package com.liquilabs.vankoo.iam.infrastructure.persistence.jpa.repositories;

import com.liquilabs.vankoo.iam.domain.model.aggregates.User;
import lombok.extern.java.Log;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Log> {

}
