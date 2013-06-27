package org.iglas.grails.gridfs

import org.codehaus.groovy.grails.validation.Validateable
import org.iglas.grails.utils.ConfigHelper
import org.springframework.web.multipart.MultipartFile

import com.mongodb.BasicDBObject
import com.mongodb.DBObject

@Validateable
class UploadFileCommand {

	transient def config
	transient def gridfsService
	
	String 			idparent
	MultipartFile	file
	
	String parentclass
	String text
	String accesspolitic	= "public"
	
	String id
	String successController
	String successAction
	String successType
	
	static constraints = {
		file				nullable:false
		idparent 			nullable:false
		parentclass			nullable:true
		text				nullable:true
		accesspolitic		nullable:true
		successController	nullable:true
		successAction		nullable:true
		successType			nullable:true
		id					nullable:true
	}
	
	void setAccesspolitic(String val) {
		this.accesspolitic = val ?: 'public'
	}
	
	def getOriginalFilename() {
		file?.originalFilename
	}
	
	def getFileExtension() {
		file.originalFilename.substring(file.originalFilename.lastIndexOf('.')+1)
	}
	
	def getTargetFilename() {
		String newFileName = (idparent + "_" + originalFilename).toLowerCase()
		if (parentclass) {
			newFileName = parentclass + "_" + newFileName
		}
		newFileName
	}
	
	def getMetadata() {
		
		DBObject metadata = new BasicDBObject()
		metadata.put("idparent",			idparent)
		metadata.put("originalFilename",	originalFilename.toLowerCase())
		metadata.put("fileExtension",		fileExtension.toLowerCase())
		metadata.put("access",				accesspolitic)
		
		if (parentclass) metadata.put("parentclass", parentclass)
		if (text) metadata.put("text", text)
		
		return metadata
		
	}
	
	def targetFileExists() {
		gridfsService.exists(config, targetFilename)
	}
	
	def createTargetFile() {
		def gridFsFilename	= targetFilename.toLowerCase().replaceAll(/ /,"") // TODO Why? Should this be part of getTargetFilename?
		def gfsFile			= gridfsService.addToGridFS(config, file, gridFsFilename)
		gfsFile.setMetaData(metadata)
		gridfsService.attemptUpload(config, gfsFile)
	}
	
}
