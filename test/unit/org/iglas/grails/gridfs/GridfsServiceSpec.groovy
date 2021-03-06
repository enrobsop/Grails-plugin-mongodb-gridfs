package org.iglas.grails.gridfs

import grails.plugin.spock.UnitSpec
import grails.test.mixin.*

import org.bson.types.ObjectId

import spock.lang.Shared
import spock.lang.Unroll

import com.mongodb.gridfs.GridFSDBFile

@TestFor(GridfsService)
class GridfsServiceSpec extends UnitSpec {
	
	@Shared def gridfsHelperService
	
	def setup() {
		gridfsHelperService			= Mock(GridfsHelperService)
		service.gridfsHelperService	= gridfsHelperService
	}

	def "can get a file by object ID"() {
		
		given: "a file id"
			def theOid	= new ObjectId("51d2821703649df48c3edd5c")
			def theFile	= Mock(GridFSDBFile)
			
		when: "attempt to get the file by ID"
			def found = service.getById(theOid)
			
		then: "the correct file is returned"
			found 		!= null
			found		== theFile
		
		and: "any calls to delegates are correctly made"
			1 * gridfsHelperService.findOne(theOid) >> theFile
		
	}		

	def "can get a file by filename"() {
		
		given: "a file id"
			def theFilename	= "myid_myimage.jpg"
			def theFile		= Mock(GridFSDBFile)
			
		when: "attempt to get the file using the filename"
			def found = service.getByFilename(theFilename)
			
		then: "the correct file is returned"
			found 		!= null
			found		== theFile
		
		and: "any calls to delegates are correctly made"
			1 * gridfsHelperService.findOne(theFilename) >> theFile
		
	}
	
}
