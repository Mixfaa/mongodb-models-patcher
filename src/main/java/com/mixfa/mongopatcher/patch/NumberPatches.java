package com.mixfa.mongopatcher.patch;

import com.mixfa.mongopatcher.Patch;
import com.mixfa.mongopatcher.processor.annotations.FieldNameParam;

public class NumberPatches {
    public static <CT> Patch<CT> inc(Patch<CT> update, @FieldNameParam String field, Number value) {
        update.inc(field, value);
        return update;
    }
}

