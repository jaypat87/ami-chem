package com.google.common.collect;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

import com.google.common.base.Supplier;

public class LinkedHashBasedTable<R, C, V> extends StandardTable<R, C, V> {

	private static final long serialVersionUID = -494472060040021896L;

	private static class Factory<C, V> implements Supplier<Map<C, V>>, Serializable {

		private static final long serialVersionUID = 0;
		
		final int expectedSize;

		Factory(int expectedSize) {
			this.expectedSize = expectedSize;
		}


		public Map<C, V> get() {
			return new LinkedHashMap<C, V>(expectedSize);
		}
		
	}

	/**
	 * Creates an empty {@code LinkedHashBasedTable}.
	 */
	public static <R, C, V> LinkedHashBasedTable<R, C, V> create() {
		return new LinkedHashBasedTable<R, C, V>(new LinkedHashMap<R, Map<C, V>>(), new Factory<C, V>(0));
	}

	/**
	 * Creates an empty {@code LinkedHashBasedTable} with the specified map
	 * sizes.
	 * 
	 * @param expectedRows the expected number of distinct row keys
	 * @param expectedCellsPerRow the expected number of column key / value mappings in each row
	 * @throws IllegalArgumentException if {@code expectedRows} or {@code expectedCellsPerRow} is negative
	 */
	public static <R, C, V> LinkedHashBasedTable<R, C, V> create(int expectedRows, int expectedCellsPerRow) {
		Map<R, Map<C, V>> backingMap = new LinkedHashMap<R, Map<C, V>>(expectedRows);
		return new LinkedHashBasedTable<R, C, V>(backingMap, new Factory<C, V>(expectedCellsPerRow));
	}

	/**
	 * Creates a {@code LinkedHashBasedTable} with the same mappings as the
	 * specified table.
	 * 
	 * @param table the table to copy
	 * @throws NullPointerException if any of the row keys, column keys, or values in {@code table} are null.
	 */
	public static <R, C, V> LinkedHashBasedTable<R, C, V> create(Table<? extends R, ? extends C, ? extends V> table) {
		LinkedHashBasedTable<R, C, V> result = create();
		result.putAll(table);
		return result;
	}

	LinkedHashBasedTable(Map<R, Map<C, V>> backingMap, Factory<C, V> factory) {
		super(backingMap, factory);
	}

}