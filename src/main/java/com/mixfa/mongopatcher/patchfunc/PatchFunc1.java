package com.mixfa.mongopatcher.patchfunc;

import com.mixfa.mongopatcher.Patch;

public interface PatchFunc1<CT,T> {
    Patch<CT> apply(Patch<CT> p, T param);
}