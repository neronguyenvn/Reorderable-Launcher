package com.example.customlauncher.core.common.util

fun <L, R> Map<L, List<R>>.mergeWith(map: Map<L, List<R>>): Map<L, List<R>> {
    val result = mutableMapOf<L, MutableList<R>>()
    result.putAll(this.mapValues { it.value.toMutableList() })

    map.forEach { (key, values) ->
        result.merge(key, values.toMutableList()) { existing, new ->
            existing.apply { addAll(new) }
        }
    }

    return result
}