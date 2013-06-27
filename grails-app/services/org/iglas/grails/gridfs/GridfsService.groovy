package org.iglas.grails.gridfs

import org.codehaus.groovy.grails.web.taglib.exceptions.GrailsTagException
import org.iglas.grails.utils.UserConfig

import com.mongodb.BasicDBObject
import com.mongodb.DB
import com.mongodb.DBCollection
import com.mongodb.DBObject
import com.mongodb.Mongo
import com.mongodb.gridfs.GridFS
import com.mongodb.gridfs.GridFSDBFile
import com.mongodb.gridfs.GridFSInputFile

class GridfsService {
    public static total
    static String configName = "gridfsConfig"
    public static list(params) {
        def result = []
        def config = new UserConfig(configName).get(params)

        BasicDBObject query = new BasicDBObject()
        if(params?.parentclass)
            query.put("metadata.parentclass",params.parentclass)
        if(params?.idparent)
            query.put( "metadata.idparent",params.idparent,)
        GridFS gfsFiles =  getStore()
        if(query)
            result = gfsFiles.getFileList(query)
        else
            result = gfsFiles.getFileList()

        if(params?.max)
            result.limit((int)params.max)
        if(params?.offset)
            result.skip((int)params.offset)
        total = result.count()

        if(params?.relative){
            result = result.toArray()
            for(def key=0;key<result.size();key++){
                //  println(key)
                Map resultMap =  [:]
                DBObject item  = result[key]

                resultMap["size"] = item.get("length")
                resultMap["filename"] = item.get("filename")
                resultMap["contentType"] = item.get("contentType")
                resultMap["uploadDate"] = item.getUploadDate()
                resultMap["metadata"] = item.getMetaData()
                resultMap["id"] = item.getId().toString()

                DBObject  metadataItem = item.getMetaData()
                if(metadataItem)
                    metadataItem.each{key_meta,value ->
                        resultMap.put(key_meta+"_metadata" , value)
                    }
                if(params?.icon ){
                    resultMap.put('iconUrl' , new UtilsService().getIconForFile(item ,params))
                }
                result[key] = resultMap
            }
        }

        result

    }
	
	public boolean exists(config, filename) {
		getGridFS(config).findOne(filename) != null
	}
	public def addToGridFS(config,file, filename) {
		(GridFSInputFile) getGridFS(config).createFile(file.getInputStream(),filename)
	}
	public def addToGridFS(config, file, filename, params) {
		def inputFile = addToGridFS(config, file, filename)
		setMetaData(params, inputFile)
	}
	public def attemptUpload(config, gfsFile) {
		def accessResult = true
		def access
		if (config.accessClass && config.accessMethod){
			access = Class.forName(config.accessClass  ,true,Thread.currentThread().contextClassLoader).newInstance()
			accessResult = access."${config.accessMethod}"(gfsFile,"upload")
		}
		if (accessResult) {
			gfsFile.save()
		}
		[isAllowed: accessResult, msg: access?.message]
	}
	private def getGridFS(config) {
		Mongo mongo = new Mongo(config.db.host)
		DB db  = mongo.getDB(config.db.name)
		DBCollection col = db.getCollection(config.db.collection + ".files")
		col.ensureIndex(new BasicDBObject(config.indexes))
		new GridFS(db, config.db.collection)
	}
	private def setMetaData(params, gfsFile) {
		DBObject metadata = new BasicDBObject()
		metadata.put("idparent",params.idparent)
		metadata.put("originalFilename",file.originalFilename.toLowerCase())
		metadata.put("fileExtension",fileExtension.toLowerCase())

		if(params?.text)
			metadata.put("text",params?.text)
		if(params["accesspolitic"])
			metadata.put("access",params["accesspolitic"])
		else
			metadata.put("access","public")

		gfsFile.setMetaData(metadata)
		gfsFile
	}
	
	

    public static GridFS getStore() {
        def config = new UserConfig(configName).get()
        GridFS gfsFiles
        try {
            Mongo mongo = new Mongo(config.db.host)
            DB db  = mongo.getDB(config.db.name)
            gfsFiles = new GridFS(db, config.db.collection )
        }catch (Exception e){
            println(e.message)
            def errorMsg = "Connection to database error: ${e.message}."
            throw new GrailsTagException(errorMsg)
        }
        gfsFiles
    }
    public static GridFSDBFile get(params) {
        def config = new UserConfig(configName).get(params)
        String FileName = params.filename
        GridFSDBFile fileForOutput
        try {
            GridFS gfsFiles = getStore()
            fileForOutput = gfsFiles.findOne(FileName)
            fileForOutput
        }catch (Exception e){
            println(e.message)
            fileForOutput = new GridFSDBFile()
        }
        fileForOutput
    }
    public static boolean remove(params){
        def config = new UserConfig(configName).get(params)
        String FileName = params.filename
        try {
            GridFS gfsFiles =  getStore()
            GridFSDBFile gfsFile =  gfsFiles.findOne(FileName)
            UtilsService.deleteIcons(gfsFile)
            gfsFiles.remove(FileName)
        }catch (e){
            println(e.message)
            return false
        }
        true
    }
    public static boolean updateMetaData(params){
        def config = new UserConfig(configName).get(params)
        String FileName = params.filename
        try {
            GridFSDBFile gfsFile =  get(filename: FileName)
            if(params?.metadata && params.metadata instanceof Map){
                DBObject metaData =  gfsFile.getMetaData()
                params.metadata.each{key,val ->
                    metaData[key] = val
                }
                gfsFile.setMetaData(metaData)
                gfsFile.save()
            }

        }catch (e){
            println(e.message)
            return false
        }
        true
    }

}
