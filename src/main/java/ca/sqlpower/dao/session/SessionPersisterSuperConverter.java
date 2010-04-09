/*
 * Copyright (c) 2009, SQL Power Group Inc.
 *
 * This file is part of SQL Power Library.
 *
 * SQL Power Library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * SQL Power Library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>. 
 */

package ca.sqlpower.dao.session;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.text.Format;

import ca.sqlpower.object.SPObject;
import ca.sqlpower.sql.DataSourceCollection;
import ca.sqlpower.sql.JDBCDataSource;
import ca.sqlpower.sql.SPDataSource;
import ca.sqlpower.sql.SpecificDataSourceCollection;

/**
 * Converts any known object into a simple type of object that can be
 * pushed through an HTTP request and persisted on the server. This also
 * contains a way to get the object back based on the simple type that can be
 * passed and stored.
 */
public class SessionPersisterSuperConverter {
	
	private final SPObjectConverter spObjectConverter;
	
	private final FormatConverter formatConverter = new FormatConverter();
	
	private final Point2DConverter point2DConverter = new Point2DConverter();
	
    private final RectangleConverter rectangleConverter = new RectangleConverter();

    private final DimensionConverter dimensionConverter = new DimensionConverter();
	
	private final ColorConverter colorConverter = new ColorConverter();
	
	private final StringArrayConverter stringArrayConverter = new StringArrayConverter();
	
	private final DataSourceCollection <JDBCDataSource> dsCollection;

	/**
	 * This converter will allow changes between any complex object in the
	 * session's workspace and a simple type that can be passed between
	 * persisters.
	 * 
	 * @param session
	 *            The session used to find necessary parts for converting
	 *            between simple and complex types. The session may be used to
	 *            look up connections, cubes, and {@link SPObject}s in the
	 *            workspace.
	 */
	public SessionPersisterSuperConverter(DataSourceCollection<? extends SPDataSource> dsCollection, 
			SPObject root) {
		spObjectConverter = new SPObjectConverter(root);
		this.dsCollection = new SpecificDataSourceCollection(dsCollection, JDBCDataSource.class);
	}

	/**
	 * Converts a complex object to a basic type or reference value that can
	 * then be passed on to other persisters. To reverse this process, see
	 * {@link #convertToComplexType}. If a basic object is given to this method
	 * it will be returned without modification.
	 * 
	 * @param convertFrom
	 *            The value to convert to a basic type
	 * @param fromType
	 *            The type that the basic type will be defined as
	 * @param additionalInfo
	 *            Any additional information that is required by the converters
	 *            for specific object types. The ONLY class that currently
	 *            requires an additional type is the cube converter. If we can
	 *            remove the need to pass the data source type with the cube
	 *            then this value can go away.
	 * @returns The basic type representation of the given object.
	 */
	public Object convertToBasicType(Object convertFrom, Object ... additionalInfo) {
		if (convertFrom == null) {
			return null;
		} else if (convertFrom instanceof SPObject) {
			SPObject spo = (SPObject) convertFrom;
			return spObjectConverter.convertToSimpleType(spo);
			
		} else if (convertFrom instanceof String) {
			return convertFrom;
			
		} else if (convertFrom instanceof Integer) {
			return convertFrom;
			
		} else if (convertFrom instanceof Double) {
			return convertFrom;
			
		} else if (convertFrom instanceof Boolean) {
			return convertFrom;
			
		} else if (convertFrom instanceof Long) {
			return convertFrom;
			
		} else if (convertFrom.getClass().isEnum()) {
			return new EnumConverter(convertFrom.getClass()).convertToSimpleType((Enum) convertFrom);
			
		} else if (convertFrom instanceof JDBCDataSource) {
			return ((JDBCDataSource) convertFrom).getName();
		
		} else if (convertFrom instanceof Format) {
			return formatConverter.convertToSimpleType((Format) convertFrom);
			
		} else if (convertFrom instanceof Point2D) {
		    Point2D p = (Point2D) convertFrom;
		    return point2DConverter.convertToSimpleType(p);

        } else if (convertFrom instanceof Rectangle) {
            Rectangle r = (Rectangle) convertFrom;
            return rectangleConverter.convertToSimpleType(r);

        } else if (convertFrom instanceof Dimension) {
            Dimension d = (Dimension) convertFrom;
            return dimensionConverter.convertToSimpleType(d);

		} else if (convertFrom instanceof Color) {
		    Color c = (Color) convertFrom;
		    return colorConverter.convertToSimpleType(c);
		} else if (convertFrom instanceof String[]) {
			String[] array = (String[]) convertFrom;
			return stringArrayConverter.convertToSimpleType(array);
		} else {
		    throw new IllegalArgumentException("Cannot convert " + convertFrom + " of type " + 
		            convertFrom.getClass());
		}
		
	}

	/**
	 * Converts a basic type to a complex type that can then be passed to other
	 * persisters. To reverse this process, see
	 * {@link #convertToBasicType(Object, Object...)}.
	 * 
	 * @param o
	 *            The value to convert to a complex type.
	 * @param type
	 *            The type that the complex type will be defined as
	 * @return The complex type representation of the given object.
	 */
	public Object convertToComplexType(Object o, Class<? extends Object> type) {
		if (o == null) {
			return null;
			
		} else if (SPObject.class.isAssignableFrom(type)) {
			return spObjectConverter.convertToComplexType((String) o);
			
		} else if (String.class.isAssignableFrom(type)) {
			return (String) o;
			
		} else if (Integer.class.isAssignableFrom(type)) {
			return (Integer) o;
			
		} else if (Double.class.isAssignableFrom(type)) {
			return (Double) o;
			
		} else if (Boolean.class.isAssignableFrom(type)) {
			return (Boolean) o;
		} else if (Long.class.isAssignableFrom(type)) {
			return (Long) o;
			
		} else if (Enum.class.isAssignableFrom(type)) {
			return new EnumConverter(type).convertToComplexType((String) o);
			
		} else if (JDBCDataSource.class.isAssignableFrom(type)) {
			if (((String) o).equals("PlayPen Database")) {
				return new JDBCDataSource(dsCollection);
			} else {
				return dsCollection.getDataSource((String) o, JDBCDataSource.class);
			}
		} else if (Format.class.isAssignableFrom(type)) {
			return formatConverter.convertToComplexType((String) o);
			
		} else if (Point.class.isAssignableFrom(type)) {
		    Point2D p2d = point2DConverter.convertToComplexType((String) o);
		    return new Point((int) p2d.getX(), (int) p2d.getY());

        } else if (Point2D.class.isAssignableFrom(type)) {
            return point2DConverter.convertToComplexType((String) o);

        } else if (Rectangle.class.isAssignableFrom(type)) {
            return rectangleConverter.convertToComplexType((String) o);

        } else if (Color.class.isAssignableFrom(type)) {
            return colorConverter.convertToComplexType((String) o);
            
        } else if (Dimension.class.isAssignableFrom(type)) {
            return dimensionConverter.convertToComplexType((String) o);
        } else if (String[].class.isAssignableFrom(type)) {
        	return stringArrayConverter.convertToComplexType((String) o);
		} else {
			throw new IllegalArgumentException("Cannot convert " + o + " of type " + 
					o.getClass() + " to the type " + type);
		}
	}

}