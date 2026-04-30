package com.vahak.parentcontroll.core.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import com.vahak.parentcontroll.MainActivity
import com.vahak.parentcontroll.core.data.local.SessionManager
import com.vahak.parentcontroll.core.data.local.dao.WebDao
import com.vahak.parentcontroll.core.util.DnsPacketHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentHashMap // 🚀 Added for Thread-Safe Network Tracking
import javax.inject.Inject

@AndroidEntryPoint
class WebFilterVpnService : VpnService() {

    @Inject
    lateinit var sessionManager: SessionManager
    @Inject
    lateinit var webDao: WebDao

    companion object {
        private const val TAG = "WebFilterVpnService"
        const val ACTION_START = "ACTION_START_VPN"
        const val ACTION_STOP = "ACTION_STOP_VPN"

        private const val VPN_ADDRESS = "10.111.222.1"
        private const val VPN_DNS = "10.111.222.2"

        private const val NOTIFICATION_ID = 9999
        private const val CHANNEL_ID = "secure_vpn_channel"
    }

    private var vpnInterface: ParcelFileDescriptor? = null
    private val vpnScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var vpnJob: Job? = null

    // 🚀 Network State Variables
    private lateinit var connectivityManager: ConnectivityManager
    private var activeBlockedDomains: List<String> = emptyList()

    // 🚀 Data class to track the network type and its specific DNS
    data class PhysicalNetworkInfo(val isWifi: Boolean, val dns: InetAddress?)

    // 🚀 The Map that tracks all active networks simultaneously
    private val activeNetworksMap = ConcurrentHashMap<Network, PhysicalNetworkInfo>()

    // 🚀 The Bulletproof Network Tracker
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {

        override fun onLinkPropertiesChanged(network: Network, lp: android.net.LinkProperties) {
            super.onLinkPropertiesChanged(network, lp)

            // 1. Fetch capabilities ON DEMAND to avoid race conditions
            val caps = connectivityManager.getNetworkCapabilities(network) ?: return

            // 2. Ignore our own VPN network
            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) return

            val isWifi = caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
            val isCell = caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)

            if (isWifi || isCell) {
                // 3. Extract the IPv4 DNS
                val dns = lp.dnsServers.firstOrNull {
                    it.hostAddress?.contains(".") == true && it.hostAddress != VPN_DNS
                }

                // 4. Atomically insert or update the map in one single step!
                activeNetworksMap[network] = PhysicalNetworkInfo(isWifi, dns)

                val type = if (isWifi) "Wi-Fi" else "Cellular"
                Log.i(TAG, "🌐 $type DNS mapped: ${dns?.hostAddress}")
            }
        }

        override fun onLost(network: Network) {
            super.onLost(network)
            activeNetworksMap.remove(network)
            Log.i(TAG, "🔌 Network lost. Remaining active networks: ${activeNetworksMap.size}")
        }
    }

    // 🚀 The Priority Router
    private fun getBestUnderlyingDns(): InetAddress {
        // 1. Absolute Priority: If ANY Wi-Fi network is alive and has a DNS, use it!
        val wifiDns = activeNetworksMap.values.firstOrNull { it.isWifi && it.dns != null }?.dns
        if (wifiDns != null) return wifiDns

        // 2. Fallback: If no Wi-Fi, look for a Cellular DNS
        val cellDns = activeNetworksMap.values.firstOrNull { !it.isWifi && it.dns != null }?.dns
        if (cellDns != null) return cellDns

        // 3. Worst-case scenario: Both dropped, fallback to safe public DNS
        return InetAddress.getByName("8.8.8.8")
    }

    override fun onCreate() {
        super.onCreate()
        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        // Listen for Wi-Fi and Cellular network changes dynamically
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(request, networkCallback)

        // Keep the blocklist updated in memory for zero-latency packet processing
        vpnScope.launch {
            sessionManager.activeChildIdFlow.collectLatest { childId ->
                if (childId != null) {
                    webDao.observeBlockedDomains(childId).collectLatest { domains ->
                        activeBlockedDomains =
                            domains.filter { it.isActive }.map { it.domain.lowercase() }
                    }
                } else {
                    activeBlockedDomains = emptyList()
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                // Satisfy the OS 5-second deadline instantly
                createNotificationChannel()

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    startForeground(
                        NOTIFICATION_ID,
                        buildSecureNotification(),
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                    )
                } else {
                    startForeground(NOTIFICATION_ID, buildSecureNotification())
                }

                // Now it's safe to actually build the VPN tunnel
                startVpn()
            }
            ACTION_STOP -> stopVpn()
        }
        return START_STICKY
    }

    private fun startVpn() {
        if (vpnInterface != null) {
            Log.i(TAG, "VPN is already running.")
            return
        }

        try {
            val builder = Builder()
                .setSession("محافظت وب خانواده")
                .setMtu(1500)
                .addAddress(VPN_ADDRESS, 24)
                .addDnsServer(VPN_DNS)
                .addRoute(VPN_DNS, 32) // Split Tunnel

            vpnInterface = builder.establish()

            if (vpnInterface != null) {
                Log.i(TAG, "🚀 VPN Interface Established Successfully")
                startPacketProcessing()
            } else {
                Log.e(TAG, "❌ Failed to establish VPN.")
                stopVpn()
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error configuring VPN: ${e.message}")
            stopVpn()
        }
    }

    private fun startPacketProcessing() {
        vpnJob = vpnScope.launch {
            val fd = vpnInterface?.fileDescriptor ?: return@launch
            val inputStream = FileInputStream(fd)
            val outputStream = FileOutputStream(fd)
            val packet = ByteBuffer.allocate(1500)

            Log.d(TAG, "📡 Listening for DNS packets on TUN interface...")

            while (isActive) {
                // 🚀 PRO FIX: Try/Catch is INSIDE the loop.
                // A single network glitch will no longer kill the VPN!
                try {
                    val length = inputStream.read(packet.array())
                    if (length > 0) {
                        val rawPacketBytes = packet.array()
                        val requestedDomain = DnsPacketHelper.extractDomainFromPacket(rawPacketBytes, length)

                        if (requestedDomain != null) {
                            val isBlocked = activeBlockedDomains.any {
                                requestedDomain.lowercase().contains(it)
                            }

                            if (isBlocked) {
                                Log.w(TAG, "🚨 BLOCKED: Dropping packets for $requestedDomain")
                            } else {
                                Log.v(TAG, "✅ ALLOWED: $requestedDomain")
                                forwardDnsRequest(rawPacketBytes, length, outputStream)
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Log the error, but KEEP THE LOOP RUNNING!
                    if (isActive) Log.e(TAG, "Transient packet error, recovering: ${e.message}")
                } finally {
                    // 🚀 CRITICAL: Always clear the buffer so old packet data doesn't corrupt the next one
                    packet.clear()
                }
            }

            Log.w(TAG, "🛑 Packet processing loop has officially terminated.")
        }
    }

    private suspend fun forwardDnsRequest(
        rawPacketBytes: ByteArray,
        length: Int,
        outputStream: FileOutputStream
    ) {
        withContext(Dispatchers.IO) {
            var socket: DatagramSocket? = null
            try {
                val ihl = (rawPacketBytes[0].toInt() and 0x0F) * 4
                val dnsPayloadOffset = ihl + 8
                val dnsPayloadLength = length - dnsPayloadOffset

                if (dnsPayloadLength <= 0) return@withContext

                val dnsPayload = ByteArray(dnsPayloadLength)
                System.arraycopy(rawPacketBytes, dnsPayloadOffset, dnsPayload, 0, dnsPayloadLength)

                socket = DatagramSocket()

                // PUNCH A HOLE IN THE VPN to prevent the routing loop
                if (!protect(socket)) {
                    Log.e(TAG, "❌ Failed to protect the outbound socket.")
                    return@withContext
                }

                // 🚀 PRO FIX: Use the Priority Map to find the best active DNS
                val systemDns = getBestUnderlyingDns()

                val outPacket = DatagramPacket(dnsPayload, dnsPayloadLength, systemDns, 53)
                socket.send(outPacket)

                socket.soTimeout = 3000
                val responseBuffer = ByteArray(1024)
                val inPacket = DatagramPacket(responseBuffer, responseBuffer.size)
                socket.receive(inPacket)

                val responseDnsPayload = inPacket.data.copyOfRange(0, inPacket.length)
                val finalIpPacket = forgeDnsResponsePacket(rawPacketBytes, responseDnsPayload)

                outputStream.write(finalIpPacket)

            } catch (e: Exception) {
                Log.v(TAG, "DNS Forwarding timeout/error: ${e.message}")
            } finally {
                socket?.close()
            }
        }
    }

    private fun forgeDnsResponsePacket(
        originalPacket: ByteArray,
        dnsResponsePayload: ByteArray
    ): ByteArray {
        val ihl = (originalPacket[0].toInt() and 0x0F) * 4
        val totalLength = ihl + 8 + dnsResponsePayload.size
        val responsePacket = ByteArray(totalLength)

        System.arraycopy(originalPacket, 0, responsePacket, 0, ihl)

        responsePacket[2] = (totalLength shr 8).toByte()
        responsePacket[3] = (totalLength and 0xFF).toByte()

        for (i in 0..3) {
            val temp = responsePacket[12 + i]
            responsePacket[12 + i] = responsePacket[16 + i]
            responsePacket[16 + i] = temp
        }

        System.arraycopy(originalPacket, ihl, responsePacket, ihl, 8)

        val udpLength = 8 + dnsResponsePayload.size
        responsePacket[ihl + 4] = (udpLength shr 8).toByte()
        responsePacket[ihl + 5] = (udpLength and 0xFF).toByte()

        val srcPort0 = responsePacket[ihl]
        val srcPort1 = responsePacket[ihl + 1]
        responsePacket[ihl] = responsePacket[ihl + 2]
        responsePacket[ihl + 1] = responsePacket[ihl + 3]
        responsePacket[ihl + 2] = srcPort0
        responsePacket[ihl + 3] = srcPort1

        responsePacket[ihl + 6] = 0x00
        responsePacket[ihl + 7] = 0x00

        System.arraycopy(dnsResponsePayload, 0, responsePacket, ihl + 8, dnsResponsePayload.size)

        responsePacket[10] = 0x00
        responsePacket[11] = 0x00
        var checksum = 0
        for (i in 0 until ihl step 2) {
            val word =
                ((responsePacket[i].toInt() and 0xFF) shl 8) + (responsePacket[i + 1].toInt() and 0xFF)
            checksum += word
        }
        while ((checksum shr 16) > 0) {
            checksum = (checksum and 0xFFFF) + (checksum shr 16)
        }
        checksum = checksum.inv() and 0xFFFF
        responsePacket[10] = (checksum shr 8).toByte()
        responsePacket[11] = (checksum and 0xFF).toByte()

        return responsePacket
    }

    private fun buildSecureNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("سپر اینترنت فعال است")
            .setContentText("ترافیک اینترنت در حال بررسی و محافظت است.")
            .setSmallIcon(android.R.drawable.ic_secure)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "سپر امنیتی (VPN)",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "نمایش وضعیت فیلتر وب و محافظت از اینترنت"
            setShowBadge(false)
        }
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    private fun stopVpn() {
        Log.i(TAG, "🛑 Stopping Web Filter VPN")
        vpnJob?.cancel()
        try {
            vpnInterface?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing VPN interface: ${e.message}")
        }
        vpnInterface = null

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        connectivityManager.unregisterNetworkCallback(networkCallback)
        stopVpn()
        super.onDestroy()
    }
}