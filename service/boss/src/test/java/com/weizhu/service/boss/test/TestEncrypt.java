package com.weizhu.service.boss.test;

import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import com.google.common.base.Charsets;
import com.weizhu.common.utils.HexUtil;

public class TestEncrypt {

	public static void main(String[] args) throws Exception {
		
		SecureRandom r = new SecureRandom(); // should be the best PRNG
		byte[] iv = new byte[16];
		r.nextBytes(iv);
		
		System.out.println(Base64.getEncoder().encodeToString(iv));
		
		
		String content = "abc";
		String key = "01234567890123456789012345678901";
		String initVector = HexUtil.bin2Hex(iv);
		
		String encrpted = encrpt(key, initVector, content);
		
		System.out.println("encrpted=" + encrpted);
		
		String decrpted = decrpt(key, initVector, encrpted);
		
		System.out.println("decrpted=" + decrpted);
	}
	
	private static String encrpt(String key, String initVector, String content) throws Exception {
		IvParameterSpec iv = new IvParameterSpec(HexUtil.hex2bin(initVector));
		SecretKeySpec skeySpec = new SecretKeySpec(HexUtil.hex2bin(key), "AES");
		
		Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
        cipher.init(Cipher.ENCRYPT_MODE, skeySpec, iv);

        byte[] encrypted = cipher.doFinal(content.getBytes(Charsets.UTF_8));
        return Base64.getEncoder().encodeToString(encrypted);
	}
	
	private static String decrpt(String key, String initVector, String encrpted) throws Exception {
		IvParameterSpec iv = new IvParameterSpec(HexUtil.hex2bin(initVector));
		SecretKeySpec skeySpec = new SecretKeySpec(HexUtil.hex2bin(key), "AES");
		
		Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
        cipher.init(Cipher.DECRYPT_MODE, skeySpec, iv);

        byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(encrpted));
        return new String(decrypted, Charsets.UTF_8);
	}

}
