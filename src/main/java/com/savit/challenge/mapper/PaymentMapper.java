package com.savit.challenge.mapper;

import com.savit.challenge.domain.PaymentVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;

@Mapper
public interface PaymentMapper {
    void insertPending(@Param("merchantUid") String merchantUid,
                       @Param("userId") Long userId,
                       @Param("challengeId") Long challengeId,
                       @Param("amount") Long amount); // BIGINT

    PaymentVO findByMerchantUid(@Param("merchantUid") String merchantUid);

    PaymentVO findByMerchantUidForUpdate(@Param("merchantUid") String merchantUid);

    void markSuccess(@Param("merchantUid") String merchantUid,
                     @Param("impUid") String impUid,
                     @Param("paidAt") Date paidAt);
}