package org.iglas.grails.gridfs

import grails.plugin.spock.UnitSpec
import grails.test.mixin.*

import org.bson.types.ObjectId
import org.codehaus.groovy.grails.web.mapping.LinkGenerator
import org.iglas.grails.utils.ConfigHelper

import pl.burningice.plugins.image.BurningImageService
import pl.burningice.plugins.image.engines.Worker
import spock.lang.Shared

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
			theMetadata.get("idparent") >> "theParentId"
			theMetadata.get("originalFilename") >> "theOriginalFilename.JPG"
			theFile.getMetaData() >> theMetadata
		and: "an expected URL"
			def theExpectedUrl = "/myicons/thumbnail.jpg"
		and: "a burning image job"
			def burningImageWorker = Mock(Worker)
		
		when: "getting an icon for the file"
			def theUrlStr = service.getIconForFile(theOid, [:]) 
		
		then: "the correct icon is returned"
			theUrlStr == theExpectedUrl
		and: "the expected calls are made"
			1 * gridfsService.getById(theOid) >> theFile
			(1.._) * linkGenerator.resource(_) >> theExpectedUrl
			1 * gridfsService.getByFilename(theFilename) >> theFile
			1 * burningImageService.doWith(_,_) >> burningImageWorker
			1 * burningImageWorker.execute(_ as String, _ as Closure)
			
	}
	
	private def configureWith(options=[:]) {
		def defaultConfig = [
			allowedExtensions:	["jpg"],
			iconsdir:			"myIcons",
			thumbconfig:		[
				publicdir:	"public"
			]
		]
		def config = defaultConfig << options
		
		def configHelper = Mock(ConfigHelper)
		configHelper.getConfig(_) >> config
		service.configHelper = configHelper
	}

	
}
