package com.toasterofbread.spectre.ui.layout

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.toasterofbread.toastercomposetools.settings.ui.Theme

interface AppPage {
    @Composable
    fun Page(theme: Theme, modifier: Modifier)
}
