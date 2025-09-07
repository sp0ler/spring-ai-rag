package ru.deevdenis.ai.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class StoredDocument {

    @Builder.Default
    private String text = "";

    @Builder.Default
    private LocalDateTime createdTime = LocalDateTime.now();

    public static StoredDocument valueOf(String text) {
        return StoredDocument.builder().text(text).build();
    }
}
