package com.weizhu.service.external;

import javax.mail.Session;
import javax.mail.internet.InternetAddress;

public class EmailInfo {
	
	private final InternetAddress from;
	private final Session session;
	
	EmailInfo(InternetAddress from, Session session) {
		this.from = from;
		this.session = session;
	}

	public InternetAddress from() {
		return from;
	}

	public Session session() {
		return session;
	}
	
}
