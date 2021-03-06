/*
MIT License

Copyright (c) 2012-2017 PASTA Contributors (see CONTRIBUTORS.txt)

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/

package pasta.domain.template;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.OrderColumn;
import javax.persistence.Table;

import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;

import pasta.domain.BaseEntity;
import pasta.domain.VerboseName;

/**
 * Container class for the hand marking assessment module.
 * <p>
 * Contains all of the information required for the hand marking.
 * Very similar to a 2 dimensional array where some elements are
 * missing. Has a row and column header which are used as keys and
 * data for the element.
 * 
 * The column and row headers are pairings of a String name and a
 * double weight.
 * 
 * The weighting for hand marking is not relative. It should all add up
 * to 1 (100%), but it is possible to have a marking template that adds
 * up to 125%, giving students multiple avenues to getting full marks.
 * The final mark is capped at 100%.
 * 
 * <p>
 * File location on disk: $projectLocation$/template/handMarking/$handMarkingName$
 * 
 * @author Alex Radu
 * @version 2.0
 * @since 2012-11-13
 *
 */
@Entity
@Table (name = "hand_markings")
@VerboseName("hand marking module")
public class HandMarking extends BaseEntity implements Comparable<HandMarking> {

	private static final long serialVersionUID = 5276980986516750657L;
	
	private String name;
	
	@OneToMany(
			cascade = CascadeType.ALL, 
			orphanRemoval = true
	)
    @OrderBy("weight")
	@JoinTable(name="hand_marking_columns", 
		joinColumns=@JoinColumn(name="hand_marking_id"), 
		inverseJoinColumns=@JoinColumn(name="weighted_field_id")
	)
	@LazyCollection(LazyCollectionOption.FALSE)
	private List<WeightedField> columnHeader = new ArrayList<WeightedField>();
	
	@OneToMany(
			cascade = CascadeType.ALL, 
			orphanRemoval = true,
			mappedBy = "handMarking"
			)
	@LazyCollection(LazyCollectionOption.FALSE)
	private List<HandMarkData> data = new ArrayList<HandMarkData>();
	
	@OneToMany(
			cascade = CascadeType.ALL,
			orphanRemoval = true
	)
	@JoinTable(name="hand_marking_rows", 
		joinColumns=@JoinColumn(name="hand_marking_id"), 
		inverseJoinColumns=@JoinColumn(name="weighted_field_id")
	)
    @OrderColumn(name = "row_index")
	@LazyCollection(LazyCollectionOption.FALSE)
	private List<WeightedField> rowHeader = new ArrayList<WeightedField>();
	
	public String getName() {
		return name;
	}
	
	public String getFileAppropriateName() {
		return name.replaceAll("[^\\w]+", "");
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<WeightedField> getColumnHeader() {
		return columnHeader;
	}
	
	public void setColumnHeader(List<WeightedField> columnHeader) {
		removeAllColumns();
		addColumns(columnHeader);
	}

	public List<WeightedField> getRowHeader() {
		return rowHeader;
	}

	public void setRowHeader(List<WeightedField> rowHeader) {
		removeAllRows();
		addRows(rowHeader);
	}

	public List<HandMarkData> getData() {
		return data;
	}

	public void setData(List<HandMarkData> data) {
		removeAllData();
		for(HandMarkData datum : data) {
			addData(datum);
		}
	}

	private WeightedField getRow(long id) {
		for(WeightedField row : rowHeader) {
			if(row.getId() == id) {
				return row;
			}
		}
		return null;
	}
	
	private WeightedField getColumn(long id) {
		for(WeightedField col : columnHeader) {
			if(col.getId() == id) {
				return col;
			}
		}
		return null;
	}
	
	public boolean hasRow(long id) {
		return getRow(id) != null;
	}
	
	public double getRowWeight(long id) {
		WeightedField row = getRow(id);
		return row == null ? 0 : row.getWeight();
	}
	
	public boolean hasColumn(long id) {
		return getColumn(id) != null;
	}
	
	public double getColumnWeight(long id) {
		WeightedField col = getColumn(id);
		return col == null ? 0 : col.getWeight();
	}

	public void addData(HandMarkData handMarkData) {
		getData().add(handMarkData);
		handMarkData.setHandMarking(this);
	}
	
	public void addData(Collection<HandMarkData> handMarkData) {
		for(HandMarkData datum : handMarkData) {
			addData(datum);
		}
	}
	
	public boolean removeData(HandMarkData handMarkData) {
		boolean removed = getData().remove(handMarkData);
		if(removed) {
			handMarkData.setHandMarking(null);
		}
		return removed;
	}
	
	public boolean removeData(Collection<HandMarkData> handMarkData) {
		boolean success = true;
		for(HandMarkData data : handMarkData) {
			success &= removeData(data);
		}
		return success;
	}
	
	public void removeAllData() {
		for(HandMarkData datum : getData()) {
			datum.setHandMarking(null);
		}
		getData().clear();
	}
	
	public void removeAllRows() {
		getRowHeader().clear();
	}
	
	public void removeAllColumns() {
		getColumnHeader().clear();
	}

	public void addColumn(WeightedField column) {
		getColumnHeader().add(column);
	}
	
	public void addColumns(Collection<WeightedField> columns) {
		for(WeightedField column : columns) {
			addColumn(column);
		}
	}
	
	public boolean removeColumn(WeightedField column) {
		List<HandMarkData> toRemove = new LinkedList<>();
		for(HandMarkData data : getData()) {
			if(data.getColumn().equals(column)) {
				toRemove.add(data);
			}
		}
		boolean success = true;
		for(HandMarkData data : toRemove) {
			success &= removeData(data);
		}
		return success && getColumnHeader().remove(column);
	}
	
	public boolean removeColumns(Collection<WeightedField> columns) {
		boolean success = true;
		for(WeightedField column : columns) {
			success &= removeColumn(column);
		}
		return success;
	}
	
	public void addRow(WeightedField row) {
		getRowHeader().add(row);
	}
	
	public void addRows(Collection<WeightedField> rows) {
		for(WeightedField row : rows) {
			addRow(row);
		}
	}
	
	public boolean removeRow(WeightedField row) {
		List<HandMarkData> toRemove = new LinkedList<>();
		for(HandMarkData data : getData()) {
			if(data.getRow().equals(row)) {
				toRemove.add(data);
			}
		}
		boolean success = true;
		for(HandMarkData data : toRemove) {
			success &= removeData(data);
		}
		return success && getRowHeader().remove(row);
	}
	
	public boolean removeRows(Collection<WeightedField> rows) {
		boolean success = true;
		for(WeightedField row : rows) {
			success &= removeRow(row);
		}
		return success;
	}

	@Override
	public int compareTo(HandMarking other) {
		return this.name.compareTo(other.name);
	}
	
	/*===========================
	 * CONVENIENCE RELATIONSHIPS
	 * 
	 * Making unidirectional many-to-one relationships into bidirectional 
	 * one-to-many relationships for ease of deletion by Hibernate
	 *===========================
	 */
	@OneToMany(mappedBy = "handMarking", fetch = FetchType.LAZY, cascade = CascadeType.REMOVE, orphanRemoval = true)
	private List<WeightedHandMarking> weightedHandMarkings;
}
