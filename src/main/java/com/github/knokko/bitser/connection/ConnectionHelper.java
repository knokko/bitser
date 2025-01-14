package com.github.knokko.bitser.connection;

import com.github.knokko.bitser.io.BitOutputStream;
import com.github.knokko.bitser.serialize.Bitser;
import com.github.knokko.bitser.serialize.IntegerBitser;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

class ConnectionHelper {

	static final byte[] STOP_SIGN = new byte[0];

	static byte[] encodePacket(Bitser bitser, Object packet) throws IOException {
		ByteArrayOutputStream packetBytes = new ByteArrayOutputStream();
		BitOutputStream packetBits = new BitOutputStream(packetBytes);
		bitser.serialize(packet, packetBits);
		packetBits.finish();
		return packetBytes.toByteArray();
	}

	static void sendEncodedPacket(byte[] packet, BitOutputStream output) throws IOException {
		IntegerBitser.encodeVariableInteger(packet.length, 0, Integer.MAX_VALUE, output);
		output.write(packet);
		output.flush();
	}
}
