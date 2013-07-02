package org.iglas.grails.gridfs

import org.codehaus.groovy.grails.validation.Validateable
import org.springframework.web.multipart.MultipartFile

import com.mongodb.BasicDBObject
import com.mongodb.DBObject
import com.mongodb.gridfs.GridFSFile;

@Validateable
class UploadFileCommand {
	
	transient def config
	transient def gridfsService
	private transient def targetFile
	
	String 			idparent
	MultipartFile	file
	
	String parentclass
	String text
	String accesspolitic	= "public"
	
	String id
	String successController
	String successAction
	String successType
	String errorController
	String errorAction
	String errorType

	static constraints = {
		file				nullable:false
		idparent 			nullable:false
		parentclass			nullable:true
		text				nullable:true
		accesspolitic		nullable:true
		successController	nullable:true
		successAction		nullable:true
		successType			nullable:true
		errorController		nullable:true
		errorAction			nullable:true
		errorType			nullable:true
		id					nullable:true
	}
	
	void setAccesspolitic(String val) {
		this.accesspolitic = val ?: 'public'
	}
	
	def getOriginalFilename() {
		file?.originalFilename
	}
	
	def getFileExtension() {
		def filename = file?.originalFilename?.trim()
		def lastDotIndex = filename?.lastIndexOf('.')		
		(lastDotIndex > 0) ? filename.substring(lastDotIndex+1) : null
	}
	
	def getTargetFilename() {
		String newFileName = (idparent + "_" + originalFilename)
		if (parentclass) {
			newFileName = parentclass + "_" + newFileName
		}
		newFileName.replaceAll(/\s*/, "").toLowerCase()
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
		def gfsFile	= gridfsService.addToGridFS(config, file, targetFilename)
		gfsFile.setMetaData(metadata)
		this.targetFile = gfsFile
		gridfsService.attemptUpload(config, gfsFile)
	}
	
	boolean isFileEmpty() {
		!file || file.size <= 0
	}
	
	boolean isFileTooBig() {
		def hasSizeLimit = maxSize > 0
		hasSizeLimit && !isFileEmpty() && file.size > maxSize 
	}
	
	boolean isExtensionAllowed() {
		config?.allowedExtensions?.contains(fileExtension)
	}
	
	void setSuccessController(String value) {
		successController = value?.trim()
	}
	
	String getSuccessController() {
		successController ?: config?.controllers?.successController
	}
	
	void setSuccessAction(String value) {
		successAction = value?.trim()
	}
	
	String getSuccessAction() {
		successAction ?: config?.controllers?.successAction
	}

	void setSuccessType(String value) {
		successType = value?.trim()
	}
	
	String getSuccessType() {
		successType ?: config?.controllers?.successType
	}
	
	void setErrorController(String value) {
		errorController = value?.trim()
	}
	
	String getErrorController() {
		errorController ?: config?.controllers?.errorController
	}
	
	void setErrorAction(String value) {
		errorAction = value?.trim()
	}
	
	String getErrorAction() {
		errorAction ?: config?.controllers?.errorAction
	}

	void setErrorType(String value) {
		errorType = value?.trim()
	}
	
	String getErrorType() {
		errorType ?: config?.controllers?.errorType
	}
	
	def getAllowedExtensions() {
		config?.allowedExtensions
	}
	
	def getMaxSize() {
		config?.maxSize ?: 0
	}
	
	def getTargetFile() {
		targetFile
	}
	
	def getTargetFileId() {
		targetFile?.getId().toString()
	}

}
