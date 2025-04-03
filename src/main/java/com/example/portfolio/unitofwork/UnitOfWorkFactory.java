package com.example.portfolio.unitofwork;

import org.springframework.stereotype.Component;

@Component
public class UnitOfWorkFactory {
    private final UnitOfWorkImpl unitOfWork;

    public UnitOfWorkFactory(UnitOfWorkImpl unitOfWork) {
        this.unitOfWork = unitOfWork;
    }

    public UnitOfWork create() {
        unitOfWork.beginTransaction();
        return unitOfWork;
    }
} 