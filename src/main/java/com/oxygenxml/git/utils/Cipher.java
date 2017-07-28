package com.oxygenxml.git.utils;

import org.apache.commons.codec.binary.Base64;

/**
 * TODO Use 
 * 
 * pluginWorkspaceAccess.getUtilAccess().encrypt(toEncrypt)
 */
public class Cipher {
	private static final String KEY = "some-secret-key-of-your-choice";

	public String encrypt(final String text) {
		return Base64.encodeBase64String(this.xor(text.getBytes()));
	}

	public String decrypt(final String hash) {
		try {
			return new String(this.xor(Base64.decodeBase64(hash.getBytes())), "UTF-8");
		} catch (java.io.UnsupportedEncodingException ex) {
			throw new IllegalStateException(ex);
		}
	}

	private byte[] xor(final byte[] input) {
		final byte[] output = new byte[input.length];
		final byte[] secret = this.KEY.getBytes();
		int spos = 0;
		for (int pos = 0; pos < input.length; ++pos) {
			output[pos] = (byte) (input[pos] ^ secret[spos]);
			spos += 1;
			if (spos >= secret.length) {
				spos = 0;
			}
		}
		return output;
	}
}