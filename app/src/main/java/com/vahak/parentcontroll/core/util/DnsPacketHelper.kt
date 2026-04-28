package com.vahak.parentcontroll.core.util

object DnsPacketHelper {

    /**
     * Slices a raw IPv4 packet to extract the requested DNS Domain Name.
     * Returns null if the packet is not a valid UDP Port 53 DNS query.
     */
    fun extractDomainFromPacket(packet: ByteArray, length: Int): String? {
        if (length < 20) return null // Too short to even be an IPv4 packet

        // 1. Check IP Version (First 4 bits of the first byte)
        val version = (packet[0].toInt() shr 4) and 0x0F
        if (version != 4) return null // We are only intercepting IPv4 for now

        // 2. Calculate Internet Header Length (IHL)
        // IHL is the bottom 4 bits of the first byte, multiplied by 4 (usually 20 bytes)
        val ihl = (packet[0].toInt() and 0x0F) * 4
        if (length < ihl + 8) return null

        // 3. Check Protocol (Byte 9 of IPv4 header)
        val protocol = packet[9].toInt() and 0xFF
        if (protocol != 17) return null // 17 is UDP. If it's TCP (6), ignore it.

        // 4. Check Destination Port (Bytes 2 and 3 of the UDP Header)
        val udpOffset = ihl
        val dstPort = ((packet[udpOffset + 2].toInt() and 0xFF) shl 8) or 
                      (packet[udpOffset + 3].toInt() and 0xFF)
        
        if (dstPort != 53) return null // Not a DNS request

        // 5. Parse the DNS Query Payload
        val dnsOffset = udpOffset + 8
        if (length < dnsOffset + 12) return null // Too short to contain a DNS header

        // The actual domain name starts exactly 12 bytes after the DNS header begins
        var currentOffset = dnsOffset + 12
        val domainBuilder = StringBuilder()

        // DNS names are formatted as length-prefixed labels.
        // E.g., www.google.com -> [3]www[6]google[3]com[0]
        try {
            while (currentOffset < length) {
                val labelLength = packet[currentOffset].toInt() and 0xFF
                if (labelLength == 0) break // A zero-length label marks the end of the domain

                // Ignore DNS compression pointers (0xC0) for simple query parsing
                if ((labelLength and 0xC0) == 0xC0) break 

                currentOffset++
                if (currentOffset + labelLength > length) break

                val label = String(packet, currentOffset, labelLength)
                domainBuilder.append(label).append(".")
                
                currentOffset += labelLength
            }
        } catch (e: Exception) {
            return null // Malformed packet
        }

        val finalDomain = domainBuilder.toString()
        return if (finalDomain.isNotEmpty()) finalDomain.dropLast(1) else null // Remove trailing dot
    }
}