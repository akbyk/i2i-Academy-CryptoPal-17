package com.akbyk.cryptopal.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AiQueryRequestDto {

    @NotBlank
    @Size(max = 1000, message = "message must be 1000 characters or fewer")
    private String message;
}