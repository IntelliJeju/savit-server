package com.savit.budget.controller;

import com.savit.budget.domain.BudgetCategoryVO;
import com.savit.budget.domain.BudgetVO;
import com.savit.budget.dto.BudgetCategoryDTO;
import com.savit.budget.dto.BudgetCategorySearchDTO;
import com.savit.budget.dto.BudgetDTO;
import com.savit.budget.service.BudgetCategoryService;
import com.savit.budget.service.BudgetService;
import com.savit.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/api/budget")
@RequiredArgsConstructor
@Slf4j
public class BudgetController {
    private final BudgetService budgetService;
    private final BudgetCategoryService budgetCategoryService;
    private final JwtUtil jwtUtil;

    @PostMapping
    public ResponseEntity<Void> createBudget(@RequestBody BudgetDTO dto, HttpServletRequest request) {
        Long userId = jwtUtil.getUserIdFromToken(request);
        budgetService.createBudget(dto, userId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PutMapping
    public ResponseEntity<Void> changeBudget(@RequestBody BudgetDTO dto, HttpServletRequest request) {
        Long userId = jwtUtil.getUserIdFromToken(request);
        int result = budgetService.changeBudget(dto, userId);
        if ( result != 1 ) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public ResponseEntity<BudgetVO> getBudget(HttpServletRequest request) {
        Long userId = jwtUtil.getUserIdFromToken(request);
        BudgetVO result = budgetService.getBudget(userId);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/categories")
    public ResponseEntity<Void> createBudgetCategory(@RequestBody List<BudgetCategoryDTO> categories, HttpServletRequest request) {
        Long userId = jwtUtil.getUserIdFromToken(request);
        budgetCategoryService.createBudgetCategory(categories,userId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PutMapping("/categories")
    public ResponseEntity<Void> changeBudgetCategory(@RequestBody List<BudgetCategoryDTO> categories, HttpServletRequest request) {
        Long userId = jwtUtil.getUserIdFromToken(request);
        int result = budgetCategoryService.changeBudgetCategory(categories,userId);
        if ( result ==0 ){
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok().build();
    }

    // 조회
    @PostMapping("/categories/list")
    public ResponseEntity<Map<String, List<BudgetCategoryVO>>> getBudgetCategories(
            @RequestBody BudgetCategorySearchDTO searchDTO,
            HttpServletRequest request) {
        Long userId = jwtUtil.getUserIdFromToken(request);
        Map<String, List<BudgetCategoryVO>> result = budgetCategoryService.getBudgetCategories(
                userId, searchDTO.getMonths(), searchDTO.getCategoryIds());
        return ResponseEntity.ok(result);
    }

    // 사용자 + categoryId 기준 또래 월평균 금액(없으면 0)
    @GetMapping("/peer-avg/{categoryId}")
    public ResponseEntity<Integer> getMyPeerAvgByCategoryId(@PathVariable Long categoryId,
                                                            HttpServletRequest request) {
        Long userId = jwtUtil.getUserIdFromToken(request);
        int amount = budgetService.getPeerAvgForUserAndCategory(userId, categoryId);
        return ResponseEntity.ok(amount); // 없으면 0
    }
}

