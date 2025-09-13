package com.mixfa.mongopatcher.processor;

import java.util.List;
import java.util.Optional;

public record PatchClassMakingSettings(List<PatchClassMakingSetting> settings) {
    private static final PatchClassMakingSettings EMPTY = new PatchClassMakingSettings();

    public static PatchClassMakingSettings empty() {
        return EMPTY;
    }

    public PatchClassMakingSettings(PatchClassMakingSetting... settings) {
        this(List.of(settings));
    }

    public <T extends PatchClassMakingSetting> Optional<T> findFirstByType(Class<T> type) {
        return (Optional<T>) settings.stream().filter(type::isInstance).findFirst();
    }
}
