package com.savit.card.domain;

import lombok.Data;

import java.util.Date;

@Data
public class CardTransactionVO {
    private Long id;
    private Long cardId;
    private String resCardNo;
    private String resUsedDate;
    private String resUsedTime;
    private String resUsedAmount;
    private String resCancelYn;
    private String resCancelAmount;
    private String resTotalAmount;
    private Long budgetCategoryId;
    private Long categoryId;
    private String resMemberStoreName;
    private String resMemberStoreType;
    private Date createdAt;
    private Date updatedAt;
}