package com.toasterofbread.spectre.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.toasterofbread.composekit.settings.ui.Theme
import com.toasterofbread.spectre.LocalTheme

@Composable
fun PermissionRequestButton(permission_name: String, modifier: Modifier = Modifier, onGrantRequested: () -> Unit) {
    val theme: Theme = LocalTheme.current
    val shape: Shape = RoundedCornerShape(20.dp)

    Column(
        modifier
            .background(theme.accent, shape)
            .width(IntrinsicSize.Max)
            .border(2.dp, theme.vibrant_accent, shape)
            .padding(15.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Missing $permission_name permission", color = theme.on_accent)

        Button(
            { onGrantRequested() },
            Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = theme.vibrant_accent,
                contentColor = theme.on_accent
            )
        ) {
            Text("Grant permission")
        }
    }
}
