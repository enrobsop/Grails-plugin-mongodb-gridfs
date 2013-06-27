package org.iglas.grails.gridfs

import org.codehaus.groovy.grails.validation.Validateable

@Validateable
class UploadFileCommand {

	String idparent
	String parentclass
	String originalFilename
	
	static constraints = {
		idparent 			nullable:false
		parentClass			nullable:true
		originalFilename	nullable:false
	}
	
	def getTargetFilename() {
		String newFileName = (idparent + "_" + originalFilename).toLowerCase()
		if (parentclass) {
			newFileName = parentclass + "_" + newFileName
		}
		newFileName
	}
	
}
