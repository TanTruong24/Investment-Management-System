package com.gostock.repository;

import com.gostock.entity.Broker;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BrokerRepository extends JpaRepository<Broker, Long> {
    Optional<Broker> findByCodeIgnoreCase(String code);
}
