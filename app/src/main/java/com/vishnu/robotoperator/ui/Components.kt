package com.vishnu.robotoperator.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.vishnu.robotoperator.model.AnnotationType
import com.vishnu.robotoperator.viewmodel.RoomState
import com.vishnu.robotoperator.viewmodel.RoomViewModel

@Composable
fun RoomControlPanel(
    modifier: Modifier = Modifier,
    roomStateState: State<RoomState>,
    roomViewModel: RoomViewModel,
) {
    val roomState = roomStateState.value

    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface, tonalElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Edit Mode",
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = roomState.isEditMode,
                        onCheckedChange = { roomViewModel.setEditMode(it) }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Annotation Type",
                    style = MaterialTheme.typography.bodyLarge
                )

                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    AnnotationTypeItem(
                        type = AnnotationType.SPRAY_AREA,
                        isSelected = roomState.selectedAnnotationType == AnnotationType.SPRAY_AREA,
                        onSelected = { roomViewModel.setAnnotationType(it) }
                    )

                    AnnotationTypeItem(
                        type = AnnotationType.SAND_AREA,
                        isSelected = roomState.selectedAnnotationType == AnnotationType.SAND_AREA,
                        onSelected = { roomViewModel.setAnnotationType(it) }
                    )

                    AnnotationTypeItem(
                        type = AnnotationType.OBSTACLE,
                        isSelected = roomState.selectedAnnotationType == AnnotationType.OBSTACLE,
                        onSelected = { roomViewModel.setAnnotationType(it) }
                    )
                }
            }
        }
    }
}

@Composable
fun AnnotationTypeItem(
    type: AnnotationType,
    isSelected: Boolean,
    onSelected: (AnnotationType) -> Unit
) {
    val backgroundColor = when (type) {
        AnnotationType.SPRAY_AREA -> Color.Red
        AnnotationType.SAND_AREA -> Color.Green
        AnnotationType.OBSTACLE -> Color.Blue
    }

    val textColor = MaterialTheme.colorScheme.onSurface

    Row(
        modifier = Modifier
            .padding(8.dp)
            .background(backgroundColor)
            .border(
                width = 2.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
            )
            .clickable { onSelected(type) }
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Rounded.Settings,
            contentDescription = type.name,
            tint = textColor
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = type.name.replace("_", " ").replace("AREA", "").trim(),
            color = textColor
        )
    }
}