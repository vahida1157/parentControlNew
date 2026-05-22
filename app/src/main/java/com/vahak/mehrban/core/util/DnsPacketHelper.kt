package com.vahak.mehrban.core.util

import android.util.Log

/**
 * A utility to manually slice raw network packets and extract DNS (Domain Name System) requests.
 *
 * 📦 THE ANATOMY OF THE PACKET:
 * When a packet arrives from the VPN TUN interface, it looks like this:
 * [ IPv4 Header (20+ bytes) ] --> [ UDP Header (8 bytes) ] --> [ DNS Payload ]
 *
 * This object acts like a scanner. It carefully walks through the raw bytes,
 * verifying each header, until it finds the requested website (e.g., "www.google.com").
 */
object DnsPacketHelper {

    private const val TAG = "DnsPacketHelper"

    fun extractDomainFromPacket(packet: ByteArray, length: Int): String? {
        try {
            // The absolute minimum size for an IPv4 packet is 20 bytes (just the IP header).
            // If it is smaller, it's a corrupted fragment. Drop it.
            if (length < 20) return null

            // --- 1. IP (Internet Protocol) HEADER VALIDATION ---

            // The very first byte contains the IP Version in its top 4 bits.
            // Using 'shr 4' (shift right) pushes those bits down so we can read them.
            val version = (packet[0].toInt() shr 4) and 0x0F
            if (version != 4) return null // We only process IPv4. Ignore IPv6 for now.

            // The bottom 4 bits of the first byte tell us the "Internet Header Length" (IHL).
            // It represents the number of 32-bit (4-byte) rows. Usually 5 (5 * 4 = 20 bytes).
            val ihl = (packet[0].toInt() and 0x0F) * 4

            // Byte 9 of the IP header contains the Protocol ID.
            // Protocol 17 means UDP (User Datagram Protocol). DNS almost always uses UDP.
            val protocol = packet[9].toInt() and 0xFF
            if (protocol != 17) return null

            // --- 2. UDP HEADER VALIDATION ---

            val udpOffset = ihl // The UDP header starts exactly where the IP header ends.

            // The Destination Port is at bytes 2 and 3 of the UDP header.
            // We combine two 8-bit bytes into one 16-bit integer using bit shift (shl 8) and OR.
            val dstPort = ((packet[udpOffset + 2].toInt() and 0xFF) shl 8) or
                    (packet[udpOffset + 3].toInt() and 0xFF)

            // DNS traffic always goes to port 53. If it's not 53, it's not a DNS request.
            if (dstPort != 53) return null

            // --- 3. DNS HEADER VALIDATION ---

            val dnsOffset = udpOffset + 8 // The UDP header is always exactly 8 bytes long.
            if (length < dnsOffset + 12) return null // A standard DNS header is 12 bytes long.

            // Byte 2 of the DNS header contains the "Flags".
            // The highest bit (0x80) tells us if this is a Question (0) or an Answer (1).
            val flags = packet[dnsOffset + 2].toInt() and 0xFF
            val isResponse = (flags and 0x80) != 0
            if (isResponse) return null // We only block outgoing requests, ignore incoming answers.

            // Bytes 4 & 5 of the DNS header tell us how many questions are in this packet.
            val qdCount = ((packet[dnsOffset + 4].toInt() and 0xFF) shl 8) or
                    (packet[dnsOffset + 5].toInt() and 0xFF)
            if (qdCount == 0) return null // Ignore empty "keep-alive" network pings.

            // --- 4. DOMAIN NAME EXTRACTION (The Question Section) ---

            var currentOffset = dnsOffset + 12 // The domain name starts right after the 12-byte header.
            val domainBuilder = StringBuilder()

            // 🧠 HOW DNS DOMAINS ARE FORMATTED:
            // DNS does not send standard strings. It uses "Length-Prefixed Labels".
            // Instead of "www.google.com", it sends: [3]www [6]google [3]com [0]

            while (currentOffset < length) {
                // Read the length of the next word (label)
                val labelLength = packet[currentOffset].toInt() and 0xFF

                // A length of 0 means we've reached the end of the domain name.
                if (labelLength == 0) break

                // 🚀 SECURITY: Block Malformed Labels & Compression Pointers
                // Standard DNS labels cannot be longer than 63 characters.
                // If the top two bits are 11 (0xC0), it's a "Compression Pointer" used to save space.
                // Pointers shouldn't exist in a simple query question. If we see one, it might be
                // an attempt to bypass our filter, so we drop the packet.
                if (labelLength > 63 || (labelLength and 0xC0) == 0xC0) {
                    return null
                }

                currentOffset++ // Move past the length byte to the actual characters

                // Safety check: Ensure we don't try to read past the end of the packet (prevents crashes)
                if (currentOffset + labelLength > length) return null

                // 🚀 STABILITY: Convert the raw bytes into human-readable text
                // We strictly use US_ASCII because standard domain names do not use UTF-8 emojis/symbols.
                val label = String(packet, currentOffset, labelLength, Charsets.US_ASCII)
                domainBuilder.append(label).append(".")

                currentOffset += labelLength // Jump forward to the next label's length byte
            }

            val finalDomain = domainBuilder.toString()

            // Format the result: Remove the trailing dot (e.g., "google.com." -> "google.com")
            // and force lowercase so our blocklist database matching never fails due to capitalization.
            return if (finalDomain.isNotEmpty()) {
                finalDomain.dropLast(1).lowercase()
            } else null

        } catch (e: Exception) {
            // If the network sends us absolute garbage data, catch the math error so the VPN doesn't crash.
            Log.e(TAG, "Parsing error: ${e.message}")
            return null
        }
    }
}