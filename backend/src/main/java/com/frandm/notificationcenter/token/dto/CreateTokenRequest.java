package com.frandm.notificationcenter.token.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Set;

public record CreateTokenRequest(
        @NotBlank @Size(max = 100)
        String name,

        @NotBlank @Size(max = 100)
        @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "source must contain only alphanumeric characters, dashes, or underscores")
        String source,

        @NotEmpty
        Set<String> scopes
) {
}
