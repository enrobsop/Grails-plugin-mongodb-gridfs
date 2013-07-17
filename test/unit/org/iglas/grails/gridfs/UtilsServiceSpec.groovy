package org.iglas.grails.gridfs

import grails.plugin.spock.UnitSpec
import grails.test.mixin.*

import org.bson.types.ObjectId
import org.codehaus.groovy.grails.web.mapping.LinkGenerator
import org.iglas.grails.utils.ConfigHelper

import pl.burningice.plugins.image.BurningImageService
import pl.burningice.plugins.image.engines.Worker
import spock.lang.Shared
import spock.lang.Unroll

import com.mongodb.DBObject
import com.mongodb.gridfs.GridFSDBFile

@TestFor(UtilsService)
class UtilsServiceSpec extends UnitSpec {

	@Shared gridfsService
	@Shared linkGenerator
	@Shared burningImageService
	
	def setup() {
		gridfsService = Mock(GridfsService)
		service.gridfsService = gridfsService
		
		linkGenerator = Mock(LinkGenerator)
		service.grailsLinkGenerator = linkGenerator
		
		burningImageService = Mock(BurningImageService)
		service.burningImageService = burningImageService
		
		configureWith()
	}
	
	def "can get an icon URL for a file using its object ID"() {
		
		given: "a file"
			ObjectId theOid	= new ObjectId("51d2821703649df48c3edd5c")
			def theFile		= Mock(GridFSDBFile)
			def theFilename	= "myId_myfile.jpg"
			theFile.getFilename() >> theFilename
		and: "the file metadata"
			def theMetadata	= Mock(DBObject)
			theMetadata.get("idparent") 		>> "theParentId"
			theMetadata.get("originalFilename") >> "theOriginalFilename.JPG"
			theMetadata.get("fileExtension") 	>> "JPG"
			theFile.getMetaData() >> theMetadata
		and: "an expected URL"
			def theExpectedUrl = "/myicons/thumbnail.jpg"
		and: "a burning image job"
			def burningImageWorker = Mock(Worker)
		
		when: "getting an icon for the file"
			def result = service.getIconForFile(theOid, [:]) 
		
		then: "the correct icon is returned"
			result.link == theExpectedUrl
		and: "the expected calls are made"
			1 * gridfsService.getById(theOid) >> theFile
			(1.._) * linkGenerator.resource(_) >> theExpectedUrl
			1 * burningImageService.doWith(_,_) >> burningImageWorker
			1 * burningImageWorker.execute(_ as String, _ as Closure)
			
	}
	
	def "can get an icon for a non-image file"() {
		
		given: "a PDF"
			def theFile		= Mock(GridFSDBFile)
			def theFilename	= "myId_myfile.pdf"
			theFile.getFilename() >> theFilename
		and: "the file metadata"
			def theMetadata	= Mock(DBObject)
			theMetadata.get("idparent")			>> "theParentId"
			theMetadata.get("originalFilename")	>> "theOriginalFilename.pdf"
			theMetadata.get("fileExtension")	>> "pdf"
			theFile.getMetaData() >> theMetadata
			
		when: "getting an icon"
			def result = service.getIconForFile(theFile, [:])
			
		then: "the expected calls are made"
			1 * linkGenerator.resource(_) >> "theIcon.gif"
		and: "the correct icon is given"	
			result.link == "theIcon.gif"
			
	}
	
	@Unroll
	def "thumbnail file names are created correctly [#expectedResult]"() {
		
		expect:
		service.getThumbnailName(originalFilename, width, height) == expectedResult
		
		where:
		width	| height	| originalFilename	| expectedResult
		50		| 75		| "myImage.jpg"		| "50x75myimage"
		50		| 75		| "my.Image.jpg"	| "50x75my.image"
		150		| 175		| " animage.jpg "	| "150x175animage"
		
	}
	
//	def "getting an icon checks access permissions"() {
//		
//		given: "a file"
//			def theFile		= Mock(GridFSDBFile)
//		
//		when: "getting an icon for the file"
//			def result = service.getIconForFile(theFile)
//		
//		then: "the permissions are checked"
//			1 * checkPermissions()
//		
//	}
	
	private def configureWith(options=[:]) {
		def defaultConfig = [
			allowedExtensions:	["jpg"],
			imagestype:			["jpg", "JPG", "jpeg", "JPEG", "png", "gif"],
			iconsdir:			"myIcons",
			thumbconfig:		[
				publicdir:	"public",
				x_size:		128,
				y_size:		128
			]
		]
		def config = defaultConfig << options
		
		def configHelper = Mock(ConfigHelper)
		configHelper.getConfig(_) >> config
		service.configHelper = configHelper
	}

	
}
