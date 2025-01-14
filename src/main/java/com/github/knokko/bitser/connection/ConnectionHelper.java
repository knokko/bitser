package com.github.knokko.bitser.connection;

import com.github.knokko.bitser.io.BitOutputStream;
import com.github.knokko.bitser.serialize.Bitser;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
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

	static void sendPacket(byte[] packet, DataOutputStream output) throws IOException {
		if (packet.length > 254) {
			output.write(255);
			output.writeInt(packet.length);
		} else output.write(packet.length);
		output.write(packet);
		output.flush();
	}

	static byte[] readPacket(DataInputStream input) throws IOException {
		int size = input.readUnsignedByte();
		if (size == 255) size = input.readInt();
		byte[] packet = new byte[size];
		input.readFully(packet);
		return packet;
	}
}
