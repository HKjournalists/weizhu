package com.weizhu.common.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.escape.Escaper;
import com.google.common.escape.Escapers;
import com.google.protobuf.ByteString;

public class DBUtil {

	/**
	 * <pre>join ","</pre>
	 */
	public static final Joiner COMMA_JOINER = Joiner.on(",").skipNulls();
	
	/**
	 * <pre>split ","</pre>
	 */
	public static final Splitter COMMA_SPLITTER = Splitter.on(",").omitEmptyStrings();
	
	/**
	 * <pre>join "','"</pre>
	 */
	public static final Joiner QUOTE_COMMA_JOINER = Joiner.on("','").skipNulls();
	
	public static final Escaper SQL_STRING_ESCAPER = Escapers.builder()
			.addEscape('\0', "\\0")
			.addEscape('\'', "\\\'")
			.addEscape('\"', "\\\"")
			.addEscape('\b', "\\b")
			.addEscape('\n', "\\n")
			.addEscape('\r', "\\r")
			.addEscape('\t', "\\t")
			.addEscape('\u001A', "\\Z")
			.addEscape('\\', "\\\\")
			// .addEscape('%', "\\%")
			// .addEscape('_', "\\_")
			.build();

	// 用于like关键字后字符串的过滤
	public static final Escaper SQL_LIKE_STRING_ESCAPER = Escapers.builder()
			.addEscape('\0', "\\0")
			.addEscape('\'', "\\\'")
			.addEscape('\"', "\\\"")
			.addEscape('\b', "\\b")
			.addEscape('\n', "\\n")
			.addEscape('\r', "\\r")
			.addEscape('\t', "\\t")
			.addEscape('\u001A', "\\Z")
			.addEscape('\\', "\\\\")
			.addEscape('%', "\\%")
			// .addEscape('_', "\\_")
			.build();
	
	public static <K, V> void addMapLinkedList(Map<K, List<V>> map, K key, V value) {
		List<V> list = map.get(key);
		if (list == null) {
			list = new LinkedList<V>();
			map.put(key, list);
		}
		list.add(value);
	}
	
	public static <K, V> void addMapArrayList(Map<K, List<V>> map, K key, V value) {
		List<V> list = map.get(key);
		if (list == null) {
			list = new ArrayList<V>();
			map.put(key, list);
		}
		list.add(value);
	}
	
	public static void closeQuietly(ResultSet rs) {
		if (rs != null) {
			try {
				rs.close();
			} catch (SQLException e) {
				// TODO : print to log
				e.printStackTrace();
			}
		}
	}
	
	public static void closeQuietly(Statement stmt) {
		if (stmt != null) {
			try {
				stmt.close();
			} catch (SQLException e) {
				// TODO : print to log
				e.printStackTrace();
			}
		}
	}
	
	public static void closeQuietly(Connection conn) {
		if (conn != null) {
			try {
				conn.close();
			} catch (SQLException e) {
				// TODO : print to log
				e.printStackTrace();
			}
		}
	}
	
	public static void closeQuietly(Connection conn, Boolean autoCommit) {
		if (conn != null) {
			if (autoCommit != null) {
				try {
					conn.setAutoCommit(autoCommit);
				} catch (SQLException e) {
					// TODO : print to log
					e.printStackTrace();
				}
			}
			
			try {
				conn.close();
			} catch (SQLException e) {
				// TODO : print to log
				e.printStackTrace();
			}
		}
	}
	
	public static void set(PreparedStatement pstmt, int parameterIndex, boolean hasValue, int value) throws SQLException {
		if (hasValue) {
			pstmt.setInt(parameterIndex, value);
		} else {
			pstmt.setNull(parameterIndex, java.sql.Types.INTEGER);
		}
	}
	
	public static void set(PreparedStatement pstmt, int parameterIndex, @Nullable Integer value) throws SQLException {
		if (value != null) {
			pstmt.setInt(parameterIndex, value);
		} else {
			pstmt.setNull(parameterIndex, java.sql.Types.INTEGER);
		}
	}
	
	public static void set(PreparedStatement pstmt, int parameterIndex, boolean hasValue, long value) throws SQLException {
		if (hasValue) {
			pstmt.setLong(parameterIndex, value);
		} else {
			pstmt.setNull(parameterIndex, java.sql.Types.BIGINT);
		}
	}
	
	public static void set(PreparedStatement pstmt, int parameterIndex, @Nullable Long value) throws SQLException {
		if (value != null) {
			pstmt.setLong(parameterIndex, value);
		} else {
			pstmt.setNull(parameterIndex, java.sql.Types.BIGINT);
		}
	}
	
	public static void set(PreparedStatement pstmt, int parameterIndex, boolean hasValue, float value) throws SQLException {
		if (hasValue) {
			pstmt.setFloat(parameterIndex, value);
		} else {
			pstmt.setNull(parameterIndex, java.sql.Types.FLOAT);
		}
	}
	
	public static void set(PreparedStatement pstmt, int parameterIndex, @Nullable Float value) throws SQLException {
		if (value != null) {
			pstmt.setFloat(parameterIndex, value);
		} else {
			pstmt.setNull(parameterIndex, java.sql.Types.FLOAT);
		}
	}
	
	public static void set(PreparedStatement pstmt, int parameterIndex, boolean hasValue, double value) throws SQLException {
		if (hasValue) {
			pstmt.setDouble(parameterIndex, value);
		} else {
			pstmt.setNull(parameterIndex, java.sql.Types.DOUBLE);
		}
	}
	
	public static void set(PreparedStatement pstmt, int parameterIndex, @Nullable Double value) throws SQLException {
		if (value != null) {
			pstmt.setDouble(parameterIndex, value);
		} else {
			pstmt.setNull(parameterIndex, java.sql.Types.DOUBLE);
		}
	}
	
	public static void set(PreparedStatement pstmt, int parameterIndex, boolean hasValue, boolean value) throws SQLException {
		if (hasValue) {
			pstmt.setBoolean(parameterIndex, value);
		} else {
			pstmt.setNull(parameterIndex, java.sql.Types.BOOLEAN);
		}
	}
	
	public static void set(PreparedStatement pstmt, int parameterIndex, @Nullable Boolean value) throws SQLException {
		if (value != null) {
			pstmt.setBoolean(parameterIndex, value);
		} else {
			pstmt.setNull(parameterIndex, java.sql.Types.BOOLEAN);
		}
	}
	
	public static void set(PreparedStatement pstmt, int parameterIndex, boolean hasValue, String value) throws SQLException {
		if (hasValue && value != null) {
			pstmt.setString(parameterIndex, value);
		} else {
			pstmt.setNull(parameterIndex, java.sql.Types.VARCHAR);
		}
	}
	
	public static void set(PreparedStatement pstmt, int parameterIndex, @Nullable String value) throws SQLException {
		if (value != null) {
			pstmt.setString(parameterIndex, value);
		} else {
			pstmt.setNull(parameterIndex, java.sql.Types.VARCHAR);
		}
	}
	
	public static void set(PreparedStatement pstmt, int parameterIndex, boolean hasValue, ByteString value) throws SQLException {
		if (hasValue && value != null) {
			pstmt.setBinaryStream(parameterIndex, value.newInput());
		} else {
			pstmt.setNull(parameterIndex, java.sql.Types.VARBINARY);
		}
	}
	
	public static void set(PreparedStatement pstmt, int parameterIndex, @Nullable ByteString value) throws SQLException {
		if (value != null) {
			pstmt.setBinaryStream(parameterIndex, value.newInput());
		} else {
			pstmt.setNull(parameterIndex, java.sql.Types.VARBINARY);
		}
	}
	
	public static void set(PreparedStatement pstmt, int parameterIndex, boolean hasValue, Enum<?> value) throws SQLException {
		if (hasValue && value != null) {
			pstmt.setString(parameterIndex, value.name());
		} else {
			pstmt.setNull(parameterIndex, java.sql.Types.VARCHAR);
		}
	}
	
	public static void set(PreparedStatement pstmt, int parameterIndex, @Nullable Enum<?> value) throws SQLException {
		if (value != null) {
			pstmt.setString(parameterIndex, value.name());
		} else {
			pstmt.setNull(parameterIndex, java.sql.Types.VARCHAR);
		}
	}
}
