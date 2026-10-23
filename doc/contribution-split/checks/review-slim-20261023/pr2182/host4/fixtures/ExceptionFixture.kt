package com.osfans.trime.util
fun errorState(messageTemplate: Int): Nothing = throw IllegalStateException("message=$messageTemplate")
