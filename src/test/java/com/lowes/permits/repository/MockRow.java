package com.lowes.permits.repository;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.r2dbc.spi.ColumnMetadata;
import io.r2dbc.spi.Nullability;
import io.r2dbc.spi.Row;
import io.r2dbc.spi.RowMetadata;
import io.r2dbc.spi.Type;

/** Mock implementation of Row for testing purposes */
public class MockRow implements Row {
	private final Map<String, Object> data = new HashMap<>();

	public MockRow put(String column, Object value) {
		data.put(column, value);
		return this;
	}

	@Override
	public <T> T get(String name, Class<T> type) {
		Object value = data.get(name);
		if (value == null) {
			return null;
		}
		return type.cast(value);
	}

	@Override
	public <T> T get(int index, Class<T> type) {
		// For simplicity, we'll use the first value for any index
		if (data.isEmpty()) {
			return null;
		}
		Object value = data.values().iterator().next();
		return type.cast(value);
	}

	@Override
	public Object get(String name) {
		return data.get(name);
	}

	@Override
	public Object get(int index) {
		if (data.isEmpty()) {
			return null;
		}
		return data.values().iterator().next();
	}

	public int size() {
		return data.size();
	}

	public Object[] toArray() {
		return data.values().toArray();
	}

	@Override
	public RowMetadata getMetadata() {
		return new MockRowMetadata();
	}

	private static class MockRowMetadata implements RowMetadata {
		@Override
		public ColumnMetadata getColumnMetadata(int index) {
			return new MockColumnMetadata();
		}

		@Override
		public ColumnMetadata getColumnMetadata(String name) {
			return new MockColumnMetadata();
		}

		@Override
		public List<? extends ColumnMetadata> getColumnMetadatas() {
			return Collections.emptyList();
		}

		@Override
		public boolean contains(String columnName) {
			return false;
		}
	}

	private static class MockColumnMetadata implements ColumnMetadata {
		@Override
		public Class<?> getJavaType() {
			return Object.class;
		}

		@Override
		public String getName() {
			return "mock";
		}

		@Override
		public Object getNativeTypeMetadata() {
			return null;
		}

		@Override
		public Nullability getNullability() {
			return Nullability.UNKNOWN;
		}

		@Override
		public Integer getPrecision() {
			return null;
		}

		@Override
		public Integer getScale() {
			return null;
		}

		@Override
		public Type getType() {
			return null; // Return null since this is a mock
		}
	}
}
