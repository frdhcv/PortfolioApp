package com.example.portfolio.unitofwork;

import com.example.portfolio.Repository.ProjectRepository;
import com.example.portfolio.Repository.UserRepository;

public interface UnitOfWork {
    ProjectRepository getProjectRepository();
    UserRepository getUserRepository();
    void commit();
    void rollback();
} 