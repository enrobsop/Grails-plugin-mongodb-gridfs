package org.iglas.grails.gridfs

import org.bson.types.ObjectId
import org.codehaus.groovy.grails.web.taglib.exceptions.GrailsTagException
import org.iglas.grails.utils.UserConfig

import com.mongodb.DB
import com.mongodb.Mongo
import com.mongodb.gridfs.GridFS
import com.mongodb.gridfs.GridFSDBFile

class GridfsHelperService {
	
//	private GridFS getStore() {
//		def config = new UserConfig(GridfsService.configName).get()
//		GridFS gfsFiles
//		try {
//			Mongo mongo = new Mongo(config.db.host)
//			DB db  = mongo.getDB(config.db.name)
//			gfsFiles = new GridFS(db, config.db.collection )
//		}catch (Exception e){
//			println(e.message)
//			def errorMsg = "Connection to database error: ${e.message}."
//			throw new GrailsTagException(errorMsg)
//		}
//		gfsFiles
//	}

	
	GridFSDBFile findOne(ObjectId oid) { 
//		store.findOne(oid)
	}

}
