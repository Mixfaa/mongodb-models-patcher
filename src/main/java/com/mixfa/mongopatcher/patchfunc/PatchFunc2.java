package com.mixfa.mongopatcher.patchfunc;

import com.mixfa.mongopatcher.Patch;

public interface PatchFunc2<CT,T1, T2> {
    Patch<CT> apply(Patch<CT> p, T1 param, T2 param2);
}
