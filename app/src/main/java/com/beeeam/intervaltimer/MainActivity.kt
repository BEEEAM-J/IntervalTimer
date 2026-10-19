package com.beeeam.intervaltimer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import com.beeeam.feature.main.MainScreen
import com.beeeam.feature.addFeatureScreens
import com.slack.circuit.foundation.Circuit
import com.slack.circuit.foundation.CircuitCompositionLocals
import com.slack.circuit.foundation.CircuitContent

class MainActivity : ComponentActivity() {

    private val circuit: Circuit = Circuit.Builder()
        .addFeatureScreens()
        .build()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                CircuitCompositionLocals(circuit) {
                    CircuitContent(MainScreen)
                }
            }
        }
    }
}
