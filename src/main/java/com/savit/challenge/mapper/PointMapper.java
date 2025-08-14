package com.savit.challenge.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PointMapper {
    void ensureRow(@Param("userId") Long userId);
    void add(@Param("userId") Long userId, @Param("delta") Long delta);
    Long getAmount(@Param("userId") Long userId);
}
