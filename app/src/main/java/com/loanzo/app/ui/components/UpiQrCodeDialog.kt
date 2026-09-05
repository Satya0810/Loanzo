package com.loanzo.app.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.loanzo.app.ui.theme.*
import com.loanzo.app.util.UpiHelper
import com.loanzo.app.util.toInrString
import java.net.URLEncoder

/**
 * Dynamic UPI Scan & Pay QR Code Dialog.
 * Inspired by Revolut, PhonePe & Google Pay P2P settlements.
 */
@Composable
fun UpiQrCodeDialog(
    payeeName: String,
    payeeUpiId: String,
    amount: Double,
    loanPurpose: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    // Standard UPI Specification URI
    val upiUri = remember(payeeUpiId, payeeName, amount, loanPurpose) {
        "upi://pay?pa=${payeeUpiId.trim()}&pn=${URLEncoder.encode(payeeName.trim(), "UTF-8")}&am=${String.format(java.util.Locale.US, "%.2f", amount)}&cu=INR&tn=${URLEncoder.encode("Loanzo: $loanPurpose", "UTF-8")}"
    }

    // QR Code API image URL with dark navy pattern on crisp white background
    val qrCodeUrl = remember(upiUri) {
        "https://api.qrserver.com/v1/create-qr-code/?size=360x360&data=${URLEncoder.encode(upiUri, "UTF-8")}&color=0A1628&bgcolor=FFFFFF&margin=1"
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = Navy800),
            border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = Emerald400.copy(alpha = 0.15f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Default.QrCodeScanner,
                                contentDescription = null,
                                tint = Emerald400,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Scan & Pay UPI",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, "Close", tint = Gray400)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // High-Contrast QR Code Surface
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color.White,
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .size(230.dp)
                        .padding(4.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = qrCodeUrl,
                            contentDescription = "UPI QR Code",
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Payment Info
                Text(
                    text = amount.toInrString(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Gold500
                )

                Text(
                    text = "Paying to $payeeName",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Navy900,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text(
                        text = payeeUpiId,
                        style = MaterialTheme.typography.labelSmall,
                        color = Gray300,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Open UPI App
                    Button(
                        onClick = {
                            if (UpiHelper.isUpiAvailable(context)) {
                                val intent = UpiHelper.createPaymentIntent(
                                    payeeUpiId = payeeUpiId,
                                    payeeName = payeeName,
                                    amount = amount,
                                    transactionNote = "Loanzo: $loanPurpose"
                                )
                                try {
                                    if (context !is android.app.Activity) {
                                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Could not open UPI app: ${e.localizedMessage ?: "Not available"}", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Toast.makeText(context, "No UPI app found on device", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Emerald400, contentColor = Navy900)
                    ) {
                        Icon(Icons.Default.Payment, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Pay via App", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    // Share Link
                    OutlinedButton(
                        onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "Loanzo Repayment Request")
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "Please pay ${amount.toInrString()} for '$loanPurpose' using UPI: $upiUri"
                                )
                            }
                            try {
                                val chooser = Intent.createChooser(shareIntent, "Share Payment Request")
                                if (context !is android.app.Activity) {
                                    chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(chooser)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Unable to share payment request", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)
                    ) {
                        Icon(Icons.Default.Share, null, modifier = Modifier.size(16.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share Link", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Color.White)
                    }
                }
            }
        }
    }
}
