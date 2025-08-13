package com.savit.card.dto;

import lombok.Getter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Getter
public class CardRenameRequestDTO {
    @NotBlank
    @Size(max = 50)
    private String cardName;
}