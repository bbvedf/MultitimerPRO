package com.android.multitimerpro.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.android.multitimerpro.R

@Composable
fun translateCategory(internalName: String): String {
    return when(internalName.uppercase()) {
        "ALL" -> stringResource(R.string.category_all)
        "GENERAL" -> stringResource(R.string.cat_general)
        "WORK" -> stringResource(R.string.cat_work)
        "LEISURE" -> stringResource(R.string.cat_leisure)
        "OTHERS" -> stringResource(R.string.cat_other)
        else -> internalName
    }
}
