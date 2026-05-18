package com.lumenml.api.dto;

import jakarta.validation.constraints.Size;

public record ProjectPatchRequest(@Size(max = 200) String name, @Size(max = 4000) String description) {}
