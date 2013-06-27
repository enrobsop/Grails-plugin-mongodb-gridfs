package org.iglas.grails.gridfs

import org.codehaus.groovy.grails.validation.Validateable

import com.mongodb.BasicDBObject
import com.mongodb.DBObject

@Validateable
class UploadFileCommand {

	String idparent
	String originalFilename
	String fileExtension
	
	String parentclass
	String text
	String accesspolitic	= "public"
	
	static constraints = {
		idparent 			nullable:false
		originalFilename	nullable:false
		fileExtension		nullable:false
		parentClass			nullable:true
		text				nullable:true
		accesspolitic		nullable:true
	}
	
	void setAccesspolitic(String val) {
		this.accesspolitic = val ?: 'public'
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
	
}
