package org.iglas.grails.gridfs

import grails.plugin.spock.UnitSpec
import grails.test.mixin.*

import org.bson.types.ObjectId

import spock.lang.Shared

@TestFor(GridfsTagLib)
class GridfsTagLibSpec extends UnitSpec {

	@Shared utilsService
	
	def setup() {
		utilsService = Mock(UtilsService)
		tagLib.utilsService = utilsService
	}
	
	def "the default form successType and errorType should be left to the controller/service when not defined"() {
		
		given: "a minimal form"
			def theMinimalForm = '<gridfs:form idparent="myId" />'
			
		when: "the tag is applied"
			def html = applyTemplate('<gridfs:form idparent="myId" />')
		
		then: "the successType and errorTypes do not appear"
			!html.contains("<input type='hidden' name='successType'")
			!html.contains("<input type='hidden' name='errorType'")
					
	}
	
	def "the form successType and errorType should use the configured values when set"() {
		
		given: "a form defining the success and error types"
			def theForm = '<gridfs:form idparent="myId" successType="forward" errorType="chain"/>'
			
		when: "the tag is applied"
			def html = applyTemplate(theForm)
			
		then:
			html.contains("<input type='hidden' name='successType' value='forward'")
			html.contains("<input type='hidden' name='errorType' value='chain'")
					
	}
	
	def "an icon can be obtained using an object ID"() {
		
		given: "an image tag with a file ID and thumbnail dimensions"
			def theId		= new ObjectId("51d2821703649df48c3edd5c")
			def theWidth	= 100
			def theHeight	= 60
			def theTagHtml	= "<gridfs:getIcon fileId=\"$theId\" x=\"$theWidth\" y=\"$theHeight\" />"
		and: "the path to the icon"
			def theExpectedPath = "myIcons/myIcon.gif"
		
		when: "the tag is applied"
			def html = applyTemplate(theTagHtml)
		
		then: "the correct calls are made"
			1 * utilsService.getIconForFile(theId, [thumbconfig:[x_size:theWidth,y_size:theHeight]]) >> theExpectedPath
			0 * utilsService.getIconForFile(_ as String, _)
		and: "the correct html is created"
			html.contains("src=\"$theExpectedPath\"")
		
	}

	def "an icon can be obtained using a filename"() {
		
		given: "an image tag with a file ID and thumbnail dimensions"
			def theFilename	= "my_file.jpg"
			def theWidth	= 100
			def theHeight	= 60
			def theTagHtml	= "<gridfs:getIcon filename=\"$theFilename\" x=\"$theWidth\" y=\"$theHeight\" />"
		and: "the path to the icon"
			def theExpectedPath = "myIcons/myIcon.gif"
		
		when: "the tag is applied"
			def html = applyTemplate(theTagHtml)
		
		then: "the correct calls are made"
			1 * utilsService.getIconForFile(theFilename, [thumbconfig:[x_size:theWidth,y_size:theHeight]]) >> theExpectedPath
			0 * utilsService.getIconForFile(_ as ObjectId, _) >> theExpectedPath
		and: "the correct html is created"
			html.contains("src=\"$theExpectedPath\"")
		
	}

}
