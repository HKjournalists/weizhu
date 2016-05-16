package com.weizhu.service.admin.test;

import com.google.common.base.Charsets;
import com.google.common.hash.Hashing;

public class AdminTestUtil {
	
	public static void main(String[] args) {
//		printPasswordHash("123abcABC", "xxx");
		
		long companyId = 26;
		for (long adminId = 1001L; adminId <= 1008L; ++adminId) {
			
			for (int roleId = 1; roleId <= 12; ++roleId) {
				System.out.println("(" + adminId + ", " + companyId + ", " + roleId + "), ");
			}
		}
	}
	
	@SuppressWarnings("unused")
	private static void printPasswordHash(String password, String salt) {
		System.out.println(Hashing.sha1().hashString(password + salt + "admin@2016", Charsets.UTF_8).toString());
	}
	
}
