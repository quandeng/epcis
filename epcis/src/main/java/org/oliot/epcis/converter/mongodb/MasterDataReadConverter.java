package org.oliot.epcis.converter.mongodb;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.bson.BsonArray;
import org.bson.BsonDocument;
import org.bson.BsonType;
import org.bson.BsonValue;
import org.oliot.epcis.configuration.Configuration;
import org.oliot.model.epcis.AttributeType;
import org.oliot.model.epcis.IDListType;
import org.oliot.model.epcis.VocabularyElementListType;
import org.oliot.model.epcis.VocabularyElementType;
import org.oliot.model.epcis.VocabularyType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

/**
 * Copyright (C) 2014-2016 Jaewook Byun
 *
 * This project is part of Oliot open source (http://oliot.org). Oliot EPCIS
 * v1.2.x is Java Web Service complying with Electronic Product Code Information
 * Service (EPCIS) v1.2.
 *
 * @author Jaewook Byun, Ph.D student
 * 
 *         Korea Advanced Institute of Science and Technology (KAIST)
 * 
 *         Real-time Embedded System Laboratory(RESL)
 * 
 *         bjw0829@kaist.ac.kr, bjw0829@gmail.com
 */

/**
* Modifications copyright (C) 2019 Quan Deng
* 
*/

public class MasterDataReadConverter {
    private static Logger log = LoggerFactory.getLogger(MasterDataReadConverter.class);


	public VocabularyType convert(BsonDocument dbObject) {
		try {
			VocabularyType vt = new VocabularyType();

			if (dbObject.get("type") != null)
				vt.setType((String) dbObject.getString("type").getValue());

			VocabularyElementListType velt = new VocabularyElementListType();
			List<VocabularyElementType> vetList = new ArrayList<VocabularyElementType>();

			VocabularyElementType vet = new VocabularyElementType();
			BsonDocument attrObj = dbObject.getDocument("attributes");
			List<AttributeType> attrListType = new ArrayList<AttributeType>();
			if (attrObj != null) {
				Iterator<String> attrIter = attrObj.keySet().iterator();

				DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
				DocumentBuilder builder = dbf.newDocumentBuilder();
				Document doc = builder.newDocument();

				if (dbObject.containsKey("id"))
					vet.setId(dbObject.getString("id").getValue());

				while (attrIter.hasNext()) {
					String key = attrIter.next();
					BsonValue attrValue = attrObj.get(key);
					BsonType attrValueType = attrObj.get(key).getBsonType();
					key = decodeMongoObjectKey(key);

					if (attrValueType == BsonType.ARRAY) {
						BsonArray attrArray = attrValue.asArray();
						Iterator<BsonValue> bsonValueIterator = attrArray.iterator();
						while (bsonValueIterator.hasNext()) {
							AttributeType attrType = MongoReaderUtil.getAttributeType(doc, key,
									bsonValueIterator.next());
							attrListType.add(attrType);
						}
					} else if (attrValueType == BsonType.DOCUMENT) {
						AttributeType attrType = new AttributeType();
						attrType.setId(key);
						attrType.setContent(MongoReaderUtil.putAny(attrValue.asDocument(), null));
						attrListType.add(attrType);
					} else {
						AttributeType attrType = MongoReaderUtil.getAttributeType(doc, key, attrValue);
						attrListType.add(attrType);
					}
				}
			}
			vet.setAttribute(attrListType);

			IDListType idListType = new IDListType();
			List<String> idList = new ArrayList<String>();
			if (dbObject.containsKey("children")) {
				BsonArray childList = dbObject.getArray("children");
				if (childList != null && childList.isEmpty() == false) {
					Iterator<BsonValue> childIter = childList.iterator();
					while (childIter.hasNext()) {
						idList.add(childIter.next().asString().getValue());
					}
					idListType.setId(idList);
				}
				vet.setChildren(idListType);
			}
			vetList.add(vet);
			velt.setVocabularyElement(vetList);
			vt.setVocabularyElementList(velt);
			return vt;
		} catch (ParserConfigurationException e) {
			log.error(e.toString());
		}
		return null;
	}

	public String encodeMongoObjectKey(String key) {
		key = key.replace(".", "\uff0e");
		return key;
	}

	public String decodeMongoObjectKey(String key) {
		key = key.replace("\uff0e", ".");
		return key;
	}
}
