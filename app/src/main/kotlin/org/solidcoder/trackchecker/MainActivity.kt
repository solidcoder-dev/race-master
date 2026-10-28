package org.solidcoder.trackchecker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import org.solidcoder.trackchecker.routes.infrastructure.RoutesContainer
import org.solidcoder.trackchecker.routes.ui.RoutesApp

class MainActivity : ComponentActivity() {
    private val container by lazy { RoutesContainer(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    RoutesApp(container.useCases, container.fileReader::read)
                }
            }
        }
    }
}
