package com.company.user.infrastructure.persistence;

import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
public class UserDataObject {

    private String id;
    private String name;
    private String email;
    private String status;
    private OffsetDateTime createdAt;
}
