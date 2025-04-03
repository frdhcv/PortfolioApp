package com.example.portfolio.unitofwork;

import com.example.portfolio.Repository.ProjectRepository;
import com.example.portfolio.Repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class UnitOfWorkImpl implements UnitOfWork {
    private final EntityManager entityManager;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private EntityTransaction transaction;

    public UnitOfWorkImpl(EntityManager entityManager, 
                         ProjectRepository projectRepository, 
                         UserRepository userRepository) {
        this.entityManager = entityManager;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
    }

    @Override
    public ProjectRepository getProjectRepository() {
        return projectRepository;
    }

    @Override
    public UserRepository getUserRepository() {
        return userRepository;
    }

    @Override
    @Transactional
    public void commit() {
        if (transaction != null && transaction.isActive()) {
            transaction.commit();
        }
    }

    @Override
    public void rollback() {
        if (transaction != null && transaction.isActive()) {
            transaction.rollback();
        }
    }

    public void beginTransaction() {
        if (transaction == null || !transaction.isActive()) {
            transaction = entityManager.getTransaction();
            transaction.begin();
        }
    }
} 