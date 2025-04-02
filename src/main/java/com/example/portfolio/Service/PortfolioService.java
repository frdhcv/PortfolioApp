package com.example.portfolio.Service;

import com.example.portfolio.Repository.UserRepository;
import com.example.portfolio.entity.PortfolioEntity;
import com.example.portfolio.entity.UserEntity;
import org.springframework.stereotype.Service;

@Service
public class PortfolioService {

    private final UserRepository userRepository;

    public PortfolioService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public PortfolioEntity getPortfolioForUser(UserEntity user) {
        return userRepository.findById(user.getId())
                .map(UserEntity::getPortfolio)
                .orElse(null);
    }
    public PortfolioEntity getPortfolioByUserId(Long userId) {
        UserEntity user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        PortfolioEntity portfolio = user.getPortfolio();

        if (portfolio == null) {
            throw new RuntimeException("This user has no portfolio.");
        }

        return portfolio;
    }

}
