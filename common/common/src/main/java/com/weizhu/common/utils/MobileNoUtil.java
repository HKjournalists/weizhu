package com.weizhu.common.utils;

/**
 * 手机号码相关工具方法
 * @author lindongjlu
 *
 */
public class MobileNoUtil {

	/**
	 * 判断手机号是否正确，目前仅判断是否为11位数字
	 * @param mobileNo
	 * @return
	 */
	public static boolean isValid(String mobileNo) {
		if (mobileNo == null || mobileNo.trim().isEmpty()) {
			return false;
		}
		mobileNo = mobileNo.trim();
		if (mobileNo.length() != 11) {
			return false;
		}
		for (int i=0; i<11; ++i) {
			char c = mobileNo.charAt(i);
			if (c < '0' || c > '9') {
				return false;
			}
		}
		return true;
	}
	
	/**
	 * 调整手机号字符串，使其符合标准规范
	 * @param mobileNo
	 * @return
	 */
	public static String adjustMobileNo(String mobileNo) {
		if (!isValid(mobileNo)) {
			throw new IllegalArgumentException("invalid mobileNo");
		}
		return mobileNo.trim();
	}
	
	public static void main(String[] args) {
		System.out.println(MobileNoUtil.isValid("18612341234"));
	}
	
}
