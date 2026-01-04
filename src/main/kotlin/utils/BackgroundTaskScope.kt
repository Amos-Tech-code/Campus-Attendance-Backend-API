package com.amos_tech_code.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class BackgroundTaskScope {
    val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO
    )
}