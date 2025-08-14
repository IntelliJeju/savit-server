package com.savit.challenge.domain;

import lombok.Getter;
import lombok.Setter;
import java.util.Date;

@Getter
@Setter
public class PointVO {
    private Long userId;
    private Long amount;
    private Date createdAt;
    private Date updatedAt;
}