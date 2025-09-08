package com.mixfa.mongopatcher.processor;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PatchClassMakingSettings {
    private static final PatchClassMakingSettings EMPTY = new PatchClassMakingSettings();

    public static PatchClassMakingSettings empty() {
        return EMPTY;
    }

    private final List<PatchClassMakingSetting> settings;

    public PatchClassMakingSettings(PatchClassMakingSetting... settings) {
        this.settings = List.of(settings);
    }

    public <T extends PatchClassMakingSetting> Optional<T> findFirstByType(Class<T> type) {
        return (Optional<T>) settings.stream().filter(type::isInstance).findFirst();
    }
}
