package me.zeroeightsix.bot.util

import org.cache2k.Cache2kBuilder

// I dislike files like these, but sometimes you've got to bite the bullet

/// Less ugly-ish way of constructing a Cache2kBuilder
inline fun <reified K, reified V> cache() =
    Cache2kBuilder.of(K::class.java, V::class.java)