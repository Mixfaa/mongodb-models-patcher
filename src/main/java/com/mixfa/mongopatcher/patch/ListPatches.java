package com.mixfa.mongopatcher.patch;

import com.mixfa.mongopatcher.Patch;
import com.mixfa.mongopatcher.processor.annotations.FieldNameParam;
import com.mixfa.mongopatcher.processor.annotations.IgnoreMethod;
import com.mixfa.mongopatcher.processor.annotations.ParameterizeByParameterizedType;
import com.mixfa.mongopatcher.processor.annotations.TakeParameterizedType;

import java.util.List;

public class ListPatches {
    public static <CT> Patch<CT> clear(Patch<CT> update, @FieldNameParam String field) {
        update.set(field, List.of());
        return update;
    }

    public static <CT> Patch<CT> set(Patch<CT> update, @FieldNameParam String field, @ParameterizeByParameterizedType Object[] value) {
        update.set(field, value);
        return update;
    }

    public static <CT, T> Patch<CT> set(Patch<CT> update, @FieldNameParam String field, @ParameterizeByParameterizedType Iterable<T> value) {
        update.set(field, value);
        return update;
    }

    @IgnoreMethod
    public static <CT> Patch<CT> set(Patch<CT> update, @FieldNameParam String field, long[] value) {
        update.set(field, value);
        return update;
    }

    @IgnoreMethod
    public static <CT> Patch<CT> set(Patch<CT> update, @FieldNameParam String field, int[] value) {
        update.set(field, value);
        return update;
    }

    @IgnoreMethod
    public static <CT> Patch<CT> set(Patch<CT> update, @FieldNameParam String field, float[] value) {
        update.set(field, value);
        return update;
    }

    @IgnoreMethod
    public static <CT> Patch<CT> set(Patch<CT> update, @FieldNameParam String field, double[] value) {
        update.set(field, value);
        return update;
    }

    @IgnoreMethod
    public static <CT> Patch<CT> set(Patch<CT> update, @FieldNameParam String field, char[] value) {
        update.set(field, value);
        return update;
    }

    @IgnoreMethod
    public static <CT> Patch<CT> set(Patch<CT> update, @FieldNameParam String field, byte[] value) {
        update.set(field, value);
        return update;
    }

    @IgnoreMethod
    public static <CT> Patch<CT> set(Patch<CT> update, @FieldNameParam String field, boolean[] value) {
        update.set(field, value);
        return update;
    }

    public static <CT, T> Patch<CT> addToSet(Patch<CT> update, @FieldNameParam String field, @TakeParameterizedType T value) {
        update.addToSet(field, value);
        return update;
    }

    public static <CT, T> Patch<CT> push(Patch<CT> update, @FieldNameParam String field, @TakeParameterizedType T value) {
        update.push(field, value);
        return update;
    }

    public static <CT, T> Patch<CT> pull(Patch<CT> update, @FieldNameParam String field, @TakeParameterizedType T value) {
        update.push(field, value);
        return update;
    }
}
