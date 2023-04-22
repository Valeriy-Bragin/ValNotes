package com.meriniguan.notepad.utils

import java.io.Serializable
import kotlin.Triple

public data class Quadruple<out A, out B, out C, out D>(
    public val first: A,
    public val second: B,
    public val third: C,
    public val fourth: D
) : Serializable {

    public override fun toString(): String = "($first, $second, $third, $fourth)"
}