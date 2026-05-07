package com.vahak.parentcontroll.core.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
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
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
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

        // Default safe MTU for MCI/Irancell
        private const val SAFE_MTU = 1300
    }

    private var vpnInterface: ParcelFileDescriptor? = null
    private val vpnScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var vpnJob: Job? = null

    private lateinit var connectivityManager: ConnectivityManager
    private var activeBlockedDomains: List<String> = emptyList()

    // 🚀 Store the Network object here so we can bind to it later
    data class PhysicalNetworkInfo(val isWifi: Boolean, val dns: InetAddress?, val network: Network)

    private val activeNetworksMap = ConcurrentHashMap<Network, PhysicalNetworkInfo>()

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {

        override fun onLinkPropertiesChanged(network: Network, lp: android.net.LinkProperties) {
            super.onLinkPropertiesChanged(network, lp)
            Log.d(TAG, "🔄 OS triggered onLinkPropertiesChanged for network: $network")

            val caps = connectivityManager.getNetworkCapabilities(network)
            if (caps == null) {
                Log.w(TAG, "⚠️ Could not fetch capabilities for network $network")
                return
            }

            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) {
                Log.d(TAG, "🛡️ Ignored VPN network loopback.")
                return
            }

            val isWifi = caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
            val isCell = caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)

            if (isWifi || isCell) {
                val dns = lp.dnsServers.firstOrNull {
                    it.hostAddress?.contains(".") == true && it.hostAddress != VPN_DNS
                }

                // Save the actual 'network' object into our map
                activeNetworksMap[network] = PhysicalNetworkInfo(isWifi, dns, network)

                val type = if (isWifi) "Wi-Fi" else "Cellular"
                Log.i(TAG, "🌐 $type mapped. DNS: ${dns?.hostAddress}. Total Active Networks: ${activeNetworksMap.size}")
            } else {
                Log.d(TAG, "❓ Ignored unknown network transport type.")
            }
        }

        override fun onLost(network: Network) {
            super.onLost(network)
            val removed = activeNetworksMap.remove(network)
            val type = if (removed?.isWifi == true) "Wi-Fi" else "Cellular"
            Log.i(TAG, "🔌 $type Network lost. Remaining active networks: ${activeNetworksMap.size}")
        }
    }

    // 🚀 RESTORED: Now returns the PhysicalNetworkInfo containing the Network object
    private fun getBestUnderlyingNetwork(): PhysicalNetworkInfo? {
        val wifiInfo = activeNetworksMap.values.firstOrNull { it.isWifi && it.dns != null }
        if (wifiInfo != null) {
            return wifiInfo
        }

        val cellInfo = activeNetworksMap.values.firstOrNull { !it.isWifi && it.dns != null }
        if (cellInfo != null) {
            return cellInfo
        }

        return null
    }

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "🟢 Service onCreate called")
        connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(request, networkCallback)
        Log.d(TAG, "📡 Network Callback Registered")

        vpnScope.launch {
            sessionManager.activeChildIdFlow.collectLatest { childId ->
                Log.d(TAG, "👦 Active Child ID changed: $childId")
                if (childId != null) {
                    webDao.observeBlockedDomains(childId).collectLatest { domains ->
                        activeBlockedDomains = domains.filter { it.isActive }.map { it.domain.lowercase() }
                        Log.i(TAG, "📋 Blocklist loaded into memory. Active rules: ${activeBlockedDomains.size}")
                    }
                } else {
                    activeBlockedDomains = emptyList()
                    Log.i(TAG, "📋 Blocklist cleared (No active child).")
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(TAG, "⚙️ onStartCommand received action: ${intent?.action}")
        when (intent?.action) {
            ACTION_START -> {
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

                startVpn()
            }
            ACTION_STOP -> stopVpn()
        }
        return START_STICKY
    }

    private fun startVpn() {
        if (vpnInterface != null) {
            Log.w(TAG, "⚠️ startVpn called but interface is already running.")
            return
        }

        try {
            Log.d(TAG, "🏗️ Building VPN Interface with MTU $SAFE_MTU...")
            val builder = Builder()
                .setSession("محافظت وب خانواده")
                .setMtu(SAFE_MTU) // 🚀 Hardcoded to 1300 for stability
                .addAddress(VPN_ADDRESS, 24)
                .addDnsServer(VPN_DNS)
                .addRoute(VPN_DNS, 32)

            vpnInterface = builder.establish()

            if (vpnInterface != null) {
                Log.i(TAG, "🚀 VPN Interface Established Successfully!")
                startPacketProcessing()
            } else {
                Log.e(TAG, "❌ Failed to establish VPN (builder returned null).")
                stopVpn()
            }

        } catch (e: Exception) {
            Log.e(TAG, "💥 Error configuring VPN: ${e.javaClass.simpleName} - ${e.message}", e)
            stopVpn()
        }
    }

    private fun startPacketProcessing() {
        vpnJob = vpnScope.launch {
            val fd = vpnInterface?.fileDescriptor ?: return@launch
            val inputStream = FileInputStream(fd)
            val outputStream = FileOutputStream(fd)
            val packet = ByteBuffer.allocate(1500)

            Log.i(TAG, "🎧 TUN Listener Thread Started. Waiting for traffic...")

            while (isActive) {
                try {
                    val length = inputStream.read(packet.array())
                    if (length > 0) {
                        val rawPacketBytes = packet.array()
                        val requestedDomain = DnsPacketHelper.extractDomainFromPacket(rawPacketBytes, length)

                        if (requestedDomain != null) {
                            // 🚀 Generate a unique ID for this specific packet flow
                            val packetId = UUID.randomUUID().toString().substring(0, 4).uppercase()

                            val isBlocked = activeBlockedDomains.any {
                                requestedDomain.lowercase().contains(it)
                            }

                            if (isBlocked) {
                                Log.w(TAG, "[$packetId] 🚨 BLOCKED: Dropping packet for -> $requestedDomain")
                            } else {
                                Log.v(TAG, "[$packetId] ✅ ALLOWED: Forwarding domain -> $requestedDomain")
                                // Pass the ID and domain down so we don't parse it twice
                                forwardDnsRequest(rawPacketBytes, length, outputStream, requestedDomain, packetId)
                            }
                        } else {
                            Log.v(TAG, "❓ Packet parsed, but no domain extracted.")
                        }
                    }
                } catch (e: Exception) {
                    if (isActive) {
                        Log.e(TAG, "⚠️ Packet loop exception: ${e.javaClass.simpleName} - ${e.message}")
                    }
                } finally {
                    packet.clear()
                }
            }

            Log.w(TAG, "🛑 Packet processing loop has officially terminated.")
        }
    }

    private suspend fun forwardDnsRequest(
        rawPacketBytes: ByteArray,
        length: Int,
        outputStream: FileOutputStream,
        requestedDomain: String,
        packetId: String
    ) {
        withContext(Dispatchers.IO) {
            var socket: DatagramSocket? = null
            try {
                val ihl = (rawPacketBytes[0].toInt() and 0x0F) * 4
                val dnsPayloadOffset = ihl + 8
                val dnsPayloadLength = length - dnsPayloadOffset

                if (dnsPayloadLength <= 0) {
                    Log.v(TAG, "[$packetId] ⚠️ DNS payload length <= 0, skipping.")
                    return@withContext
                }

                val dnsPayload = ByteArray(dnsPayloadLength)
                System.arraycopy(rawPacketBytes, dnsPayloadOffset, dnsPayload, 0, dnsPayloadLength)

                socket = DatagramSocket()

                if (!protect(socket)) {
                    Log.e(TAG, "[$packetId] ❌ Failed to protect the outbound UDP socket!")
                    return@withContext
                }

                // 🚀 BIND SOCKET TO PHYSICAL NETWORK
                val bestNetworkInfo = getBestUnderlyingNetwork()
                if (bestNetworkInfo != null) {
                    bestNetworkInfo.network.bindSocket(socket)
                    Log.v(TAG, "[$packetId] 🔗 Bound to ${if(bestNetworkInfo.isWifi) "Wi-Fi" else "Cellular"} antenna")
                }

                val systemDns = bestNetworkInfo?.dns ?: InetAddress.getByName("8.8.8.8")
                Log.v(TAG, "[$packetId] 📤 Sending UDP packet ($dnsPayloadLength bytes) to ${systemDns.hostAddress}:53")

                val outPacket = DatagramPacket(dnsPayload, dnsPayloadLength, systemDns, 53)
                socket.send(outPacket)

                socket.soTimeout = 3000
                val responseBuffer = ByteArray(1024)
                val inPacket = DatagramPacket(responseBuffer, responseBuffer.size)

                socket.receive(inPacket)
                Log.v(TAG, "[$packetId] 📥 Received UDP response (${inPacket.length} bytes) from ${inPacket.address.hostAddress}")

                val responseDnsPayload = inPacket.data.copyOfRange(0, inPacket.length)
                val finalIpPacket = forgeDnsResponsePacket(rawPacketBytes, responseDnsPayload)

                outputStream.write(finalIpPacket)
                Log.v(TAG, "[$packetId] ✍️ Forged IP packet written back to TUN.")

            } catch (e: Exception) {
                // 🚀 The log you requested: Shows the domain, the error, and the exact packet ID
                Log.e(TAG, "[$packetId] ⏳ Forwarding timeout/error for $requestedDomain: ${e.javaClass.simpleName} - ${e.message}")
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
        Log.i(TAG, "🛑 Triggering stopVpn(). Shutting down interfaces...")
        vpnJob?.cancel()
        try {
            vpnInterface?.close()
            Log.d(TAG, "🔒 VPN Interface closed successfully.")
        } catch (e: Exception) {
            Log.e(TAG, "💥 Error closing VPN interface: ${e.message}")
        }
        vpnInterface = null

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        Log.i(TAG, "💀 Service onDestroy called. Unregistering callbacks.")
        connectivityManager.unregisterNetworkCallback(networkCallback)
        stopVpn()
        super.onDestroy()
    }
}