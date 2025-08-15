package com.mixfa.mongopatcher.patch;


import com.mixfa.mongopatcher.Patch;
import com.mixfa.mongopatcher.processor.annotations.FieldNameParam;

public class ValuePatches {
    public static <CT, T> Patch<CT> set(Patch<CT> update, @FieldNameParam String field, T value) {
        update.set(field, value);
        return update;
    }
}

