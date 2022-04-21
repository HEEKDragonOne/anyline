/*  
 * Copyright 2006-2022 www.anyline.org
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at 
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0 
 * 
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License. 
 * 
 *           
 */ 
 
 
package org.anyline.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.anyline.entity.DataRow;
import org.anyline.entity.DataSet;
import org.anyline.util.regular.Regular;
import org.anyline.util.regular.RegularUtil;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class BeanUtil { 
	public static ObjectMapper JSON_MAPPER = new ObjectMapper();

	private static final Logger log = LoggerFactory.getLogger(BeanUtil.class);
	static{
		JSON_MAPPER.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
		JSON_MAPPER.setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
		JSON_MAPPER.setSerializationInclusion(JsonInclude.Include.ALWAYS);
		//Include.Include.ALWAYS 默认
		//Include.NON_DEFAULT 属性为默认值不序列化
		//Include.NON_EMPTY 属性为 空（“”） 或者为 NULL 都不序列化
		//Include.NON_NULL 属性为NULL 不序列化
	}


	private static ObjectMapper newObjectMapper(JsonInclude.Include include){
		ObjectMapper result = new ObjectMapper();
		result.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
		result.setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
		result.setSerializationInclusion(include);
		return result;
	}
	public static boolean setFieldValue(Object obj, Field field, Object value){ 
		if(null == obj || null == field){ 
			return false; 
		} 
		if(Modifier.isStatic(field.getModifiers())){ 
			return false; 
		}
		try{
			Object v = value;
			boolean compatible = true;//是否兼容 int long等不能设置null值
			String type = field.getType().getSimpleName().toLowerCase();
			if(null != v){
				if(type.equals("int") || type.equals("integer")){
					v = Integer.parseInt(value.toString());
				}else if(type.equals("double")){
					v = Double.parseDouble(value.toString());
				}else if(type.equals("long")){
					v = Long.parseLong(value.toString());
				}else if(type.equals("float")){
					v = Float.parseFloat(value.toString());
				}else if(type.equals("boolean")){
					v = Boolean.parseBoolean(value.toString());
				}else if(type.equals("short")){
					v = Short.parseShort(value.toString());
				}else if(type.equals("bigdecimal")){
					v = new BigDecimal(value.toString());
				}else if(type.equals("byte")){
					v = Byte.parseByte(value.toString());
				}
			}else{
				if(type.equals("int")
						|| type.equals("double")
						|| type.equals("long")
						|| type.equals("float")
						|| type.equals("boolean")
						|| type.equals("short")
						|| type.equals("byte")
				){
					compatible = false;
				}
			}
			if(compatible) {
				if (field.isAccessible()) {
					//可访问属性
					field.set(obj, v);
				} else {
					//不可访问属性
					field.setAccessible(true);
					field.set(obj, v);
					field.setAccessible(false);
				}
			}
		}catch(Exception e){
			e.printStackTrace();
			return false;
		}
		return true; 
	} 
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static boolean setFieldValue(Object obj, String field, Object value, boolean recursion){ 
		if(null == obj || null == field){ 
			return false; 
		} 
		if(obj instanceof Map){ 
			Map tmp = (Map)obj; 
			tmp.put(field, value); 
		}else{ 
			Field f = getField(obj.getClass(), field, recursion); 
			setFieldValue(obj, f, value); 
		} 
		return true; 
	} 
	public static boolean setFieldValue(Object obj, String field, Object value){ 
		return setFieldValue(obj, field, value, true); 
	}

	public static Method getMethod(Class<?> clazz, String name, boolean recursion, Class<?>... parameterTypes){ 
		Method method = null; 
		try{ 
			method = clazz.getMethod(name, parameterTypes); 
		}catch(Exception e){} 
		if(null == method){ 
			try{ 
				method = clazz.getDeclaredMethod(name, parameterTypes); 
			}catch(Exception e){ 
				 
			} 
		} 
		//递归父类 
		if(null == method && recursion){ 
			clazz = clazz.getSuperclass(); 
			if(null != clazz){ 
				method = getMethod(clazz, name, recursion, parameterTypes); 
			} 
		} 
		return method; 
	} 
 
	public static Method getMethod(Class<?> clazz, String name, Class<?>... parameterTypes){ 
		return getMethod(clazz, name, false, parameterTypes); 
	} 
	public static Field getField(Class<?> clazz, String name, boolean recursion){ 
		Field field = null; 
		try{ 
			field = clazz.getField(name); 
		}catch(Exception e){} 
		if(null == field){ 
			try{ 
				field = clazz.getDeclaredField(name); 
			}catch(Exception e){ 
				 
			} 
		} 
		//递归父类 
		if(null == field && recursion){ 
			clazz = clazz.getSuperclass(); 
			if(null != clazz){ 
				field = getField(clazz, name); 
			} 
		} 
		return field; 
	} 
 
	public static Field getField(Class<?> clazz, String name){ 
		return getField(clazz, name, true); 
	} 
	public static Object getFieldValue(Object obj, Field field){ 
		Object value = null; 
		if(null == obj || null == field){ 
			return null; 
		} 
		try{ 
			if(field.isAccessible()){ 
				//可访问属性 
				value = field.get(obj); 
			}else{ 
				//不可访问属性 
				field.setAccessible(true); 
				value = field.get(obj); 
				field.setAccessible(false); 
			} 
		}catch(Exception e){ 
			return null; 
		} 
		return value; 
	} 
	@SuppressWarnings("rawtypes")
	public static Object getFieldValue(Object obj, String field, boolean recursion){ 
		if(null == obj){ 
			return null; 
		} 
		Object value = null;
		if(obj instanceof DataRow){
			DataRow row = (DataRow)obj;
			value = row.get(field);
		}else if(obj instanceof Map){
			Map map = (Map)obj; 
			value = map.get(field); 
		}else if(obj instanceof Class){
			Field f = getField((Class)obj, field, recursion); 
			value = getFieldValue(obj, f); 
		}else{ 
			Field f = getField(obj.getClass(), field, recursion); 
			value = getFieldValue(obj, f); 
		} 
		return value; 
		 
	}

	public static Object getFieldValue(Object obj, String field){ 
		return getFieldValue(obj, field, false); 
	} 
	@SuppressWarnings("rawtypes")
	public static List<String> getMapKeys(Map map){ 
		List<String> list = new ArrayList<>();
		for(Object key:map.keySet()){ 
			list.add(key.toString()); 
		} 
		return list; 
	} 
	/**
	 * 属性对应的列 
	 * @param field field
	 * @param checkInsert checkInsert
	 * @param checkUpdate checkUpdate
	 * @return return
	 */
	public static String getColumn(Field field, boolean checkInsert, boolean checkUpdate){ 
 /*
		try{ 
			Annotation annotation = field.getAnnotation(Column.class); 
			if(null == annotation){ 
				//没有Column注解 
				return field.getName(); 
			} 
			String column = (String)getAnnotationValue(field, Column.class, "name"); 
			if(checkInsert){ 
				//检查是否可插入 
				Object insertAble = getAnnotationValue(field, Column.class, "insertable"); 
				if(!BasicUtil.parseBoolean(insertAble, true)){ 
					return null; 
				} 
			} 
			if(checkUpdate){ 
				//检查是否可更新 
				Object updateAble = getAnnotationValue(field, Column.class, "updatable"); 
				if(!BasicUtil.parseBoolean(updateAble, true)){ 
					return null; 
				} 
			} 
			return column; 
		}catch(Exception e){
			e.printStackTrace(); 
			return null; 
		} */
		return null;
	} 
	public static String getColumn(Class<?> clazz,String field, boolean checkInsert, boolean checkUpdate){ 
		try { 
			Field _field = clazz.getDeclaredField(field); 
			return getColumn(_field, checkInsert, checkUpdate); 
		} catch (SecurityException e) { 
			e.printStackTrace(); 
		} catch (NoSuchFieldException e) { 
			e.printStackTrace(); 
		} 
		return null; 
	} 
	/** 
	 * 属性注解值 
	 * @param field  field
	 * @param clazz  clazz
	 * @param property  property
	 * @return return
	 */ 
	@SuppressWarnings({ "unchecked", "rawtypes" }) 
	public static Object getAnnotationValue(Field field, Class clazz, String property){ 
		try{ 
			Annotation annotation = field.getAnnotation(clazz); 
			if(null == annotation){ 
				return null; 
			} 
			Method method = annotation.annotationType().getMethod(property); 
			if(null == method){ 
				return null; 
			} 
			Object value = method.invoke(annotation); 
			return value; 
		}catch(Exception e){ 
			e.printStackTrace(); 
			return null; 
		} 
	} 
	/** 
	 * 根据列名读取属性值 
	 * @param obj obj
	 * @param column  column
	 * @return return
	 */ 
	@SuppressWarnings("rawtypes")
	public static Object getValueByColumn(Object obj, String column){ 
		/*读取类属性*/ 
		if(null == obj || null == column){ 
			return null; 
		} 
		if(obj instanceof Map){ 
			return ((Map)obj).get(column); 
		}else{ 
			List<Field> fields = getFields(obj.getClass());					 
			for(Field field:fields){ 
				String col = getColumn(field, false, false); 
				if(null != col && col.equals(column)){ 
					try{ 
						return getFieldValue(obj, field); 
					}catch(Exception e){ 
						e.printStackTrace(); 
					} 
				} 
			} 
		} 
		return null; 
	} 
	 
	 
	/** 
	 * 提取类及父类的所有属性 
	 * @param clazz  clazz
	 * @return return
	 */ 
	public static List<Field> getFields(Class<?> clazz){ 
		List<Field> fields = new ArrayList<Field>(); 
		while(null != clazz){ 
			Field[] tmp = clazz.getDeclaredFields(); 
			for(Field field:tmp){ 
				fields.add(field); 
			} 
			clazz = clazz.getSuperclass(); 
		} 
		return fields; 
	} 
	public static List<String> getFieldsName(Class<?> clazz){ 
		List<Field> fields = getFields(clazz); 
		List<String> keys = new ArrayList<>();
		for(Field field:fields){
			keys.add(field.getName()); 
		} 
		return keys; 
	} 
	public static String checkTable(Class<?> clazz){ 
		String result = null; 
/*		try{
			Annotation annotation = clazz.getAnnotation(Table.class);					//提取Table注解 
			Method method = annotation.annotationType().getMethod("name");				//引用name方法 
			result = (String)method.invoke(annotation);									//执行name方法返回结果 
		}catch(Exception e){ 
			e.printStackTrace(); 
		} */
		return result; 
	} 
	/** 
	 * 查询指定类的有annotation注解的属性 
	 * @param clazz  clazz
	 * @param annotation  annotation
	 * @return return
	 */ 
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static List<Field> searchFieldsByAnnotation(Class clazz, Class annotation){ 
		List<Field> list = new ArrayList<Field>(); 
		try{ 
			List<Field> fields = getFields(clazz); 
			for(Field field:fields){ 
				Annotation at = field.getAnnotation(annotation); 
				if(null != at){ 
					list.add(field); 
				} 
			} 
		}catch(Exception e){ 
			e.printStackTrace(); 
		} 
		return list; 
	} 
	/** 
	 * 主键列名 
	 * @param clazz  clazz
	 * @return return
	 */ 
	public static String getPrimaryKey(Class<?> clazz){ 
		/*List<Field> fields = searchFieldsByAnnotation(clazz, Id.class);
		if(fields.size()>0){ 
			Field field = fields.get(0); 
			return getColumn(field, false, false); 
		} */
		return null; 
	} 
	public static Object getPrimaryValue(Object obj){ 
		if(null == obj){ 
			return null; 
		} 
		String key = getPrimaryKey(obj.getClass()); 
		return getFieldValue(obj, key); 
	} 
	/** 
	 * 对象转换成Map 
	 * @param obj  obj
	 * @param keys keys
	 * @return return
	 */ 
	@SuppressWarnings("unchecked")
	public static Map<String,Object> toMap(Object obj, String ... keys){ 
		if(null == obj){ 
			return null; 
		} 
		Map<String,Object> map = new HashMap<String,Object>(); 
		if(null == keys || keys.length ==0){ 
			if(obj instanceof Map){ 
				// map to map 
				Map<String,Object> tmp = (Map<String,Object>)obj; 
				for(String key:tmp.keySet()){ 
					map.put(key, tmp.get(key)); 
				} 
			}else{ 
				// object to map 
				List<Field> fields = getFields(obj.getClass()); 
				for(Field field:fields){ 
					String key = field.getName(); 
					Object value = getFieldValue(obj, field); 
					if(null == value){ 
						value = ""; 
					} 
					map.put(key, value); 
				} 
			} 
		}else{ 
			for(String key:keys){ 
				Object value = null; 
				if(obj instanceof Map){ 
					value = ((Map<String,Object>)obj).get(key); 
				}else{ 
					value = getFieldValue(obj, key); 
					if(null == value){ 
						value = ""; 
					} 
				} 
				map.put(key, value); 
			} 
		} 
		return map; 
	} 
	public static List<Map<String,Object>> toMaps(Collection<?> objs, String ... keys){ 
		if(null == objs){ 
			return null; 
		} 
		List<Map<String,Object>> list = new ArrayList<Map<String,Object>>(); 
		for(Object obj:objs){ 
			list.add(toMap(obj,keys)); 
		} 
		return list; 
	} 
	/** 
	 * 过虑指定属性 
	 * @param objs  objs
	 * @param keys  keys
	 */ 
	public static void removeProperty(Collection<Object> objs, String ... keys){ 
		if(null == keys || null == objs){ 
			return; 
		} 
		for(String key:keys){ 
			removeProperty(objs, key); 
		} 
	} 
	@SuppressWarnings("rawtypes")
	public static void removeProperty(Object obj, String key){ 
		if(null == obj || null == key){ 
			return; 
		} 
		if(obj instanceof Map){ 
			((Map) obj).remove(key); 
		}else{ 
			setFieldValue(obj, key, null); 
		} 
	} 
 
	/** 
	 * 提取指定属性值 
	 * @param objs  objs
	 * @param keys  keys
	 * @return return 
	 */ 
	public static Collection<Object> fetch(Collection<Object> objs, String ... keys){ 
		if(null == objs){ 
			return null; 
		} 
		Collection<Object> list = new ArrayList<Object>(); 
		for(Object obj: objs){ 
			list.add(fetch(obj, keys)); 
		} 
		return list; 
	} 
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static Object fetch(Object obj, String ... keys){ 
		if(null == obj){ 
			return null; 
		} 
		Object result = null; 
		try{ 
			result = obj.getClass().newInstance(); 
			if(null != keys){ 
				for(String key:keys){ 
					if(obj instanceof Map){ 
						Object value = ((Map)obj).get(key); 
						((Map)obj).put(key, value); 
					}else{ 
						Object value = BeanUtil.getFieldValue(obj, key); 
						BeanUtil.setFieldValue(obj, key, value); 
					} 
				} 
			} 
		}catch(Exception e){ 
			 
		} 
		return result; 
	} 
	/** 
	 * 参考 DataSet.getRows
	 * @param list  list
	 * @param params  params
	 * @return return
	 */ 
	public static Collection<?> select(Collection<?> list, String ... params){
		if(null == list || null == params || params.length==0){ 
			return list; 
		} 
		if(list instanceof DataSet){ 
			return ((DataSet)list).getRows(params); 
		} 
		Map<String,String> kvs = new HashMap<String,String>(); 
		int len = params.length; 
		int i = 0; 
		while(i<len){ 
			String p1 = params[i]; 
			if(BasicUtil.isEmpty(p1)){ 
				i++; 
				continue; 
			}else if(p1.contains(":")){ 
				String ks[] = BeanUtil.parseKeyValue(p1); 
				kvs.put(ks[0], ks[1]); 
				i++; 
				continue; 
			}else{ 
				if(i+1<len){ 
					String p2 = params[i+1]; 
					if(BasicUtil.isEmpty(p2) || !p2.contains(":")){ 
						kvs.put(p1, p2);  
						i+=2; 
						continue; 
					}else{ 
						String ks[] = BeanUtil.parseKeyValue(p2); 
						kvs.put(ks[0], ks[1]); 
						i+=2; 
						continue; 
					} 
				} 
 
			} 
			i++; 
		} 
		 
		Object[] items = list.toArray(); 
		int size = list.size(); 
		for(i=size-1; i>=0; i--){ 
			Object obj = items[i]; 
			boolean chk = true;//对比结果 
			for(String k : kvs.keySet()){ 
				String v = kvs.get(k); 
				Object value = BeanUtil.getValueByColumn(obj, k); 
				 
				if(null == v){ 
					if(null != value){ 
						chk = false; 
						break; 
					} 
				}else{ 
					if(!v.equals(value+"")){ 
						chk = false; 
						break; 
					} 
				} 
			} 
			if(!chk){ 
				list.remove(obj); 
			} 
		} 
		return list; 
	} 
	/** 
	 * pack包下的所有类 不包括jar包中定义类 
	 * @param pack  pack
	 * @param bases bases
	 * @return return
	 */ 
	 
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static List<Class> getClasses(String pack, Class ... bases){ 
		List<Class> list = new ArrayList<Class>(); 
		File dir = new File(BeanUtil.class.getResource("/").getFile(),pack.replace(".", File.separator)); 
		List<File> files = FileUtil.getAllChildrenFile(dir,".class"); 
		for(File file:files){ 
			try{ 
				String path = file.getAbsolutePath(); 
				if(ConfigTable.isDebug() && log.isWarnEnabled()){ 
					log.warn("[检索类][file:{}]",path); 
				} 
				if(path.contains(File.separator+"classes"+File.separator)){ 
					path = path.substring(path.indexOf(File.separator+"classes"+File.separator)); 
				} 
				path = path.replace(File.separator, "."); 
				path = path.replace(".classes.", "").replace(".class", ""); 
				if(ConfigTable.isDebug() && log.isWarnEnabled()){ 
					log.warn("[检索类][class:{}]",path); 
				} 
				Class clazz = Class.forName(path); 
				if(clazz.getName().contains("$")){ 
					continue; 
				} 
				if(null != bases && bases.length>0){ 
					for(Class base:bases){ 
						if(clazz.equals(base)){ 
							continue; 
						} 
						if(base.isAssignableFrom(clazz)){ 
							list.add(clazz); 
							continue; 
						} 
					} 
				}else{ 
					list.add(clazz); 
				} 
			}catch(Exception e){ 
				e.printStackTrace(); 
			} 
		} 
		return list; 
	}
	/**
	 * @param params key1,value1,key2:value2,key3,value3
	 *               "NM:zh%","AGE:&gt;20","NM","%zh%"
	 * @return Map
	 */
	public static Map<String,String> array2map(String ... params){
		Map<String, String> map = new HashMap<String, String>();
		int len = params.length;
		int i = 0;
		while (i < len) {
			String p1 = params[i];
			if (BasicUtil.isEmpty(p1)) {
				i++;
				continue;
			} else if (p1.contains(":")) {
				String ks[] = BeanUtil.parseKeyValue(p1);
				map.put(ks[0], ks[1]);
				i++;
				continue;
			} else {
				if (i + 1 < len) {
					String p2 = params[i + 1];
					if (BasicUtil.isEmpty(p2) || !p2.contains(":")) {
						map.put(p1, p2);
						i += 2;
						continue;
					} else {
						String ks[] = BeanUtil.parseKeyValue(p2);
						map.put(ks[0], ks[1]);
						i += 2;
						continue;
					}
				}

			}
			i++;
		}
		return  map;
	}
	@SuppressWarnings("rawtypes")
	public static <T> T map2object(Map<String,?> map, Class<T> clazz, boolean recursion){ 
		T obj = null; 
		try { 
			obj = (T)clazz.newInstance(); 
			Set es = map.entrySet(); 
			Iterator it = es.iterator(); 
			while (it.hasNext()) { 
				Map.Entry entry = (Map.Entry) it.next(); 
				String k = (String) entry.getKey(); 
				Object v = entry.getValue(); 
				BeanUtil.setFieldValue(obj, k, v, recursion); 
			} 
		}catch(Exception e){ 
			e.printStackTrace(); 
		} 
		return obj; 
	} 
	public static <T> T map2object(Map<String,?> map, Class<T> clazz){ 
		return map2object(map, clazz, false); 
	}
	public static <T> T json2oject(String json, Class<T> clazz, JsonInclude.Include include){
		try {
			if(null != include){
				return newObjectMapper(include).readValue(json, clazz);
			}
			return  JSON_MAPPER.readValue(json, clazz);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	public static <T> T json2oject(String json, Class<T> clazz){
		return json2oject(json,clazz, null);
	}
	@SuppressWarnings("rawtypes")
	public static String map2xml(Map<String,?> map, boolean border, boolean order){ 
		StringBuffer builder = new StringBuffer(); 
		if(border){ 
			builder.append("<xml>"); 
		} 
		if(order){ 
			SortedMap<String, Object> sort = new TreeMap<String, Object>(map); 
			Set es = sort.entrySet(); 
			Iterator it = es.iterator(); 
			while (it.hasNext()) { 
				Map.Entry entry = (Map.Entry) it.next(); 
				String k = (String) entry.getKey(); 
				String v = entry.getValue()+""; 
				if("null".equals(v)){ 
					v = ""; 
				} 
				builder.append("<" + k + ">" + v + "</" + k + ">"); 
			} 
		}else{ 
			Set es = map.entrySet(); 
			Iterator it = es.iterator(); 
			while (it.hasNext()) { 
				Map.Entry entry = (Map.Entry) it.next(); 
				String key = (String) entry.getKey(); 
				String value = entry.getValue()+""; 
				if("null".equals(value)){ 
					value = ""; 
				} 
				builder.append("<").append(key).append(">").append(value).append("</").append(key).append(">"); 
			} 
		} 
		if(border){ 
			builder.append("</xml>"); 
		} 
		return builder.toString(); 
	} 
	public static String map2xml(Map<String,?> map){ 
		return map2xml(map, true, false); 
	} 
	public static String map2json(Map<String,?> map){ 
		return object2json(map);
	} 
	public static Map<String,Object> xml2map(String xml){ 
		Map<String,Object> map = new HashMap<String,Object>(); 
		Document document; 
		try { 
			document =  DocumentHelper.parseText(xml);  
			Element root = document.getRootElement(); 
			for(Iterator<Element> itrProperty=root.elementIterator(); itrProperty.hasNext();){ 
				Element element = itrProperty.next(); 
				String key = element.getName(); 
				String value = element.getTextTrim(); 
				map.put(key, value); 
			} 
		} catch (DocumentException e) { 
			e.printStackTrace(); 
		} 
		return map; 
	}

	/**
	 * 按key升序拼接
	 * @param map  数据源
	 * @param join key,value之间的拼接符(默认=)
	 * @param separator  separator 多个kv的分隔符(默认&amp;)
	 * @param ignoreEmpty 是否忽略空值
	 * @param sort 是否排序
	 * @return String(a=1&amp;b=2&amp;b=3)
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static String map2string(Map<String,?> map, String join, String separator, boolean ignoreEmpty, boolean sort){
		StringBuilder result = new StringBuilder();
		Set es = null;
		if(sort){
			SortedMap<String, Object> wrap = new TreeMap<String, Object>(map);
			es = wrap.entrySet();
		}else{
			es = map.entrySet();
		}
		Iterator it = es.iterator();
		while (it.hasNext()) {
			Map.Entry entry = (Map.Entry) it.next();
			String k = (String) entry.getKey();
			Object v = entry.getValue();
			if(ignoreEmpty && BasicUtil.isEmpty(v)) {
				continue;
			}
			if(v instanceof Collection){
				List list = new ArrayList();
				list.addAll((Collection)v);
				Collections.sort(list);
				for(Object item: list){
					if(ignoreEmpty && BasicUtil.isEmpty(item)) {
						continue;
					}
					if (result.length() > 0) {
						result.append(separator);
					}
					result.append(k).append(join).append(item);
				}
			}else if(v instanceof String[]){
				String vals[] = (String[])v;
				Arrays.sort(vals);
				for(String item:vals){
					if(ignoreEmpty && BasicUtil.isEmpty(item)) {
						continue;
					}
					if (result.length() > 0) {
						result.append(separator);
					}
					result.append(k).append(join).append(item);
				}
			}else{
				if (result.length() > 0) {
					result.append(separator);
				}
				result.append(k).append(join).append(v);
			}
		}
		return result.toString();
	}
	public static String map2string(Map<String,?> map){
		return map2string(map, "=","&",true, true);
	}
	public static <T> T xml2object(String xml, Class<T> clazz, boolean recursion){ 
		T obj = null; 
		try { 
			Map<String,?> map = xml2map(xml); 
			obj = map2object(map, clazz, recursion); 
		} catch (Exception e) { 
			e.printStackTrace(); 
		} 
		return obj; 
	} 
	public static <T> T xml2object(String xml, Class<T> clazz){ 
		return xml2object(xml,  clazz, true); 
	} 
	public static String object2xml(Object obj){ 
		if(null == obj){ 
			return null; 
		} 
		StringBuffer builder = new StringBuffer(); 
		builder.append("<xml>"); 
		List<Field> fields = BeanUtil.getFields(obj.getClass()); 
		for(Field field:fields){ 
			Object value = BeanUtil.getFieldValue(obj, field); 
			if(null == value){ 
				value = ""; 
			} 
			builder.append("<").append(field.getName()).append(">") 
			.append(value) 
			.append("</").append(field.getName()).append(">"); 
		} 
		builder.append("</xml>"); 
		return builder.toString(); 
	}
	public static Map<String,Object> object2map(Object obj){
		if(null == obj){
			return null;
		}
		Map<String,Object> map = new HashMap<String,Object>();
		List<Field> fields = BeanUtil.getFields(obj.getClass());
		for(Field field:fields){
			Object value = BeanUtil.getFieldValue(obj, field);
			if(null == value){
				value = "";
			}
			map.put(field.getName(), value);
		}
		return map;
	}
	public static Map<String,Object> object2map(Object obj, List<String> keys){
		if(null == obj){
			return null;
		}
		Map<String,Object> map = new HashMap<String,Object>();
		for(String key:keys){
			Object value = BeanUtil.getFieldValue(obj, key);
			if(null == value){
				value = "";
			}
			map.put(key, value);
		}
		return map;
	}
	public static <T> List<Map<String,Object>> objects2maps(Collection<T> objs, List<String> keys){
		List<Map<String,Object>> maps = new ArrayList<>();
		if(null != objs)
			for(T obj:objs){
				if(obj instanceof Map){
					Map<String,Object> item = new HashMap<>();
					for(String key:keys){
						item.put(key, ((Map)obj).get(key));
					}
					maps.add(item);
				}else {
					maps.add(object2map(obj, keys));
				}
			}
		return maps;
	}
	public static <T> List<Map<String,Object>> objects2maps(Collection<T> objs){
		List<Map<String,Object>> maps = new ArrayList<>();
		if(null != objs)
			for(T obj:objs){
				if(obj instanceof Map){
					maps.add((Map)obj);
				}else {
					maps.add(object2map(obj));
				}
			}
		return maps;
	}
	public static String object2json(Object obj, JsonInclude.Include include){
		if(null != obj) {
			try {
				if (null != include) {
					return newObjectMapper(include).writeValueAsString(obj);
				}
				return JSON_MAPPER.writeValueAsString(obj);
			} catch (JsonProcessingException e) {
				e.printStackTrace();
			}
		}
		return null;
	}
	public static String object2json(Object obj){
		return object2json(obj,null);
	}

	/**
	 * 参数转map
	 * 参数格式a=1&amp;b=2&amp;b=3
	 * 如果是多个值，以String的List形式保存
	 * 如果是url将根据问号分割
	 * @param url 参数或url
	 * @param empty 结果中是否包含空值,所有空值以""形式保存
	 * @return Map
	 */
	public static Map<String,Object> param2map(String url, boolean empty){
		Map<String,Object> params = new HashMap<String,Object>();
		if(null != url){
			int index = url.indexOf("?");
			if(index != -1) {
				url = url.substring(index);
			}
			String[] kvs = url.split("&");
			for(String kv:kvs){
				String k = null;
				String v = null;
				String[] tmps = kv.split("=");
				k = tmps[0];
				if(tmps.length>1){
					v = tmps[1];
				}
				if("null".equals(v)){
					v = "";
				}else if("NULL".equals(v)){
					v = null;
				}
				if(BasicUtil.isEmpty(v) && !empty){
					continue;
				}
				if(params.containsKey(k)){
					Object olds = params.get(k);
					List<String> vals = new ArrayList<>();
					if(null == olds){
						vals.add(null);
					}else if(olds instanceof String){
						vals.add(olds.toString());
					}else if(olds instanceof ArrayList){
						vals = (ArrayList)olds;
					}
					vals.add(v);
				}else{
					params.put(k,v);
				}

			}
		}
		return params;
	}
	/** 
	 * 提取集合中每个条目的key属性的值
	 * 如提取用户列表中的所有用户ID
	 * @param list  list
	 * @param key  key
	 * @return return
	 */ 
	public static List<Object> extract(Collection<?> list, String key){ 
		List<Object> values = new ArrayList<Object>(); 
		if(null != list){ 
			for(Object obj:list){ 
				Object value = BeanUtil.getFieldValue(obj, key); 
				values.add(value); 
			} 
		} 
		return values; 
	}
	/**
	 * 提取集合中每个条目的多个key属性的值
	 * 如提取用户列表中的所有用户ID,CODE
	 * @param list  list
	 * @param keys  keys
	 * @return return
	 */
	public static List<Map<String,Object>> extracts(Collection<?> list, String ... keys){ 
		List<Map<String,Object>> result = new ArrayList<Map<String,Object>>(); 
		if(null != list){ 
			for(Object obj:list){ 
				Map<String,Object> map = new HashMap<String,Object>(); 
				if(null !=keys){ 
					for(String key:keys){ 
						Object value = BeanUtil.getFieldValue(obj, key); 
						map.put(key, value); 
					} 
					result.add(map); 
				} 
			} 
		} 
		return result; 
	} 
	/** 
	 * 去重  
	 * @param <T> T
	 * @param list   list
	 * @param keys 根据keys列或属性值比较 
	 * @return   return
	 */
	public static <T> Collection<T> distinct(Collection<T> list, String ... keys){
		List<T> result = new ArrayList<T>();
		if(null != list){
			for(T obj:list){
				if(null == keys || keys.length==0){
					if(!result.contains(obj)){
						result.add(obj);
					}
				}else{
					if(contain(result, obj, keys)){
						result.add(obj);
					}
				}
			}
		}
		return result;
	}
	public static <T> Collection<T> distinct(Collection<T> list, List<String> keys){
		List<T> result = new ArrayList<T>();
		if(null != list){
			for(T obj:list){
				if(null == keys || keys.size()==0){
					if(!result.contains(obj)){
						result.add(obj);
					}
				}else{
					if(!contain(result, obj, keys)){
						result.add(obj);
					}
				}
			}
		}
		return result;
	}
	/** 
	 * 是否包含  
	 * @param <T> T
	 * @param list  list
	 * @param obj  obj
	 * @param keys 只比较keys列,基础类型不需要指定列 
	 * @return return
	 */
	public static <T> boolean contain(Collection<T> list, T obj, String ... keys){
		for(T item:list){
			if(equals(item, obj)){
				return true;
			}
		}
		return false;
	}
	public static <T> boolean contain(Collection<T> list, T obj, List<String> keys){
		for(T item:list){
			if(equals(item, obj, keys)){
				return true;
			}
		}
		return false;
	}

	public static <T> boolean equals(T obj1, T obj2, List<String> keys){
		if(null == keys || keys.size() == 0){
			if(null == obj1){
				if(null == obj2){
					return true;
				}else{
					return false;
				}
			}else if(!ClassUtil.isPrimitiveClass(obj1)){
				if(null == obj2){
					return false;
				}else{
					if(obj1.toString().equals(obj2.toString())){
						return true;
					}
				}
			}
			return false;
		}

		for(String key:keys){
			Object v1 = BeanUtil.getFieldValue(obj1, key);
			Object v2 = BeanUtil.getFieldValue(obj2, key);
			if(!equals(v1,v2)){
				return false;
			}

		}
		return true;
	}

	public static <T> boolean equals(T obj1, T obj2, String ... keys){ 
		return equals(obj1, obj2, BeanUtil.array2list(keys));
	} 
 
 
	/** 
	 * 数组拼接成字符串 
	 *  
	 * @param list   数组 
	 * @param split  分隔符 
	 * @param key key
	 * @return return
	 */ 
 
	public static String concat(List<?> list, String key, String split) {
		StringBuilder builder = new StringBuilder();
		if (null != list) {
			for(Object item:list){
				if(BasicUtil.isEmpty(item)){
					continue;
				}
				if (builder.length() > 0) {
					builder.append(split);
				}
				builder.append(item);
			}
		}
		return builder.toString(); 
	} 
	public static String concat(List<?> list, String split) {
		StringBuilder builder = new StringBuilder();
		if (null != list) {
			for(Object item:list){
				if(BasicUtil.isEmpty(item)){
					continue;
				}
				if (builder.length() > 0) {
					builder.append(split);
				}
				builder.append(item);
			}
		}
		return builder.toString(); 
	} 
	public static String concat(List<?> list) {
		return concat(list,",");
	}


	public static <T> String concat(T[] list, String key, String split) {
		StringBuilder builder = new StringBuilder();
		if (null != list) {
			for(Object item:list){
				if(BasicUtil.isEmpty(item)){
					continue;
				}
				if (builder.length() > 0) {
					builder.append(split);
				}
				builder.append(item);
			}
		}
		return builder.toString(); 
	} 
	public static <T> String concat(T[] list, String split) {
		StringBuilder builder = new StringBuilder();
		if (null != list) {
			for(Object item:list){
				if(BasicUtil.isEmpty(item)){
					continue;
				}
				if (builder.length() > 0) {
					builder.append(split);
				}
				builder.append(item);
			}
		}
		return builder.toString(); 
	} 
	public static <T> String concat(T[] list) {
		return concat(list,",");
	} 
 
 
	public static String concat(int[] list, String split) {
		StringBuilder builder = new StringBuilder();
		if (null != list) {
			for(Object item:list){
				if(BasicUtil.isEmpty(item)){
					continue;
				}
				if (builder.length() > 0) {
					builder.append(split);
				}
				builder.append(item);
			}
		}
		return builder.toString(); 
	} 
	public static String concat(int[] list) {
		return concat(list,",");
	} 
 
	public static String concat(long[] list, String split) {
		StringBuilder builder = new StringBuilder();
		if (null != list) {
			for(Object item:list){
				if(BasicUtil.isEmpty(item)){
					continue;
				}
				if (builder.length() > 0) {
					builder.append(split);
				}
				builder.append(item);
			}
		}
		return builder.toString(); 
	} 
	public static String concat(long[] list) {
		return concat(list,",");
	} 
 
	public static String concat(double[] list, String split) {
		StringBuilder builder = new StringBuilder();
		if (null != list) {
			for(Object item:list){
				if(BasicUtil.isEmpty(item)){
					continue;
				}
				if (builder.length() > 0) {
					builder.append(split);
				}
				builder.append(item);
			}
		}
		return builder.toString(); 
	} 
	public static String concat(double[] list) {
		return concat(list,",");
	} 
 
	public static String concat(float[] list, String split) {
		StringBuilder builder = new StringBuilder();
		if (null != list) {
			for(Object item:list){
				if(BasicUtil.isEmpty(item)){
					continue;
				}
				if (builder.length() > 0) {
					builder.append(split);
				}
				builder.append(item);
			}
		}
		return builder.toString(); 
	} 
	public static String concat(float[] list) {
		return concat(list,",");
	} 
 
	public static String concat(short[] list, String split) {
		StringBuilder builder = new StringBuilder();
		if (null != list) {
			for(Object item:list){
				if(BasicUtil.isEmpty(item)){
					continue;
				}
				if (builder.length() > 0) {
					builder.append(split);
				}
				builder.append(item);
			}
		}
		return builder.toString(); 
	} 
	public static String concat(short[] list) {
		return concat(list,",");
	} 
	 
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static Object toUpperCaseKey(Object obj, String ... keys){ 
		if(null == obj){ 
			return null; 
		} 
		if (obj instanceof String || obj instanceof Number || obj instanceof Boolean || obj instanceof Date) { 
			return obj; 
		} 
		if(obj instanceof Map){ 
			obj = toUpperCaseKey((Map<String,Object>)obj, keys); 
		}else if(obj instanceof Collection){ 
			obj = toUpperCaseKey((Collection)obj, keys); 
		} 
		return obj; 
	} 
	@SuppressWarnings("rawtypes")
	public static Collection toUpperCaseKey(Collection con, String ... keys){ 
		if(null == con){ 
			return con; 
		} 
		for(Object obj :con){ 
			obj = toUpperCaseKey(obj, keys); 
		} 
		return con; 
	} 
	public static Map<String,Object> toUpperCaseKey(Map<String,Object> map, String ... keys){ 
		if(null == map){ 
			return map; 
		} 
		List<String> ks = getMapKeys(map); 
		for(String k:ks){ 
			if(null == keys || keys.length == 0 || BasicUtil.containsString(keys, k)){ 
				Object v = map.get(k); 
				String key = k.toUpperCase(); 
				map.remove(k); 
				map.put(key, v); 
			} 
		} 
		return map; 
	}
	public static <T> T[] list2array(List<T> list){
		T[] result = (T[]) Array.newInstance(list.get(0).getClass(), list.size());
		int index = 0;
		for(T item:list){
			result[index++] = item;
		}
		return result;
	}

	/** 
	 * String 转map 
	 * @param str name:zhang,age:20 
	 * @return return
	 */ 
	public static Map<String,String> string2map(String str){ 
		Map<String,String> map = new HashMap<String,String>(); 
		if(BasicUtil.isNotEmpty(str)){ 
			if(str.startsWith("{") && str.endsWith("}")){ 
				str = str.substring(1, str.length()-1); 
			}
			String[] list = str.split(","); 
			for(String item:list){ 
				String[] kv = item.split(":"); 
				if(kv.length ==2){
					String k = kv[0];
					if(k.startsWith("\"") && k.endsWith("\"")){
						k = k.substring(1,k.length()-1);
					}
					String v = kv[1];
					if(v.startsWith("\"") && v.endsWith("\"")){
						v = v.substring(1,v.length()-1);
					}
					map.put(k,v);
				}else{
					map.put(item.replace(":", ""), null);
				}
			} 
		} 
		return map; 
	}  
	public static Map<String, String> createMap(String... params) { 
		Map<String, String> result = new HashMap<String, String>(); 
		if (null != params) { 
			int size = params.length; 
			for (int i = 0; i < size - 1; i += 2) { 
				String key = params[i]; 
				String value = params[i + 1]; 
				if (null == value) { 
					value = ""; 
				} 
				result.put(key.toString(), value); 
			} 
		} 
		return result; 
	}
	/**
	 * 删除空值  
	 * @param map  map
	 * @param recursion  是否递归检测集合map类型值的长度
	 */
	public static void clearEmpty(Map<String, Object> map, boolean recursion ){
		if(null == map){
			return;
		}
		List<String> keys = BasicUtil.getMapKeys(map);
		for(String key:keys){
			Object value = map.get(key);
			if(BasicUtil.isEmpty(recursion,value)){
				map.remove(key);
			}
		}
	}
	public static void clearEmpty(Map<String, Object> map){
		clearEmpty(map,true);
	}
	/**
	 * 删除空值
	 * @param list  list
	 * @param recursion  是否递归检测集合map类型值的长度
	 */
	public static void clearEmpty(List<Object> list, boolean recursion){
		if(null == list){
			return;
		}
		int size = list.size();
		for(int i=size-1;i>=0;i--){
			if(BasicUtil.isEmpty(recursion,list.get(i))){
				list.remove(i);
			}
		}
	}
	public static void clearEmpty(List<Object> list){
		clearEmpty(list, true);
	}
	/** 
	 * 多个数组合并成一个数组(二维数组合成一维数组) 
	 * @param <T> T
	 * @param first  first
	 * @param rest  rest
	 * @return return
	 */ 
	@SuppressWarnings("unchecked")
	public static <T> T[] union(T[] first, T[]... rest) { 
		int len = first.length; 
		for (T[] array : rest) { 
			len += array.length; 
		} 
		T[] result = Arrays.copyOf(first, len); 
		int offset = first.length; 
		for (T[] array : rest) { 
			System.arraycopy(array, 0, result, offset, array.length); 
			offset += array.length; 
		} 
		return result; 
	} 
 
 
	/** 
	 * 集合中与value差值最小的成员的下标
	 * @param array  array
	 * @param value  value
	 * @return return
	 */ 
	public static int closest(short[] array, short value){ 
		int index = 0; 
		int dif = -1; 
		int len = array.length; 
		for(int i=0; i<len; i++){ 
			if(array[i]==value){ 
				index = i; 
				break; 
			} 
			int abs = Math.abs(array[i]-value); 
			if(dif == -1 || dif > abs){ 
				dif = abs; 
				index = i; 
			} 
		} 
		return index; 
	} 
	public static int closest(Short[] array, short value){ 
		int index = 0; 
		int dif = -1; 
		int len = array.length; 
		for(int i=0; i<len; i++){ 
			if(array[i]==value){ 
				index = i; 
				break; 
			} 
			int abs = Math.abs(array[i]-value); 
			if(dif == -1 || dif > abs){ 
				dif = abs; 
				index = i; 
			} 
		} 
		return index; 
	} 
	public static int closest(List<Short> array, short value){ 
		int index = 0; 
		int dif = -1; 
		int len = array.size(); 
		for(int i=0; i<len; i++){ 
			if(array.get(i)==value){ 
				index = i; 
				break; 
			} 
			int abs = Math.abs(array.get(i)-value); 
			if(dif == -1 || dif > abs){ 
				dif = abs; 
				index = i; 
			} 
		} 
		return index; 
	} 
 
	public static int closest(int[] array, int value){ 
		int index = 0; 
		int dif = -1; 
		int len = array.length; 
		for(int i=0; i<len; i++){ 
			if(array[i]==value){ 
				index = i; 
				break; 
			} 
			int abs = Math.abs(array[i]-value); 
			if(dif == -1 || dif > abs){ 
				dif = abs; 
				index = i; 
			} 
		} 
		return index; 
	} 
	public static int closest(Integer[] array, int value){ 
		int index = 0; 
		int dif = -1; 
		int len = array.length; 
		for(int i=0; i<len; i++){ 
			if(array[i]==value){ 
				index = i; 
				break; 
			} 
			int abs = Math.abs(array[i]-value); 
			if(dif == -1 || dif > abs){ 
				dif = abs; 
				index = i; 
			} 
		} 
		return index; 
	} 
	public static int closest(List<Integer> array, int value){ 
		int index = 0; 
		int dif = -1; 
		int len = array.size(); 
		for(int i=0; i<len; i++){ 
			if(array.get(i)==value){ 
				index = i; 
				break; 
			} 
			int abs = Math.abs(array.get(i)-value); 
			if(dif == -1 || dif > abs){ 
				dif = abs; 
				index = i; 
			} 
		} 
		return index; 
	} 
 
	public static int closest(long[] array, long value){ 
		int index = 0; 
		long dif = -1; 
		int len = array.length; 
		for(int i=0; i<len; i++){ 
			if(array[i]==value){ 
				index = i; 
				break; 
			} 
			long abs = Math.abs(array[i]-value); 
			if(dif == -1 || dif > abs){ 
				dif = abs; 
				index = i; 
			} 
		} 
		return index; 
	} 
	public static int closest(Long[] array, long value){ 
		int index = 0; 
		long dif = -1; 
		int len = array.length; 
		for(int i=0; i<len; i++){ 
			if(array[i]==value){ 
				index = i; 
				break; 
			} 
			long abs = Math.abs(array[i]-value); 
			if(dif == -1 || dif > abs){ 
				dif = abs; 
				index = i; 
			} 
		} 
		return index; 
	} 
	public static int closest(List<Long> array, long value){ 
		int index = 0; 
		long dif = -1; 
		int len = array.size(); 
		for(int i=0; i<len; i++){ 
			if(array.get(i)==value){ 
				index = i; 
				break; 
			} 
			long abs = Math.abs(array.get(i)-value); 
			if(dif == -1 || dif > abs){ 
				dif = abs; 
				index = i; 
			} 
		} 
		return index; 
	} 
 
	public static int closest(float[] array, float value){ 
		int index = 0; 
		float dif = -1; 
		int len = array.length; 
		for(int i=0; i<len; i++){ 
			if(array[i]==value){ 
				index = i; 
				break; 
			} 
			float abs = Math.abs(array[i]-value); 
			if(dif == -1 || dif > abs){ 
				dif = abs; 
				index = i; 
			} 
		} 
		return index; 
	} 
	public static int closest(Float[] array, float value){ 
		int index = 0; 
		float dif = -1; 
		int len = array.length; 
		for(int i=0; i<len; i++){ 
			if(array[i]==value){ 
				index = i; 
				break; 
			} 
			float abs = Math.abs(array[i]-value); 
			if(dif == -1 || dif > abs){ 
				dif = abs; 
				index = i; 
			} 
			 
		} 
		return index; 
	} 
	public static int closest(List<Float> array, float value){ 
		int index = 0; 
		float dif = -1; 
		int len = array.size(); 
		for(int i=0; i<len; i++){ 
			if(array.get(i)==value){ 
				index = i; 
				break; 
			} 
			float abs = Math.abs(array.get(i)-value); 
			if(dif == -1 || dif > abs){ 
				dif = abs; 
				index = i; 
			} 
		} 
		return index; 
	} 
 
	public static int closest(double[] array, double value){ 
		int index = 0; 
		double dif = -1; 
		int len = array.length; 
		for(int i=0; i<len; i++){ 
			if(array[i]==value){ 
				index = i; 
				break; 
			} 
			double abs = Math.abs(array[i]-value); 
			if(dif == -1 || dif > abs){ 
				dif = abs; 
				index = i; 
			} 
		} 
		return index; 
	} 
	public static int closest(Double[] array, double value){ 
		int index = 0; 
		double dif = -1; 
		int len = array.length; 
		for(int i=0; i<len; i++){ 
			if(array[i]==value){ 
				index = i; 
				break; 
			} 
			double abs = Math.abs(array[i]-value); 
			if(dif == -1 || dif > abs){ 
				dif = abs; 
				index = i; 
			} 
			 
		} 
		return index; 
	} 
	public static int closest(List<Double> array, double value){ 
		int index = 0; 
		double dif = -1; 
		int len = array.size(); 
		for(int i=0; i<len; i++){ 
			if(array.get(i)==value){ 
				index = i; 
				break; 
			} 
			double abs = Math.abs(array.get(i)-value); 
			if(dif == -1 || dif > abs){ 
				dif = abs; 
				index = i; 
			} 
		} 
		return index; 
	} 
 
	public static int closest(BigDecimal[] array, BigDecimal value){ 
		int index = 0; 
		double dif = -1; 
		int len = array.length; 
		for(int i=0; i<len; i++){ 
			double abs = Math.abs(array[i].subtract(value).doubleValue()); 
			if(abs==0){ 
				index = i; 
				break; 
			} 
			if(dif == -1 || dif > abs){ 
				dif = abs; 
				index = i; 
			} 
		} 
		return index; 
	} 
	public static int closest(List<BigDecimal> array, BigDecimal value){ 
		int index = 0; 
		double dif = -1; 
		int len = array.size(); 
		for(int i=0; i<len; i++){ 
			double abs = Math.abs(array.get(i).subtract(value).doubleValue()); 
			if(abs==0){ 
				index = i; 
				break; 
			} 
			if(dif == -1 || dif > abs){ 
				dif = abs; 
				index = i; 
			} 
		} 
		return index; 
	}

	public static String parseFinalValue(Object obj, String key){
		return parseFinalValue(obj, key, "");
	}
	public static String parseFinalValue(Object obj, String key, String def){
		if(null == obj){ 
			return key;
		} 
		String value = key;
		if(BasicUtil.isNotEmpty(key)){ 
			if(key.contains("{")){ 
				try{ 
					List<String> ks =RegularUtil.fetch(key, "\\{\\w+\\}",Regular.MATCH_MODE.CONTAIN,0); 
					for(String k:ks){ 
						Object v = BeanUtil.getFieldValue(obj,k.replace("{", "").replace("}", "")); 
						if(null == v){ 
							v = ""; 
						} 
						value = value.replace(k, v.toString()); 
					} 
				}catch(Exception e){ 
					e.printStackTrace(); 
				} 
			} else { 
				Object val = BeanUtil.getFieldValue(obj, key);
				if(null != val){
					value = val.toString();
				}
			} 
		}
		if(BasicUtil.isEmpty(value)){
			value = def;
		}
		return value; 
	} 
	/** 
	 * 集合截取 
	 * @param <T>  t
	 * @param list  list
	 * @param begin  begin
	 * @param end  end
	 * @return return
	 */ 
	public static <T> List<T> cuts(Collection<T> list, int begin, int end){ 
		List<T> result = new ArrayList<T>(); 
		if(null != list){ 
			if(begin <=0){ 
				begin = 0; 
			} 
			if(end < 0 || end >= list.size()){ 
				end = list.size()-1; 
			} 
		} 
		int idx = 0; 
		for(T obj:list){ 
			if(idx >= begin && idx <= end){ 
				result.add(obj); 
			} 
			idx ++; 
		} 
		return result; 
	}

	/**
	 * 驼峰转下划线
	 * userName : user_name
	 * @param str src
	 * @return String
	 */
	public static String camel_(String str){
		if(null == str || str.contains("_")){
			return str;
		}
		Matcher matcher = Pattern.compile("[A-Z]").matcher(str);
		StringBuffer sb = new StringBuffer();
		while (matcher.find()) {
			String g = matcher.group();
			matcher.appendReplacement(sb, "_" + g.toLowerCase());
		}
		matcher.appendTail(sb);
		if (sb.charAt(0) == '_') {
			sb.delete(0, 1);
		}
		return sb.toString();
	}

	/**
	 * 转驼峰
	 * @param key src
	 * @param hold 是否保留分隔符
	 * @return String
	 */
	public static String camel(String key, boolean hold){
		if(!key.contains("-") && !key.contains("_")){
			return key;
		}
		String[] ks = key.split("_|-");
		String sKey = null;
		for(String k:ks){
			if(null == sKey){
				sKey = k.toLowerCase();
			}else{
				if(hold){
					sKey += "_";
				}
				sKey += CharUtil.toUpperCaseHeader(k.toLowerCase());
			}
		}
		return sKey;
	}


	public static String camel(String key){
		return camel(key, false);
	}
	public static String Camel(String key, boolean hold){
		if(!key.contains("-") && !key.contains("_")){
			return key;
		}
		String[] ks = key.split("_|-");
		String sKey = null;
		for(String k:ks){
			if(null == sKey){
				sKey = CharUtil.toUpperCaseHeader(k.toLowerCase());
			}else{
				if(hold){
					sKey += "_";
				}
				sKey +=  CharUtil.toUpperCaseHeader(k.toLowerCase());
			}
		}
		return sKey;
	}

	public static String Camel(String key){
		return Camel(key, false);
	}
	/** 
	 * 解析 key:vlue形式参数age:20 
	 * 返回数组["age","20"] 
	 * 如果值为空返回["age",""] 
	 * 如果没有分隔符返回["age","age"] 
	 * @param src  src
	 * @return return
	 */ 
	public static String[] parseKeyValue(String src){ 
		if(BasicUtil.isEmpty(src)){ 
			return null; 
		} 
		int len = 2; 
		String[] result = null; 
		String key1 = src; 
		String key2 = src;
		if(src.contains(":")){
			String tmp[] = src.split(":"); 
			len = NumberUtil.max(len, tmp.length); 
			result = new String[len]; 
			key1 = tmp[0]; 
			if(tmp.length>1){ 
				key2 = tmp[1]; 
			}else{ 
				key2 = ""; 
			} 
			for(int i=2; i<len; i++){ 
				result[i] = tmp[i]; 
			} 
		}else{ 
			result = new String[2]; 
		} 
		result[0] = key1; 
		result[1] = key2; 
		return result; 
	} 
   public static boolean isJson(Object json){
	   if(null == json){
		   return false;
	   }
	   try{
		   JSON_MAPPER.readTree(json.toString());
	   }catch(Exception e){
		   return false;
	   }
	   return true;
   }

	public static Object value(JsonNode json){
		if(null == json){
			return null;
		}else{
			if(json.isNull()){
				return null;
			}else if(json.isInt()){
				return json.asInt();
			}else if(json.isBoolean()){
				return json.asBoolean();
			}else if(json.isDouble()){
				return json.asDouble();
			}else if(json.isLong()){
				return json.asLong();
			}else{
				return json.asText();
			}
		}
	}

	/**
	 * 递归提取src中的value
	 * 如
	 * {
	 *     key1:{
	 *         key11:{
	 *             key111:111   //提取111 recursion(map,key1,key11,key111)
	 *         },
	 *         key12:{
	 *             key121:{
	 *                 key1211:1211,
	 *                 key1212:1212 //提取1212 recursion(map,key1, key12, key121, key1212)
	 *             }
	 *         }
	 *     }
	 * }
	 * @param src 数据源
	 * @param voluntary 遇到基础类型是否停止(不取下一级)
	 *                  voluntary=false时遇到提取基础类型属性值时返回null
	 *                  voluntary=true时遇到提取基础类型属性值时返回当前value并return value
	 * @param keys keys 一级key.二级key.三级key
	 * @return Object
	 */
	public static Object extract(Object src, boolean voluntary, String ... keys) {
		if(null == keys || keys.length ==0){
			return null;
		}
		Object result  = src;
		for (String key : keys) {
			if(null != result) {
				if (ClassUtil.isWrapClass(result) && !(result instanceof String)) {
					result = getFieldValue(result, key);
				}else{
					if(voluntary){
						return result;
					}else {
						result = null;
					}
				}
			}
		}
		return result;
	}
	public static Object extract(Object src, String ... keys) {
		return extract(src, false, keys);
	}
	/**
	 * 取第一个不为空的值
	 * @param src 数据源
	 * @param keys keys
	 * @return Object
	 */
	public static Object nvl(Object src, String ... keys){
		Object value = null;
		if(null == src || null == keys){
			return value;
		}
		for(String key:keys){
			value = getFieldValue(src, key);
			if(null != value){
				return value;
			}
		}
		return value;
	}

	public static Object evl(Object src, String ... keys){
		Object value = null;
		if(null == src || null == keys){
			return value;
		}
		for(String key:keys){
			value = getFieldValue(src, key);
			if(BasicUtil.isNotEmpty(value)){
				return value;
			}
		}
		return value;
	}
	/**
	 * 提取第一个不为空的value
	 * @param map map
	 * @param keys keys
	 * @return String
	 */
	public static Object propertyNvl(Map<String,?> map, String ... keys){
		Object value = null;
		if(null == map || null == keys){
			return value;
		}
		for(String key:keys){
			value = map.get(key);
			if(null != value){
				return value;
			}
			//以下划线分隔的key
			String[] ks = key.split("_");
			String sKey = null;
			for(String k:ks){
				if(null == sKey){
					sKey = k;
				}else{
					sKey = sKey + CharUtil.toUpperCaseHeader(k);
				}
			}
			value = map.get(sKey);
			if(null != value){
				return value;
			}
			//以中划线分隔的key
			ks = key.split("-");
			sKey = null;
			for(String k:ks){
				if(null == sKey){
					sKey = k;
				}else{
					sKey = sKey + CharUtil.toUpperCaseHeader(k);
				}
			}
			value = map.get(sKey);
			if(null != value){
				return value;
			}
		}
		return value;
	}
	/**
	 * 设置所有属性值
	 * @param obj obj
	 * @param map map
	 */
    public static void setFieldsValue(Object obj, Map<String,?> map ){
    	if(null != map && null != obj) {
			List<String> fields = BeanUtil.getFieldsName(obj.getClass());
			for (String field : fields) {
				Object value = propertyNvl(map, field);
				if (BasicUtil.isNotEmpty(value)) {
					BeanUtil.setFieldValue(obj, field, value);
				}
			}
		}
    }

	public static byte[] serialize(Object value) {
		byte[] rv=new byte[0];
		if (value == null) {
			return rv;
		}
		ByteArrayOutputStream bos = null;
		ObjectOutputStream os = null;
		try {
			bos = new ByteArrayOutputStream();
			os = new ObjectOutputStream(bos);
			os.writeObject(value);
			os.close();
			bos.close();
			rv = bos.toByteArray();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if(os!=null)os.close();
				if(bos!=null)bos.close();
			}catch (Exception e2) {
				e2.printStackTrace();
			}
		}
		return rv;
	}

	public static Object deserialize(byte[] in) {
		Object rv=null;
		ByteArrayInputStream bis = null;
		ObjectInputStream is = null;
		try {
			if(in != null) {
				bis=new ByteArrayInputStream(in);
				is=new ObjectInputStream(bis);
				rv=is.readObject();
				is.close();
				bis.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}finally {
			try {
				if(is!=null)is.close();
				if(bis!=null)bis.close();
			} catch (Exception e2) {
				e2.printStackTrace();
			}
		}
		return rv;
	}

	public static void join(Map src, Map copy){
		join(src, copy, false);
	}
	public static void join(Map src, Map copy, boolean over){
    	if(null != src && null != copy){
    		for(Object key:copy.keySet()){
    			if(!over && src.containsKey(key)){
    				continue;
				}
    			src.put(key, copy.get(key));
			}
		}
	}

	public static <T> List<T> array2list(T[] ... arrays){
		List<T> list = new ArrayList<T>();
		if(null != arrays) {
			for (T[] array : arrays) {
				if(null != array) {
					for (T item : array) {
						list.add(item);
					}
				}
			}
		}
		return list;
	}
	/**
	 * 合成笛卡尔组合
	 *
	 * @param lists  二维数组
	 * @param <T> t
	 * @return return
	 * 输入:
	 * [[A,B,C],[1,2,3]]
	 * 输出:
	 *  [[A, 1], [A, 2], [A, 3]
	 * , [B, 1], [B, 2], [B, 3]
	 * , [C, 1], [C, 2], [C, 3]]
	 *
	 * 输入:
	 * [[A,B,C],[1,2,3],[一,二,三]]
	 * 输出:
	 *  [[A, 1, 一], [A, 1, 二], [A, 1, 三], [A, 2, 一], [A, 2, 二], [A, 2, 三], [A, 3, 一], [A, 3, 二], [A, 3, 三]
	 * , [B, 1, 一], [B, 1, 二], [B, 1, 三], [B, 2, 一], [B, 2, 二], [B, 2, 三], [B, 3, 一], [B, 3, 二], [B, 3, 三]
	 * , [C, 1, 一], [C, 1, 二], [C, 1, 三], [C, 2, 一], [C, 2, 二], [C, 2, 三], [C, 3, 一], [C, 3, 二], [C, 3, 三]
	 * ]
	 */
	public static <T> List<List<T>> descartes(List<List<T>> lists) {
		List<List<T>> result = new ArrayList<List<T>>();
		if(null == lists || lists.size()==0){
			return result;
		}
		List<T> st = lists.get(0);
		for (T t : st) {
			List<T> tmp = new ArrayList<T>();
			tmp.add(t);
			result.add(tmp);
		}
		List<List<T>> store = new ArrayList<List<T>>();
		for (int i = 1; i < lists.size(); i++) {
			List<T> r2 = lists.get(i);
			for (int j = 0; j < result.size(); j++) {
				List<T> rns = result.get(j);
				for (int k = 0; k < r2.size(); k++) {
					List<T> mid = new ArrayList<T>();
					mid.addAll(rns);
					mid.add(r2.get(k));
					store.add(mid);
				}
			}
			result = new ArrayList<List<T>>();
			result.addAll(store);
			store = new ArrayList<List<T>>();
		}
		return result;
	}

	public static <T> List<List<T>> descartes(List<T> ... lists) {
		return descartes(array2list(lists));
	}

	/**
	 * 数组合并
	 * @param array 第一个数组
	 * @param items 其他数组
	 * @param <T> 数据类型
	 * @return 合并后数组
	 */
	public static <T> T[] merge(T[] array, T[]... items) {
		T[] result = null;
		int len = array.length;
		for (T[] item : items) {
			if(null != item) {
				len += array.length;
			}
		}
		if(null != array) {
			result = Arrays.copyOf(array, len);
		}else{
			result = (T[]) new Object[len];
		}
		int offset = array.length;
		for (T[] item : items) {
			if(null != item) {
				System.arraycopy(item, 0, result, offset, item.length);
				offset += item.length;
			}
		}
		return result;
	}

	public static <T> List<T> merge(List<T> list, T... items) {
		List<T> result = new ArrayList<>();
		if(null != list){
			result.addAll(list);
		}
		if(null != items){
			for(T item:items){
				result.add(item);
			}
		}
		return result;
	}
	public static <T> List<T> join(List<T> list, T... items) {
		if(null == list){
			list = new ArrayList<>();
		}
		if(null != items){
			for(T item:items){
				list.add(item);
			}
		}
		return list;
	}


	public static String parseRuntimeValue(Object obj, String key){
		return BeanUtil.parseRuntimeValue(obj, key, false);
	}
	public static String parseRuntimeValue(Object obj, String key, boolean encrypt){
		if(null == obj){
			return key;
		}
		String value = key;
		if(BasicUtil.isNotEmpty(key)){
			if(key.contains("{")){
				value = BeanUtil.parseFinalValue(obj, key);
			} else {
				Object val = BeanUtil.getFieldValue(obj, key);
				if(null != val){
					value = val.toString();
					if (encrypt) {
						value = DESUtil.encryptValue(value + "");
					}
				}else{
					value = null;
				}
			}
		}
		if(ConfigTable.isDebug() && log.isWarnEnabled()){
			//log.warn("[parse run time value][key:"+key+"][value:"+value+"]");
		}
		return value;
	}

	public static Map<String,Object> copy(Map<String,Object> src, Map<String,Object> copy, List<String> keys){
		if(null == copy ){
			return  src;
		}
		if(null != keys) {
			for (String key : keys) {
				String ks[] = BeanUtil.parseKeyValue(key);
				src.put(ks[0], copy.get(ks[1]));
			}
		}
		return src;
	}
	public static Map<String,Object> copy(Map<String,Object> src, Map<String,Object> copy, String ... keys){
		if(null == copy ){
			return  src;
		}
		if(null != keys) {
			for (String key : keys) {
				String ks[] = BeanUtil.parseKeyValue(key);
				src.put(ks[0], copy.get(ks[1]));
			}
		}
		return src;
	}
	public static Map<String,Object> copy(Map<String,Object> src, Map<String,Object> copy){
		return copy(src, copy, getMapKeys(copy));
	}
	public static <T> T query(Collection<T> datas, Map<String,Object> kvs){
		List<T> list = querys(datas,0,1, kvs);
		if(list.size()>0){
			return list.get(0);
		}
		return null;
	}

	public static <T> List<T> querys(Collection<T> datas, int begin, String... params) {
		return querys(datas,begin, 0, params);
	}

	public static <T> List<T> querys(Collection<T> datas, String... params) {
		return querys(datas,0, params);
	}

	public static <T> List<T> querys(Collection<T> datas, int begin, int qty, String... params) {
		Map<String, Object> kvs = new HashMap<>();
		int len = params.length;
		int i = 0;
		String srcFlagTag = "srcFlag"; //参数含有{}的 在kvs中根据key值+tag 放入一个新的键值对
		while (i < len) {
			String p1 = params[i];
			if (BasicUtil.isEmpty(p1)) {
				i++;
				continue;
			} else if (p1.contains(":")) {
				String ks[] = BeanUtil.parseKeyValue(p1);
				kvs.put(ks[0], ks[1]);
				i++;
				continue;
			} else {
				if (i + 1 < len) {
					String p2 = params[i + 1];
					if (BasicUtil.isEmpty(p2) || !p2.contains(":")) {
						kvs.put(p1, p2);
						i += 2;
						continue;
					} else if (p2.startsWith("{") && p2.endsWith("}")) {
						p2 = p2.substring(1, p2.length() - 1);
						kvs.put(p1, p2);
						kvs.put(p1 + srcFlagTag, "true");
						i += 2;
						continue;
					} else {
						String ks[] = BeanUtil.parseKeyValue(p2);
						kvs.put(ks[0], ks[1]);
						i += 2;
						continue;
					}
				}

			}
			i++;
		}
		return querys(datas, begin, qty, kvs);
	}


	public static <T> List<T> querys(Collection<T> datas, int begin, int qty, Map<String, Object> kvs) {
		List<T> set = new ArrayList<>();
		String srcFlagTag = "srcFlag"; //参数含有{}的 在kvs中根据key值+tag 放入一个新的键值对
		BigDecimal d1;
		BigDecimal d2;
		for (T row:datas) {
			boolean chk = true;//对比结果
			for (String k : kvs.keySet()) {
				boolean srcFlag = false;
				if (k.endsWith(srcFlagTag)) {
					continue;
				} else {
					String srcFlagValue = kvs.get(k + srcFlagTag)+"";
					if (BasicUtil.isNotEmpty(srcFlagValue)) {
						srcFlag = true;
					}
				}
				String v = kvs.get(k)+"";
				Object value = getFieldValue(row,k);

				if (null == v) {
					if (null != value) {
						chk = false;
						break;
					}else{
						chk = true;
						break;
					}
				} else {
					if (null == value) {
						chk = false;
						break;
					}
					String str = value + "";
					str = str.toLowerCase();
					v = v.toLowerCase();
					if (srcFlag) {
						v = "{" + v + "}";
					}
					if (!v.equals(str)) {
						chk = false;
						break;
					}

				}
			}//end for kvs
			if (chk) {
				set.add(row);
				if (qty > 0 && set.size() >= qty) {
					break;
				}
			}
		}//end for rows
		return set;
	}
	private static String concatValue(Map<String,Object> row, String split){
		StringBuilder builder = new StringBuilder();
		List<String> keys = getMapKeys(row);
		for(String key:keys){
			if(builder.length() > 0){
				builder.append(split);
			}
			builder.append(row.get(key));
		}
		return builder.toString();
	}
	/**
	 * 行转列
	 * 表结构(编号, 姓名, 年度, 科目, 分数, 等级)
	 * @param datas      数据集
	 * @param pks       唯一标识key(如编号,姓名)
	 * @param classKeys 分类key(如年度,科目)
	 * @param valueKeys 取值key(如分数,等级),如果不指定key则将整行作为value
	 * @param <T> 数据类型
	 * @return
	 * 如果指定key
	 * 返回结构 [
	 *      {编号:01,姓名:张三,2010-数学-分数:100},
	 *      {编号:01,姓名:张三,2010-数学-等级:A},
	 *      {编号:01,姓名:张三,2010-物理-分数:100}
	 *  ]
	 *  如果只有一个valueKey则返回[
	 *      {编号:01,姓名:张三,2010-数学:100},
	 *      {编号:01,姓名:张三,2010-物理:90}
	 *  ]
	 * 不指定valuekey则返回 [
	 *      {编号:01,姓名:张三,2010-数学:{分数:100,等级:A}},
	 *      {编号:01,姓名:张三,2010-物理:{分数:100,等级:A}}
	 *  ]
	 */
	public static <T> List<Map<String,Object>> pivot(Collection<T> datas, List<String> pks, List<String> classKeys, List<String> valueKeys) {
		List<Map<String,Object>> result = objects2maps(distinct(datas,pks),pks);
		List<Map<String,Object>> classValues = objects2maps(distinct(datas,classKeys),classKeys);  //[{年度:2010,科目:数学},{年度:2010,科目:物理},{年度:2011,科目:数学}]
		for (Map<String,Object> row : result) {
			for (Map<String,Object> classValue : classValues) {
				Map<String,Object> params = new HashMap<>();
				copy(params, row, pks);
				copy(params, classValue);
				T valueRow = query(datas,params);
				String finalKey = concatValue(classValue,"-");//2010-数学
				if(null != valueKeys && valueKeys.size() > 0){
					if(valueKeys.size() == 1){
						if (null != valueRow) {
							row.put(finalKey, getFieldValue(valueRow, valueKeys.get(0)));
						} else {
							row.put(finalKey, null);
						}
					}else {
						for (String valueKey : valueKeys) {
							//{2010-数学-分数:100;2010-数学-等级:A}
							if (null != valueRow) {
								row.put(finalKey + "-" + valueKey, getFieldValue(valueRow, valueKey));
							} else {
								row.put(finalKey + "-" + valueKey, null);
							}
						}
					}
				}else{
					if (null != valueRow){
						row.put(finalKey, valueRow);
					}else{
						row.put(finalKey, null);
					}
				}
			}
		}
		return result;
	}

	public static <T> List<Map<String,Object>> pivot(Collection<T> datas, String[] pks, String[] classKeys, String[] valueKeys) {
		return pivot(datas, BeanUtil.array2list(pks),BeanUtil.array2list(classKeys),BeanUtil.array2list(valueKeys));
	}
	/**
	 * 行转列
	 * @param datas    数据
	 * @param pk       唯一标识key(如姓名)多个key以,分隔如(编号,姓名)
	 * @param classKey 分类key(如科目)多个key以,分隔如(科目,年度)
	 * @param valueKey 取值key(如分数)多个key以,分隔如(分数,等级)
	 * @param <T> 数据类型
	 * @return
	 *  表结构(姓名,科目,分数)
	 *  返回结构 [{姓名:张三,数学:100,物理:90,英语:80},{姓名:李四,数学:100,物理:90,英语:80}]
	 */
	public static <T> List<Map<String,Object>> pivot(Collection<T> datas, String pk, String classKey, String valueKey) {
		List<String> pks = BeanUtil.array2list(pk.trim().split(","));
		List<String> classKeys = BeanUtil.array2list(classKey.trim().split(","));
		List<String> valueKeys = BeanUtil.array2list(valueKey.trim().split(","));
		return pivot(datas, pks, classKeys, valueKeys);
	}
	public static <T> List<Map<String,Object>> pivot(Collection<T> datas, String pk, String classKey) {
		List<String> pks = BeanUtil.array2list(pk.trim().split(","));
		List<String> classKeys = BeanUtil.array2list(classKey.trim().split(","));
		List<String> valueKeys = new ArrayList<>();
		return pivot(datas, pks, classKeys, valueKeys);
	}

	public static <T> List<Map<String,Object>> pivot(Collection<T> datas, List<String> pks, List<String> classKeys, String ... valueKeys) {
		List<String> list = new ArrayList<>();
		if(null != valueKeys){
			for(String item:valueKeys){
				list.add(item);
			}
		}
		return pivot(datas, pks, classKeys, valueKeys);
	}
	private String concatValue(DataRow row, String split){
		StringBuilder builder = new StringBuilder();
		List<String> keys = row.keys();
		for(String key:keys){
			if(builder.length() > 0){
				builder.append(split);
			}
			builder.append(row.getString(key));
		}
		return builder.toString();
	}
	private static String[] kvs(Map<String,Object> row){
		List<String> keys = getMapKeys(row);
		int size = keys.size();
		String[] kvs = new String[size*2];
		for(int i=0; i<size; i++){
			String k = keys.get(i);
			String value = null;
			Object v = row.get(k);
			if(null != v){
				value = v.toString();
			}
			kvs[i*2] = k;
			kvs[i*2+1] = value;
		}
		return kvs;
	}
} 
