package com.savit.budget.service;

import com.savit.budget.domain.BudgetVO;
import com.savit.budget.dto.BudgetDTO;

public interface BudgetService {
    void createBudget(BudgetDTO dto, Long userId);
    int changeBudget(BudgetDTO dto, Long userId);
    BudgetVO getBudget(Long userId);
    int getPeerAvgForUserAndCategory(Long userId, Long categoryId);
}
