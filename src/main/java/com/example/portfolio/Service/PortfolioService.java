package com.example.portfolio.Service;

import com.example.portfolio.unitofwork.UnitOfWork;
import com.example.portfolio.unitofwork.UnitOfWorkFactory;
import com.example.portfolio.entity.PortfolioEntity;
import com.example.portfolio.entity.UserEntity;
import org.springframework.stereotype.Service;

@Service
public class PortfolioService {

    private final UnitOfWorkFactory unitOfWorkFactory;

    public PortfolioService(UnitOfWorkFactory unitOfWorkFactory) {
        this.unitOfWorkFactory = unitOfWorkFactory;
    }

    public PortfolioEntity getPortfolioForUser(UserEntity user) {
        UnitOfWork uow = unitOfWorkFactory.create();
        try {
            return uow.getUserRepository().findById(user.getId())
                    .map(UserEntity::getPortfolio)
                    .orElse(null);
        } finally {
            uow.commit();
        }
    }

    public PortfolioEntity getPortfolioByUserId(Long userId) {
        UnitOfWork uow = unitOfWorkFactory.create();
        try {
            UserEntity user = uow.getUserRepository().findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            PortfolioEntity portfolio = user.getPortfolio();

            if (portfolio == null) {
                throw new RuntimeException("This user has no portfolio.");
            }

            return portfolio;
        } finally {
            uow.commit();
        }
    }
}
