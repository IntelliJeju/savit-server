package com.savit.budget.mapper;

import com.savit.budget.domain.CategoryVO;
import org.apache.ibatis.annotations.Param;

public interface CategoryMapper {
    CategoryVO findById(Long categoryId);
    CategoryVO findByName(String name);
    Integer findPeerAvgByAgeGroupAndCategoryId(@Param("ageGroup") String ageGroup,
                                               @Param("categoryId") Long categoryId);
}
