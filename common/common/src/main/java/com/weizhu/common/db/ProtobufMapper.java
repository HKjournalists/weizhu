package com.weizhu.common.db;

import java.io.IOException;
import java.io.InputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.protobuf.ByteString;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Message;

public class ProtobufMapper<T extends Message> {

	private final T defaultInstance;
	private final FieldNode[] fieldNodes;
	
	private ProtobufMapper(T defaultInstance, String[] columns) {
		this.defaultInstance = defaultInstance;
		
		Arrays.sort(columns);
		this.fieldNodes = createMessageSubNode(defaultInstance, columns, 0, columns.length, 0);
	}
	
	public static <T extends Message> ProtobufMapper<T> createMapper(T defaultInstance, String... columns) {
		return new ProtobufMapper<T>(defaultInstance, columns);
	}
	
	@SuppressWarnings("unchecked")
	public List<T> mapToList(ResultSet rs) throws SQLException {
		Message.Builder builder = defaultInstance.newBuilderForType();
		
		List<Message> list = new ArrayList<Message>();
		while (rs.next()) {
			mapResultItem(rs, builder);
			list.add(builder.build());
			builder.clear();
		}
		return (List<T>) list;
	}
	
	public <B extends Message.Builder> B mapToItem(ResultSet rs, B builder) throws SQLException {
		mapResultItem(rs, builder);
		return builder;
	}
	
	private void mapResultItem(ResultSet rs, Message.Builder builder) throws SQLException {
		for (FieldNode fieldNode : fieldNodes) {
			Object value = fieldNode.mapResult(rs);
			if (value != null) {
				if (fieldNode.fieldDescriptor().isRepeated()) {
					builder.addRepeatedField(fieldNode.fieldDescriptor(), value);
				} else {
					builder.setField(fieldNode.fieldDescriptor(), value);
				}
			}
		}
	}
	
	private static FieldNode[] createMessageSubNode(final Message defaultInstance, final String[] sortedColumns, 
			int start, final int end, final int idx) {
		if (start >= end) {
			return new FieldNode[0];
		}
		
		List<FieldNode> nodeList = new ArrayList<FieldNode>();
		do {
			FieldNode subNode;
			String column = sortedColumns[start];
			int dotIdx = column.indexOf('.', idx);
			if (dotIdx < 0) {
				final String name = processName(column.substring(idx));
				final FieldDescriptor fieldDesc = defaultInstance.getDescriptorForType().findFieldByName(name);
				if (fieldDesc == null || fieldDesc.getJavaType() == FieldDescriptor.JavaType.MESSAGE) {
					throw new Error("wrong value type : " + column);
				}
				
				subNode = new ValueFieldNode(fieldDesc, column);
				start++;
			} else {
				final String name = processName(column.substring(idx, dotIdx));
				final FieldDescriptor fieldDesc = defaultInstance.getDescriptorForType().findFieldByName(name);
				if (fieldDesc == null || fieldDesc.getJavaType() != FieldDescriptor.JavaType.MESSAGE) {
					throw new Error("wrong message type : " + column);
				}
				
				String fullPath = column.substring(0, dotIdx + 1);
				int len = 1;
				while (start + len < end && sortedColumns[start + len].startsWith(fullPath)) {
					len++;
				}
				
				Message subDefaultInstance = defaultInstance.toBuilder().newBuilderForField(fieldDesc).getDefaultInstanceForType();
				subNode = new MessageFieldNode(fieldDesc, subDefaultInstance, createMessageSubNode(subDefaultInstance, sortedColumns, start, start + len, dotIdx + 1));
				
				start+=len;
			}
			nodeList.add(subNode);
		} while (start < end);
		return nodeList.toArray(new FieldNode[nodeList.size()]);
	}
	
	private static String processName(String field) {
		int l = field.indexOf('[');
		if (l < 0) {
			return field;
		}
		int r = field.indexOf(']', l + 1);
		if (r < 0) {
			return field;
		}
		return field.substring(0, l);
	}
	
	private static interface FieldNode {
		Descriptors.FieldDescriptor fieldDescriptor();
		Object mapResult(ResultSet rs) throws SQLException;
	}
	
	private static class MessageFieldNode implements FieldNode {
		
		private final Descriptors.FieldDescriptor fieldDescriptor;
		private final Message defaultInstance;
		private final FieldNode[] subFieldNodes;

		MessageFieldNode(FieldDescriptor fieldDescriptor, Message defaultInstance, FieldNode[] subFieldNodes) {
			this.fieldDescriptor = fieldDescriptor;
			this.defaultInstance = defaultInstance;
			this.subFieldNodes = subFieldNodes;
		}
		
		@Override
		public Descriptors.FieldDescriptor fieldDescriptor() {
			return fieldDescriptor;
		}

		@Override
		public Object mapResult(ResultSet rs) throws SQLException {
			
			Message.Builder builder = defaultInstance.newBuilderForType();
			
			for (FieldNode subNode : subFieldNodes) {
				Object value = subNode.mapResult(rs);
				if (value != null) {
					if (subNode.fieldDescriptor().isRepeated()) {
						builder.addRepeatedField(subNode.fieldDescriptor(), value);
					} else {
						builder.setField(subNode.fieldDescriptor(), value);
					}
				}
			}
			
			return builder.isInitialized() ? builder.build() : null;
		}
		
	}
	
	private static class ValueFieldNode implements FieldNode {
		
		private final Descriptors.FieldDescriptor fieldDescriptor;
		private final String columnName;

		ValueFieldNode(FieldDescriptor fieldDescriptor, String columnName) {
			this.fieldDescriptor = fieldDescriptor;
			this.columnName = columnName;
		}
		
		@Override
		public Descriptors.FieldDescriptor fieldDescriptor() {
			return fieldDescriptor;
		}

		@Override
		public Object mapResult(ResultSet rs) throws SQLException {
			switch(fieldDescriptor.getJavaType()) {
				case INT: {
					int value = rs.getInt(columnName);
					return rs.wasNull() ? null : value;
				}
				case LONG: {
					long value = rs.getLong(columnName);
					return rs.wasNull() ? null : value;
				}
				case FLOAT: {
					float value = rs.getFloat(columnName);
					return rs.wasNull() ? null : value;
				}
				case DOUBLE: {
					double value = rs.getDouble(columnName);
					return rs.wasNull() ? null : value;
				}
				case BOOLEAN: {
					boolean value = rs.getBoolean(columnName);
					return rs.wasNull() ? null : value;
				}
				case STRING: {
					String value = rs.getString(columnName);
					return rs.wasNull() ? null : value;
				}
				case BYTE_STRING: {
					InputStream value = rs.getBinaryStream(columnName);
					try {
						return value == null ? null : ByteString.readFrom(value);
					} catch (IOException e) {
						throw new SQLException("read binary column error", e);
					}
				}
				case ENUM: {
					String enumName = rs.getString(columnName);
					return enumName == null ? null : fieldDescriptor.getEnumType().findValueByName(enumName);
				}
				default: {
					throw new Error("wrong type");
				}
			}
		}
		
	}
	
}
