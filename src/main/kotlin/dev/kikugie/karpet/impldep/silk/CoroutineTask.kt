/*
Sourced from https://github.com/SilkMC/silk/blob/main/silk-core/src/main/kotlin/net/silkmc/silk/core/task/CoroutineTask.kt,
which is licensed under GPL v3.0.

Modifications:
- Changed package to 'dev.kikugie.karpet.impldep.silk'.
- Renamed 'silkCoroutineScope' to 'KARPET_COROUTINE_SCOPE' for local use.
- Removed unused functions.
 */

package dev.kikugie.karpet.impldep.silk

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

val KARPET_COROUTINE_SCOPE = CoroutineScope(SupervisorJob() + Dispatchers.Default)