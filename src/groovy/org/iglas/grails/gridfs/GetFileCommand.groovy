package org.iglas.grails.gridfs

import org.codehaus.groovy.grails.validation.Validateable

@Validateable
class GetFileCommand {
	
	String filename
	String id
	
	static constraints = {
		filename	blank: false
	}
	
}
