package com.loanzo.app.ui.loan

import androidx.compose.ui.res.stringResource
import com.loanzo.app.R

import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.Path as AndroidPath
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalESignScreen(
    onSignatureCaptured: (Bitmap?) -> Unit,
    onCancel: () -> Unit
) {
    var path by remember { mutableStateOf(Path()) }
    var androidPath by remember { mutableStateOf(AndroidPath()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.sign_agreement)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        bottomBar = {
            BottomAppBar {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onCancel) {
                        Text(stringResource(R.string.cancel))
                    }
                    Button(
                        onClick = {
                            path = Path()
                            androidPath = AndroidPath()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.clear))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.clear))
                    }
                    Button(
                        onClick = {
                            // Create Bitmap from AndroidPath
                            val bitmap = Bitmap.createBitmap(800, 400, Bitmap.Config.ARGB_8888)
                            val canvas = android.graphics.Canvas(bitmap)
                            canvas.drawColor(android.graphics.Color.WHITE)
                            val paint = Paint().apply {
                                color = android.graphics.Color.BLACK
                                style = Paint.Style.STROKE
                                strokeWidth = 10f
                                strokeJoin = Paint.Join.ROUND
                                strokeCap = Paint.Cap.ROUND
                                isAntiAlias = true
                            }
                            canvas.drawPath(androidPath, paint)
                            onSignatureCaptured(bitmap)
                        }
                    ) {
                        Icon(Icons.Default.Done, contentDescription = stringResource(R.string.save))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.save_signature))
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                stringResource(R.string.please_sign_below_to_authorize_the_loan_agreement),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                val newPath = Path().apply { addPath(path) }
                                newPath.moveTo(offset.x, offset.y)
                                path = newPath

                                val newAndroidPath = AndroidPath(androidPath)
                                newAndroidPath.moveTo(offset.x, offset.y)
                                androidPath = newAndroidPath
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                val newPath = Path().apply { addPath(path) }
                                newPath.lineTo(change.position.x, change.position.y)
                                path = newPath

                                val newAndroidPath = AndroidPath(androidPath)
                                newAndroidPath.lineTo(change.position.x, change.position.y)
                                androidPath = newAndroidPath
                            }
                        )
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawPath(
                        path = path,
                        color = Color.Black,
                        style = Stroke(
                            width = 10f,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }
            }
        }
    }
}
