package com.savit.card.mapper;

import com.savit.card.domain.Card;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CardMapper {
    void insertCards(@Param("cards") List<Card> cards);
    Card selectCardByIdAndUserId(@Param("cardId") Long cardId, @Param("userId") Long userId);
    int selectMonthlyUsageAmount(@Param("cardId") Long cardId);
    List<Card> selectCardsByUserId(Long userId);
    boolean existsCardByResCardNoAndUserId(@Param("resCardNo") String resCardNo, @Param("userId") Long userId);
    Card findFirstCardByUserId(@Param("userId") Long userId);
    int updateCardName(@Param("cardId") Long cardId,
                       @Param("userId") Long userId,
                       @Param("cardName") String cardName);

    Card selectCardBasicByIdAndUserId(@Param("cardId") Long cardId,
                                      @Param("userId") Long userId);
}