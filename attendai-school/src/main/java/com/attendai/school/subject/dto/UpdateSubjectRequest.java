package com.attendai.school.subject.dto;

import com.attendai.school.subject.entity.SubjectType;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateSubjectRequest {

    @Size(max = 200)
    private String name;

    private SubjectType type;

    @Size(max = 500)
    private String description;
}
