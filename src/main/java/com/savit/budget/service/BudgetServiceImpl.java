package com.savit.budget.service;

import com.savit.budget.domain.BudgetVO;
import com.savit.budget.dto.BudgetDTO;
import com.savit.budget.mapper.BudgetMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.savit.budget.mapper.CategoryMapper;
import com.savit.user.domain.User;
import com.savit.user.service.UserService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.Period;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class BudgetServiceImpl implements BudgetService{

    private final BudgetMapper budgetMapper;
    private final CategoryMapper categoryMapper;
    private final UserService userService;

    @Override
    public void createBudget(BudgetDTO dto, Long userId) {
        String currentMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"));

        BudgetVO vo= BudgetVO.builder()
                .userId(userId)
                .month(currentMonth)
                .totalBudget(dto.getTotalBudget())
                .build();

        budgetMapper.insertBudget(vo);
    }

    @Override
    public int changeBudget(BudgetDTO budgetDTO, Long userId) {
        String currentMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
        BudgetVO vo = BudgetVO.builder()
                .userId(userId)
                .month(currentMonth)
                .totalBudget(budgetDTO.getTotalBudget()).build();
       return budgetMapper.updateBudget(vo);
    }

    @Override
    public BudgetVO getBudget(Long userId) {
        String curMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
        return budgetMapper.getBudget(userId,curMonth);
    }

    @Override
    public int getPeerAvgForUserAndCategory(Long userId, Long categoryId) {
        User user = userService.findById(userId);
        if (user == null) return 0;

        String birth = user.getBirthDate();
        if (birth == null || birth.isBlank()) return 0;

        Optional<String> ageGroup = toAgeGroup(parseBirthDate(birth));
        if (ageGroup.isEmpty()) return 0;

        Integer amt = categoryMapper.findPeerAvgByAgeGroupAndCategoryId(ageGroup.get(), categoryId);
        return amt != null ? amt : 0;
    }

    private Optional<String> toAgeGroup(Optional<LocalDate> birthOpt) {
        if (birthOpt.isEmpty()) return Optional.empty();
        int age = Period.between(birthOpt.get(), LocalDate.now()).getYears();
        if (age < 20) return Optional.empty();
        if (age < 30) return Optional.of("20s");
        if (age < 40) return Optional.of("30s");
        return Optional.empty(); // 현재 20/30대만 시드 존재
    }

    private Optional<LocalDate> parseBirthDate(String raw) {
        for (var fmt : List.of(
                DateTimeFormatter.ofPattern("yyyy-MM-dd"),
                DateTimeFormatter.ofPattern("yyyyMMdd")
        )) {
            try { return Optional.of(LocalDate.parse(raw, fmt)); }
            catch (Exception ignore) {}
        }
        return Optional.empty();
    }
}
