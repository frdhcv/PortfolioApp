package com.example.portfolio.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PortfolioRepository extends JpaRepository<com.example.portfolio.entity.PortfolioEntity, Long> {
}
