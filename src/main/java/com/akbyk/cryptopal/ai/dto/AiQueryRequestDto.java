package com.akbyk.cryptopal.ai.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AiQueryRequestDto {

    @NotBlank
    private String message;
}