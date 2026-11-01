package com.beeeam.intervaltimer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import com.beeeam.feature.main.MainScreen
import com.slack.circuit.foundation.CircuitCompositionLocals
import com.slack.circuit.foundation.CircuitContent

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val circuit = (application as IntervalTimerApplication).appGraph.circuit
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
