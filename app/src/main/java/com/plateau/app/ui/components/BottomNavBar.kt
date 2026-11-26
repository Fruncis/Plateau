package com.plateau.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.plateau.app.ui.navigation.Destination
import com.plateau.app.ui.theme.Size
import com.plateau.app.ui.theme.Spacing

@Composable
fun BottomNavBar(
    currentDestination: Destination,
    onDestinationClick: (Destination) -> Unit
) {
    val destinations = listOf(
        Destination.Home,
        Destination.Session,
        Destination.Plans,
        Destination.Discover,
        Destination.Progress,
        Destination.Photo
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(Size.tabBarHeight)
            .padding(bottom = Spacing.md),
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.large,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            destinations.forEach { destination ->
                BottomNavItem(
                    destination = destination,
                    isSelected = (currentDestination == destination),
                    onClick = { onDestinationClick(destination) }
                )
            }
        }
    }
}

@Composable
private fun BottomNavItem(
    destination: Destination,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val icon = when (destination) {
        Destination.Home -> Event
        Destination.Session -> FitnessCenter
        Destination.Plans -> Description
        Destination.Discover -> Search
        Destination.Progress -> TrendingUp
        Destination.Photo -> CameraAlt
    }

    val color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant

    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(Size.tabBarHeight)
            .fillMaxSize()
            .padding(Spacing.sm)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = destination.label,
                modifier = Modifier.size(24.dp),
                tint = color
            )
            Text(
                text = destination.label,
                style = MaterialTheme.typography.labelSmall,
                color = color
            )
        }
    }
}